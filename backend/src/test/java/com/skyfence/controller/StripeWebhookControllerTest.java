package com.skyfence.controller;

import com.skyfence.security.JwtService;
import com.skyfence.security.UserDetailsServiceImpl;
import com.skyfence.service.StripeService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = StripeWebhookController.class,
        excludeAutoConfiguration = {SecurityAutoConfiguration.class, UserDetailsServiceAutoConfiguration.class})
@ActiveProfiles("test")
class StripeWebhookControllerTest {

    @Autowired
    MockMvc mockMvc;

    @MockBean
    StripeService stripeService;

    @MockBean
    JwtService jwtService;

    @MockBean
    UserDetailsServiceImpl userDetailsService;

    @Test
    void webhook_validSignature_returns200() throws Exception {
        doNothing().when(stripeService).handleWebhookEvent(anyString(), anyString());

        mockMvc.perform(post("/api/stripe/webhook")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Stripe-Signature", "t=123,v1=abc")
                        .content("{\"type\":\"customer.subscription.updated\"}"))
                .andExpect(status().isOk())
                .andExpect(content().string("ok"));
    }

    @Test
    void webhook_invalidSignature_returns400() throws Exception {
        doThrow(new IllegalArgumentException("Invalid signature"))
                .when(stripeService).handleWebhookEvent(anyString(), anyString());

        mockMvc.perform(post("/api/stripe/webhook")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Stripe-Signature", "invalid")
                        .content("{\"type\":\"test\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Invalid signature"));
    }
}
