package com.skyfence.controller;

import com.skyfence.model.User;
import com.skyfence.service.StripeService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api")
public class CheckoutController {

    private final StripeService stripeService;

    public CheckoutController(StripeService stripeService) {
        this.stripeService = stripeService;
    }

    @PostMapping("/checkout")
    public ResponseEntity<?> createCheckout(@AuthenticationPrincipal User user) {
        if (user == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Authentication required"));
        }
        try {
            String url = stripeService.createCheckoutSession(user);
            return ResponseEntity.ok(Map.of("url", url));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", "Could not create checkout session"));
        }
    }

    @GetMapping("/checkout/verify")
    public ResponseEntity<?> verifyCheckout(
            @RequestParam String session_id,
            @AuthenticationPrincipal User user) {
        if (user == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Authentication required"));
        }
        if (session_id == null || !session_id.matches("^cs_(test|live)_[A-Za-z0-9]+$")) {
            return ResponseEntity.badRequest().body(Map.of("error", "Invalid session id"));
        }
        boolean upgraded = stripeService.verifyAndUpgradeSession(session_id, user);
        return ResponseEntity.ok(Map.of("upgraded", upgraded));
    }

    @GetMapping("/subscription")
    public ResponseEntity<?> getSubscription(@AuthenticationPrincipal User user) {
        if (user == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Authentication required"));
        }
        return ResponseEntity.ok(Map.of(
                "subscriptionStatus", user.getSubscriptionStatus().name(),
                "username", user.getUsername()
        ));
    }
}
