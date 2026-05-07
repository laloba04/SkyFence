package com.skyfence.config;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
@Order(-200)
public class RateLimitInterceptor extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(RateLimitInterceptor.class);

    private final Map<String, Bucket> readBuckets  = new ConcurrentHashMap<>();
    private final Map<String, Bucket> writeBuckets = new ConcurrentHashMap<>();

    private Bucket createNewReadBucket() {
        Bandwidth limit = Bandwidth.builder().capacity(60).refillGreedy(60, Duration.ofMinutes(1)).build();
        return Bucket.builder().addLimit(limit).build();
    }

    private Bucket createNewWriteBucket() {
        Bandwidth limit = Bandwidth.builder().capacity(10).refillGreedy(10, Duration.ofMinutes(1)).build();
        return Bucket.builder().addLimit(limit).build();
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getServletPath();
        if (path == null || path.isEmpty()) {
            path = request.getRequestURI();
        }
        return !path.startsWith("/api/");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        String ip     = getClientIP(request);
        String method = request.getMethod();

        Bucket bucket;
        if ("POST".equalsIgnoreCase(method) || "PUT".equalsIgnoreCase(method) || "DELETE".equalsIgnoreCase(method)) {
            bucket = writeBuckets.computeIfAbsent(ip, k -> createNewWriteBucket());
        } else {
            bucket = readBuckets.computeIfAbsent(ip, k -> createNewReadBucket());
        }

        if (bucket.tryConsume(1)) {
            chain.doFilter(request, response);
        } else {
            log.warn("SECURITY ALERT: Rate limit exceeded for IP: {} [{} {}] - Possible Intrusion Attempt",
                     ip, method, request.getRequestURI());
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.getWriter().write("429 Too Many Requests");
        }
    }

    private String getClientIP(HttpServletRequest request) {
        // X-Forwarded-For is not used: it can be spoofed by clients to bypass rate limiting.
        // Use the TCP-level remote address which cannot be forged.
        return request.getRemoteAddr();
    }
}
