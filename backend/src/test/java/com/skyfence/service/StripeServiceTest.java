package com.skyfence.service;

import com.skyfence.model.Role;
import com.skyfence.model.SubscriptionStatus;
import com.skyfence.model.User;
import com.skyfence.repository.UserRepository;
import com.stripe.model.Event;
import com.stripe.model.EventDataObjectDeserializer;
import com.stripe.model.Subscription;
import com.stripe.net.Webhook;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StripeServiceTest {

    @Mock
    UserRepository userRepository;

    @InjectMocks
    StripeService stripeService;

    @BeforeEach
    void setup() {
        ReflectionTestUtils.setField(stripeService, "apiKey", "sk_test_fake");
        ReflectionTestUtils.setField(stripeService, "webhookSecret", "whsec_test_fake_secret");
        ReflectionTestUtils.setField(stripeService, "proPriceId", "price_test");
        ReflectionTestUtils.setField(stripeService, "successUrl", "https://localhost:5173/subscription/success");
        ReflectionTestUtils.setField(stripeService, "cancelUrl", "https://localhost:5173/subscription/cancel");
    }

    @Test
    void handleWebhookEvent_invalidSignature_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class,
                () -> stripeService.handleWebhookEvent("{}", "invalid-sig"));
    }

    @Test
    void handleWebhookEvent_unknownEventType_noRepositoryInteraction() {
        try (MockedStatic<Webhook> mocked = mockStatic(Webhook.class)) {
            Event event = mock(Event.class);
            mocked.when(() -> Webhook.constructEvent(anyString(), anyString(), anyString()))
                    .thenReturn(event);
            when(event.getType()).thenReturn("some.unknown.event");

            stripeService.handleWebhookEvent("{}", "sig");

            verifyNoInteractions(userRepository);
        }
    }

    @Test
    void handleWebhookEvent_subscriptionUpdated_activeStatus_setsProStatus() {
        try (MockedStatic<Webhook> mocked = mockStatic(Webhook.class)) {
            Event event = mock(Event.class);
            EventDataObjectDeserializer deserializer = mock(EventDataObjectDeserializer.class);
            Subscription subscription = mock(Subscription.class);

            mocked.when(() -> Webhook.constructEvent(anyString(), anyString(), anyString()))
                    .thenReturn(event);
            when(event.getType()).thenReturn("customer.subscription.updated");
            when(event.getDataObjectDeserializer()).thenReturn(deserializer);
            when(deserializer.getObject()).thenReturn(Optional.of(subscription));
            when(subscription.getCustomer()).thenReturn("cus_test");
            when(subscription.getStatus()).thenReturn("active");

            User user = new User("testuser", "pwd", Role.OPERATOR);
            user.setSubscriptionStatus(SubscriptionStatus.FREE);
            when(userRepository.findByStripeCustomerId("cus_test")).thenReturn(Optional.of(user));

            stripeService.handleWebhookEvent("{}", "sig");

            assertEquals(SubscriptionStatus.PRO, user.getSubscriptionStatus());
            verify(userRepository).save(user);
        }
    }

    @Test
    void handleWebhookEvent_subscriptionCreated_trialingStatus_setsProStatus() {
        try (MockedStatic<Webhook> mocked = mockStatic(Webhook.class)) {
            Event event = mock(Event.class);
            EventDataObjectDeserializer deserializer = mock(EventDataObjectDeserializer.class);
            Subscription subscription = mock(Subscription.class);

            mocked.when(() -> Webhook.constructEvent(anyString(), anyString(), anyString()))
                    .thenReturn(event);
            when(event.getType()).thenReturn("customer.subscription.created");
            when(event.getDataObjectDeserializer()).thenReturn(deserializer);
            when(deserializer.getObject()).thenReturn(Optional.of(subscription));
            when(subscription.getCustomer()).thenReturn("cus_test");
            when(subscription.getStatus()).thenReturn("trialing");

            User user = new User("testuser", "pwd", Role.OPERATOR);
            when(userRepository.findByStripeCustomerId("cus_test")).thenReturn(Optional.of(user));

            stripeService.handleWebhookEvent("{}", "sig");

            assertEquals(SubscriptionStatus.PRO, user.getSubscriptionStatus());
        }
    }

    @Test
    void handleWebhookEvent_subscriptionUpdated_canceledStatus_setsFreeStatus() {
        try (MockedStatic<Webhook> mocked = mockStatic(Webhook.class)) {
            Event event = mock(Event.class);
            EventDataObjectDeserializer deserializer = mock(EventDataObjectDeserializer.class);
            Subscription subscription = mock(Subscription.class);

            mocked.when(() -> Webhook.constructEvent(anyString(), anyString(), anyString()))
                    .thenReturn(event);
            when(event.getType()).thenReturn("customer.subscription.updated");
            when(event.getDataObjectDeserializer()).thenReturn(deserializer);
            when(deserializer.getObject()).thenReturn(Optional.of(subscription));
            when(subscription.getCustomer()).thenReturn("cus_test");
            when(subscription.getStatus()).thenReturn("canceled");

            User user = new User("testuser", "pwd", Role.OPERATOR);
            user.setSubscriptionStatus(SubscriptionStatus.PRO);
            when(userRepository.findByStripeCustomerId("cus_test")).thenReturn(Optional.of(user));

            stripeService.handleWebhookEvent("{}", "sig");

            assertEquals(SubscriptionStatus.FREE, user.getSubscriptionStatus());
        }
    }

    @Test
    void handleWebhookEvent_subscriptionDeleted_downgradesUser() {
        try (MockedStatic<Webhook> mocked = mockStatic(Webhook.class)) {
            Event event = mock(Event.class);
            EventDataObjectDeserializer deserializer = mock(EventDataObjectDeserializer.class);
            Subscription subscription = mock(Subscription.class);

            mocked.when(() -> Webhook.constructEvent(anyString(), anyString(), anyString()))
                    .thenReturn(event);
            when(event.getType()).thenReturn("customer.subscription.deleted");
            when(event.getDataObjectDeserializer()).thenReturn(deserializer);
            when(deserializer.getObject()).thenReturn(Optional.of(subscription));
            when(subscription.getCustomer()).thenReturn("cus_test");

            User user = new User("testuser", "pwd", Role.OPERATOR);
            user.setSubscriptionStatus(SubscriptionStatus.PRO);
            when(userRepository.findByStripeCustomerId("cus_test")).thenReturn(Optional.of(user));

            stripeService.handleWebhookEvent("{}", "sig");

            assertEquals(SubscriptionStatus.FREE, user.getSubscriptionStatus());
            verify(userRepository).save(user);
        }
    }

    @Test
    void handleWebhookEvent_invoicePaymentFailed_downgradesUser() {
        try (MockedStatic<Webhook> mocked = mockStatic(Webhook.class)) {
            Event event = mock(Event.class);
            EventDataObjectDeserializer deserializer = mock(EventDataObjectDeserializer.class);
            Subscription subscription = mock(Subscription.class);

            mocked.when(() -> Webhook.constructEvent(anyString(), anyString(), anyString()))
                    .thenReturn(event);
            when(event.getType()).thenReturn("invoice.payment_failed");
            when(event.getDataObjectDeserializer()).thenReturn(deserializer);
            when(deserializer.getObject()).thenReturn(Optional.of(subscription));
            when(subscription.getCustomer()).thenReturn("cus_test");

            User user = new User("testuser", "pwd", Role.OPERATOR);
            user.setSubscriptionStatus(SubscriptionStatus.PRO);
            when(userRepository.findByStripeCustomerId("cus_test")).thenReturn(Optional.of(user));

            stripeService.handleWebhookEvent("{}", "sig");

            assertEquals(SubscriptionStatus.FREE, user.getSubscriptionStatus());
        }
    }

    @Test
    void handleWebhookEvent_subscriptionDeleted_userNotFound_noSave() {
        try (MockedStatic<Webhook> mocked = mockStatic(Webhook.class)) {
            Event event = mock(Event.class);
            EventDataObjectDeserializer deserializer = mock(EventDataObjectDeserializer.class);
            Subscription subscription = mock(Subscription.class);

            mocked.when(() -> Webhook.constructEvent(anyString(), anyString(), anyString()))
                    .thenReturn(event);
            when(event.getType()).thenReturn("customer.subscription.deleted");
            when(event.getDataObjectDeserializer()).thenReturn(deserializer);
            when(deserializer.getObject()).thenReturn(Optional.of(subscription));
            when(subscription.getCustomer()).thenReturn("cus_unknown");
            when(userRepository.findByStripeCustomerId("cus_unknown")).thenReturn(Optional.empty());

            stripeService.handleWebhookEvent("{}", "sig");

            verify(userRepository, never()).save(any());
        }
    }

    @Test
    void handleWebhookEvent_subscriptionUpdated_userNotFound_noSave() {
        try (MockedStatic<Webhook> mocked = mockStatic(Webhook.class)) {
            Event event = mock(Event.class);
            EventDataObjectDeserializer deserializer = mock(EventDataObjectDeserializer.class);
            Subscription subscription = mock(Subscription.class);

            mocked.when(() -> Webhook.constructEvent(anyString(), anyString(), anyString()))
                    .thenReturn(event);
            when(event.getType()).thenReturn("customer.subscription.updated");
            when(event.getDataObjectDeserializer()).thenReturn(deserializer);
            when(deserializer.getObject()).thenReturn(Optional.of(subscription));
            when(subscription.getCustomer()).thenReturn("cus_unknown");
            when(subscription.getStatus()).thenReturn("active");
            when(userRepository.findByStripeCustomerId("cus_unknown")).thenReturn(Optional.empty());

            stripeService.handleWebhookEvent("{}", "sig");

            verify(userRepository, never()).save(any());
        }
    }
}
