package com.skyfence.service;

import com.skyfence.model.Role;
import com.skyfence.model.SubscriptionStatus;
import com.skyfence.model.User;
import com.skyfence.repository.UserRepository;
import com.stripe.model.Customer;
import com.stripe.model.Event;
import com.stripe.model.EventDataObjectDeserializer;
import com.stripe.model.Subscription;
import com.stripe.model.checkout.Session;
import com.stripe.net.Webhook;
import com.stripe.param.CustomerCreateParams;
import com.stripe.param.checkout.SessionCreateParams;
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
    void createCheckoutSession_existingCustomer_returnsUrl() throws Exception {
        try (MockedStatic<Session> mockedSession = mockStatic(Session.class)) {
            User user = new User("test", "pwd", Role.OPERATOR);
            user.setStripeCustomerId("cus_existing");

            Session session = mock(Session.class);
            mockedSession.when(() -> Session.create(any(SessionCreateParams.class))).thenReturn(session);
            when(session.getUrl()).thenReturn("https://checkout.stripe.com/test");

            String url = stripeService.createCheckoutSession(user);

            assertEquals("https://checkout.stripe.com/test", url);
            verify(userRepository, never()).save(any());
        }
    }

    @Test
    void createCheckoutSession_newCustomer_createsCustomerThenReturnsUrl() throws Exception {
        try (MockedStatic<Session> mockedSession = mockStatic(Session.class);
             MockedStatic<Customer> mockedCustomer = mockStatic(Customer.class)) {

            User user = new User("test", "pwd", Role.OPERATOR);
            user.setEmail("test@test.com");

            Customer customer = mock(Customer.class);
            when(customer.getId()).thenReturn("cus_new");
            mockedCustomer.when(() -> Customer.create(any(CustomerCreateParams.class))).thenReturn(customer);

            Session session = mock(Session.class);
            mockedSession.when(() -> Session.create(any(SessionCreateParams.class))).thenReturn(session);
            when(session.getUrl()).thenReturn("https://checkout.stripe.com/test");

            String url = stripeService.createCheckoutSession(user);

            assertEquals("cus_new", user.getStripeCustomerId());
            verify(userRepository).save(user);
            assertEquals("https://checkout.stripe.com/test", url);
        }
    }

    @Test
    void handleWebhookEvent_checkoutSessionCompleted_upgradesUser() {
        try (MockedStatic<Webhook> mocked = mockStatic(Webhook.class)) {
            Event event = mock(Event.class);
            EventDataObjectDeserializer deserializer = mock(EventDataObjectDeserializer.class);
            Session session = mock(Session.class);

            mocked.when(() -> Webhook.constructEvent(anyString(), anyString(), anyString()))
                    .thenReturn(event);
            when(event.getType()).thenReturn("checkout.session.completed");
            when(event.getDataObjectDeserializer()).thenReturn(deserializer);
            when(deserializer.getObject()).thenReturn(Optional.of(session));
            when(session.getMetadata()).thenReturn(java.util.Map.of("userId", "1"));
            when(session.getCustomer()).thenReturn("cus_test");

            User user = new User("testuser", "pwd", Role.OPERATOR);
            user.setSubscriptionStatus(SubscriptionStatus.FREE);
            when(userRepository.findById(1L)).thenReturn(Optional.of(user));

            stripeService.handleWebhookEvent("{}", "sig");

            assertEquals(SubscriptionStatus.PRO, user.getSubscriptionStatus());
            assertEquals("cus_test", user.getStripeCustomerId());
            verify(userRepository).save(user);
        }
    }

    @Test
    void handleWebhookEvent_checkoutSessionCompleted_missingMetadata_noSave() {
        try (MockedStatic<Webhook> mocked = mockStatic(Webhook.class)) {
            Event event = mock(Event.class);
            EventDataObjectDeserializer deserializer = mock(EventDataObjectDeserializer.class);
            Session session = mock(Session.class);

            mocked.when(() -> Webhook.constructEvent(anyString(), anyString(), anyString()))
                    .thenReturn(event);
            when(event.getType()).thenReturn("checkout.session.completed");
            when(event.getDataObjectDeserializer()).thenReturn(deserializer);
            when(deserializer.getObject()).thenReturn(Optional.of(session));
            when(session.getMetadata()).thenReturn(null);

            stripeService.handleWebhookEvent("{}", "sig");

            verify(userRepository, never()).save(any());
        }
    }

    @Test
    void handleWebhookEvent_checkoutSessionCompleted_deserializationFails_noSave() {
        try (MockedStatic<Webhook> mocked = mockStatic(Webhook.class)) {
            Event event = mock(Event.class);
            EventDataObjectDeserializer deserializer = mock(EventDataObjectDeserializer.class);

            mocked.when(() -> Webhook.constructEvent(anyString(), anyString(), anyString()))
                    .thenReturn(event);
            when(event.getType()).thenReturn("checkout.session.completed");
            when(event.getDataObjectDeserializer()).thenReturn(deserializer);
            when(deserializer.getObject()).thenReturn(Optional.empty());

            stripeService.handleWebhookEvent("{}", "sig");

            verify(userRepository, never()).save(any());
        }
    }

    @Test
    void verifyAndUpgradeSession_paidSession_upgradesUser() throws Exception {
        try (MockedStatic<Session> mockedSession = mockStatic(Session.class)) {
            Session session = mock(Session.class);
            mockedSession.when(() -> Session.retrieve("cs_test_abc")).thenReturn(session);
            when(session.getStatus()).thenReturn("complete");
            when(session.getCustomer()).thenReturn("cus_test");
            when(session.getMetadata()).thenReturn(java.util.Map.of("userId", "1"));

            User user = new User("testuser", "pwd", Role.OPERATOR);
            user.setSubscriptionStatus(SubscriptionStatus.FREE);
            when(userRepository.findById(1L)).thenReturn(Optional.of(user));

            User currentUser = new User("testuser", "pwd", Role.OPERATOR);

            boolean result = stripeService.verifyAndUpgradeSession("cs_test_abc", currentUser);

            assertTrue(result);
            assertEquals(SubscriptionStatus.PRO, user.getSubscriptionStatus());
            verify(userRepository).save(user);
        }
    }

    @Test
    void verifyAndUpgradeSession_unpaidSession_returnsFalse() throws Exception {
        try (MockedStatic<Session> mockedSession = mockStatic(Session.class)) {
            Session session = mock(Session.class);
            mockedSession.when(() -> Session.retrieve("cs_test_abc")).thenReturn(session);
            when(session.getStatus()).thenReturn("open");
            when(session.getPaymentStatus()).thenReturn("unpaid");

            User currentUser = new User("testuser", "pwd", Role.OPERATOR);

            boolean result = stripeService.verifyAndUpgradeSession("cs_test_abc", currentUser);

            assertFalse(result);
            verify(userRepository, never()).save(any());
        }
    }

    @Test
    void verifyAndUpgradeSession_stripeException_returnsFalse() throws Exception {
        try (MockedStatic<Session> mockedSession = mockStatic(Session.class)) {
            mockedSession.when(() -> Session.retrieve("cs_test_err"))
                    .thenThrow(new RuntimeException("Stripe error"));

            User currentUser = new User("testuser", "pwd", Role.OPERATOR);

            boolean result = stripeService.verifyAndUpgradeSession("cs_test_err", currentUser);

            assertFalse(result);
        }
    }

    @Test
    void verifyAndUpgradeSession_userNotFound_returnsFalse() throws Exception {
        try (MockedStatic<Session> mockedSession = mockStatic(Session.class)) {
            Session session = mock(Session.class);
            mockedSession.when(() -> Session.retrieve("cs_test_abc")).thenReturn(session);
            when(session.getStatus()).thenReturn("complete");
            when(session.getMetadata()).thenReturn(java.util.Map.of("userId", "999"));

            when(userRepository.findById(999L)).thenReturn(Optional.empty());

            User currentUser = new User("testuser", "pwd", Role.OPERATOR);

            boolean result = stripeService.verifyAndUpgradeSession("cs_test_abc", currentUser);

            assertFalse(result);
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
