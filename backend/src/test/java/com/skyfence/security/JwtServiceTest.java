package com.skyfence.security;

import com.skyfence.model.Role;
import com.skyfence.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

class JwtServiceTest {

    private JwtService jwtService;
    private User user;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService();
        ReflectionTestUtils.setField(jwtService, "secret",
                "test-secret-key-that-is-long-enough-for-hmac");
        ReflectionTestUtils.setField(jwtService, "expirationMs", 3600000L);

        user = new User("testuser", "encodedPwd", Role.OPERATOR);
    }

    @Test
    void generateToken_returnsNonEmptyString() {
        String token = jwtService.generateToken(user);
        assertThat(token).isNotBlank();
    }

    @Test
    void extractUsername_returnsCorrectUsername() {
        String token = jwtService.generateToken(user);
        assertThat(jwtService.extractUsername(token)).isEqualTo("testuser");
    }

    @Test
    void isValid_matchingUser_returnsTrue() {
        String token = jwtService.generateToken(user);
        assertThat(jwtService.isValid(token, user)).isTrue();
    }

    @Test
    void isValid_differentUser_returnsFalse() {
        String token = jwtService.generateToken(user);
        User other = new User("otheruser", "pwd", Role.OPERATOR);
        assertThat(jwtService.isValid(token, other)).isFalse();
    }

    @Test
    void isValid_expiredToken_returnsFalse() {
        ReflectionTestUtils.setField(jwtService, "expirationMs", -1000L);
        String token = jwtService.generateToken(user);
        assertThat(jwtService.isValid(token, user)).isFalse();
    }

    @Test
    void extractUsername_invalidToken_throwsException() {
        assertThrows(Exception.class, () -> jwtService.extractUsername("not.a.valid.token"));
    }
}
