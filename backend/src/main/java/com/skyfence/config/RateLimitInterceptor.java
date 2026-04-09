package com.skyfence.config;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class RateLimitInterceptor implements HandlerInterceptor {

    private static final Logger log = LoggerFactory.getLogger(RateLimitInterceptor.class);

    private final Map<String, Bucket> readBuckets = new ConcurrentHashMap<>();
    private final Map<String, Bucket> writeBuckets = new ConcurrentHashMap<>();

    // Read limits: 60 requests per minute
    private Bucket createNewReadBucket() {
        Bandwidth limit = Bandwidth.builder().capacity(60).refillGreedy(60, Duration.ofMinutes(1)).build();
        return Bucket.builder().addLimit(limit).build();
    }

    // Write limits: 10 requests per minute
    private Bucket createNewWriteBucket() {
        Bandwidth limit = Bandwidth.builder().capacity(10).refillGreedy(10, Duration.ofMinutes(1)).build();
        return Bucket.builder().addLimit(limit).build();
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String ip = getClientIP(request);
        String method = request.getMethod();

        if (method.equalsIgnoreCase("POST") || method.equalsIgnoreCase("PUT") || method.equalsIgnoreCase("DELETE")) {
            Bucket bucket = writeBuckets.computeIfAbsent(ip, k -> createNewWriteBucket());
            if (bucket.tryConsume(1)) {
                return true;
            }
        } else {
            // Read limits for GET, OPTIONS, HEAD
            Bucket bucket = readBuckets.computeIfAbsent(ip, k -> createNewReadBucket());
            if (bucket.tryConsume(1)) {
                return true;
            }
        }

        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        log.warn("SECURITY ALERT: Rate limit exceeded for IP: {} [{} {}] - Possible Intrusion Attempt", ip, method, request.getRequestURI());
        response.getWriter().write("429 Too Many Requests");
        return false;
    }

    private String getClientIP(HttpServletRequest request) {
        String xfHeader = request.getHeader("X-Forwarded-For");
        if (xfHeader == null || xfHeader.isEmpty()) {
            return request.getRemoteAddr();
        }
        return xfHeader.split(",")[0].trim();
    }
}
