package com.skyfence.controller;

import com.skyfence.model.Role;
import com.skyfence.model.SubscriptionStatus;
import com.skyfence.model.User;
import com.skyfence.service.StripeService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CheckoutControllerTest {

    @Mock
    StripeService stripeService;

    @InjectMocks
    CheckoutController controller;

    private User testUser() {
        User user = new User("testuser", "pwd", Role.OPERATOR);
        user.setSubscriptionStatus(SubscriptionStatus.FREE);
        return user;
    }

    @Test
    void createCheckout_nullUser_returns401() {
        ResponseEntity<?> result = controller.createCheckout(null);
        assertEquals(401, result.getStatusCode().value());
    }

    @Test
    void createCheckout_withUser_returnsUrl() throws Exception {
        when(stripeService.createCheckoutSession(any())).thenReturn("https://checkout.stripe.com/test");

        ResponseEntity<?> result = controller.createCheckout(testUser());

        assertEquals(200, result.getStatusCode().value());
    }

    @Test
    void createCheckout_stripeError_returns500() throws Exception {
        when(stripeService.createCheckoutSession(any())).thenThrow(new RuntimeException("error"));

        ResponseEntity<?> result = controller.createCheckout(testUser());

        assertEquals(500, result.getStatusCode().value());
    }

    @Test
    void getSubscription_nullUser_returns401() {
        ResponseEntity<?> result = controller.getSubscription(null);
        assertEquals(401, result.getStatusCode().value());
    }

    @Test
    void getSubscription_withUser_returnsStatus() {
        ResponseEntity<?> result = controller.getSubscription(testUser());

        assertEquals(200, result.getStatusCode().value());
    }

    @Test
    void verifyCheckout_nullUser_returns401() {
        ResponseEntity<?> result = controller.verifyCheckout("cs_test_abc123", null);
        assertEquals(401, result.getStatusCode().value());
    }

    @Test
    void verifyCheckout_invalidSessionId_returns400() {
        ResponseEntity<?> result = controller.verifyCheckout("invalid_id", testUser());
        assertEquals(400, result.getStatusCode().value());
    }

    @Test
    void verifyCheckout_nullSessionId_returns400() {
        ResponseEntity<?> result = controller.verifyCheckout(null, testUser());
        assertEquals(400, result.getStatusCode().value());
    }

    @Test
    void verifyCheckout_validSession_upgraded_returns200() {
        when(stripeService.verifyAndUpgradeSession(eq("cs_test_abc123"), any(User.class))).thenReturn(true);

        ResponseEntity<?> result = controller.verifyCheckout("cs_test_abc123", testUser());

        assertEquals(200, result.getStatusCode().value());
    }

    @Test
    void verifyCheckout_validSession_notUpgraded_returns200() {
        when(stripeService.verifyAndUpgradeSession(eq("cs_live_abc123"), any(User.class))).thenReturn(false);

        ResponseEntity<?> result = controller.verifyCheckout("cs_live_abc123", testUser());

        assertEquals(200, result.getStatusCode().value());
    }
}
