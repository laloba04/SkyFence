package com.skyfence.service;

import com.skyfence.model.SubscriptionStatus;
import com.skyfence.model.User;
import com.skyfence.repository.UserRepository;
import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.Customer;
import com.stripe.model.Event;
import com.stripe.model.Subscription;
import com.stripe.model.checkout.Session;
import com.stripe.net.Webhook;
import com.stripe.param.CustomerCreateParams;
import com.stripe.param.checkout.SessionCreateParams;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class StripeService {

    private static final Logger log = LoggerFactory.getLogger(StripeService.class);

    @Value("${stripe.api.key}")
    private String apiKey;

    @Value("${stripe.webhook.secret}")
    private String webhookSecret;

    @Value("${stripe.price.pro}")
    private String proPriceId;

    @Value("${stripe.success.url}")
    private String successUrl;

    @Value("${stripe.cancel.url}")
    private String cancelUrl;

    private final UserRepository userRepository;

    public StripeService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @PostConstruct
    public void init() {
        Stripe.apiKey = apiKey;
    }

    public String createCheckoutSession(User user) throws StripeException {
        String customerId = ensureCustomer(user);

        SessionCreateParams params = SessionCreateParams.builder()
                .setMode(SessionCreateParams.Mode.SUBSCRIPTION)
                .setCustomer(customerId)
                .setSuccessUrl(successUrl + "?session_id={CHECKOUT_SESSION_ID}")
                .setCancelUrl(cancelUrl)
                .addLineItem(SessionCreateParams.LineItem.builder()
                        .setPrice(proPriceId)
                        .setQuantity(1L)
                        .build())
                .putMetadata("userId", String.valueOf(user.getId()))
                .build();

        Session session = Session.create(params);
        return session.getUrl();
    }

    public void handleWebhookEvent(String payload, String sigHeader) {
        Event event;
        try {
            event = Webhook.constructEvent(payload, sigHeader, webhookSecret);
        } catch (Exception e) {
            log.warn("Invalid Stripe webhook signature received");
            throw new IllegalArgumentException("Invalid signature");
        }

        switch (event.getType()) {
            case "customer.subscription.updated", "customer.subscription.created" -> {
                event.getDataObjectDeserializer().getObject().ifPresentOrElse(
                        obj -> {
                            Subscription sub = (Subscription) obj;
                            updateSubscription(sub.getCustomer(), sub.getStatus());
                        },
                        () -> log.warn("Could not deserialize subscription object for event {}", event.getId())
                );
            }
            case "customer.subscription.deleted", "invoice.payment_failed" -> {
                String customerId = extractCustomerId(event);
                if (customerId != null) downgradeToFree(customerId);
            }
            default -> log.debug("Unhandled Stripe event: {}", event.getType());
        }
    }

    private String ensureCustomer(User user) throws StripeException {
        if (user.getStripeCustomerId() != null) return user.getStripeCustomerId();

        CustomerCreateParams params = CustomerCreateParams.builder()
                .setEmail(user.getEmail())
                .setName(user.getUsername())
                .putMetadata("userId", String.valueOf(user.getId()))
                .build();

        Customer customer = Customer.create(params);
        user.setStripeCustomerId(customer.getId());
        userRepository.save(user);
        return customer.getId();
    }

    private void updateSubscription(String customerId, String status) {
        userRepository.findByStripeCustomerId(customerId).ifPresent(user -> {
            SubscriptionStatus next = "active".equals(status) || "trialing".equals(status)
                    ? SubscriptionStatus.PRO : SubscriptionStatus.FREE;
            user.setSubscriptionStatus(next);
            userRepository.save(user);
            log.info("User '{}' subscription → {}", user.getUsername(), next);
        });
    }

    private void downgradeToFree(String customerId) {
        userRepository.findByStripeCustomerId(customerId).ifPresent(user -> {
            user.setSubscriptionStatus(SubscriptionStatus.FREE);
            userRepository.save(user);
            log.info("User '{}' downgraded to FREE", user.getUsername());
        });
    }

    private String extractCustomerId(Event event) {
        return event.getDataObjectDeserializer().getObject()
                .map(obj -> {
                    if (obj instanceof Subscription s) return s.getCustomer();
                    return null;
                }).orElse(null);
    }
}
