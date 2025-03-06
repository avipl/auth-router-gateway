package com.authdemo.auth.filter;

import io.github.bucket4j.ConsumptionProbe;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.util.concurrent.TimeUnit;

@Component
@RequiredArgsConstructor
public class RateLimitFilter extends OncePerRequestFilter {

    private final RedisTemplate<String, String> redisTemplate;

    private static final String PER_HOUR_PREFIX = "PER_HOUR";
    private static final String PER_MIN_PREFIX = "PER_MINUTE";

    @Value("${redis.rate-limit.limitPerHour: 200}")
    private int MAX_REQUEST_PER_HOUR;

    @Value("${redis.rate-limit.limitPerMin: 20}")
    private int MAX_REQUEST_PER_MIN;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        String ipAddress = request.getRemoteAddr();

        String perHourKey = "%s_%s".formatted(ipAddress, PER_HOUR_PREFIX);
        String perMinKey = "%s_%s".formatted(ipAddress, PER_MIN_PREFIX);

        String perMin = redisTemplate.opsForValue().get(perMinKey);
        String perHour = redisTemplate.opsForValue().get(perHourKey);

        if((perHour != null && Integer.parseInt(perHour) > MAX_REQUEST_PER_HOUR) || (perMin != null && Integer.parseInt(perMin) > MAX_REQUEST_PER_MIN)) {
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.getWriter().write("Rate limit exceeded. Please try again later.");
            return;
        }

        Integer perMinCnt = perMin == null? 0 : Integer.parseInt(perMin);
        Integer perHourCnt = perHour == null? 0 : Integer.parseInt(perHour);

        final var remainingTokens = MAX_REQUEST_PER_MIN - (perMinCnt == null? 0 : perMinCnt);
        response.setHeader("X-Rate-Limit-Remaining", String.valueOf(remainingTokens));

        if (perHourCnt == null) redisTemplate.opsForValue().set(perHourKey, "1", 60 * 60, TimeUnit.SECONDS);
        else redisTemplate.opsForValue().increment(perHourKey);

        if (perMinCnt == null) redisTemplate.opsForValue().set(perMinKey, "1", 60, TimeUnit.SECONDS);
        else redisTemplate.opsForValue().increment(perMinKey);

        filterChain.doFilter(request, response);
    }
}
