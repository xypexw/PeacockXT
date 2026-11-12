package com.github.peacockxt.filter;

import com.github.peacockxt.annotation.Idempotent;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import java.time.Duration;

@Component
@RequiredArgsConstructor
@Slf4j
public class IdempotencyInterceptor implements HandlerInterceptor {

    private final StringRedisTemplate redisTemplate;

    private static final String IDEMPOTENCY_HEADER = "X-Idempotency-Key";
    private static final String USER_ID_HEADER = "X-User-Id"; // Thêm header UserId
    private static final String REDIS_PREFIX = "IK::";
    private static final String STATUS_STARTED = "STARTED";
    private static final String REDIS_IK_ATTRIBUTE = "REDIS_IK_KEY";
    private static final Duration KEY_TTL = Duration.ofHours(24);

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if (!(handler instanceof HandlerMethod handlerMethod)) {
            return true;
        }

        if (!handlerMethod.getMethod().isAnnotationPresent(Idempotent.class)) {
            return true;
        }

        // Unpack Idempotency Key
        String idempotencyKey = request.getHeader(IDEMPOTENCY_HEADER);
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write("Missing X-Idempotency-Key header");
            return false;
        }

        String userId = request.getHeader(USER_ID_HEADER);
        if (userId == null || userId.isBlank()) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write("Missing X-UserId header");
            return false;
        }

        // unpack URL (URI) request
        String url = request.getRequestURI();

        // Creating Redis Key with format: IK::[UserId]::[URL]::[IdempotencyKey]
        String redisKey = REDIS_PREFIX + userId + "::" + url + "::" + idempotencyKey;

        Boolean isSet = redisTemplate.opsForValue().setIfAbsent(redisKey, STATUS_STARTED, KEY_TTL);

        if (Boolean.FALSE.equals(isSet)) {
            String cachedStatus = redisTemplate.opsForValue().get(redisKey);

            if (STATUS_STARTED.equals(cachedStatus)) {
                response.setStatus(HttpServletResponse.SC_CONFLICT);
                response.getWriter().write("Request is already in progress");
                return false;
            } else if (cachedStatus != null) {
                response.setStatus(Integer.parseInt(cachedStatus));
                return false;
            }
        }

        request.setAttribute(REDIS_IK_ATTRIBUTE, redisKey);
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        String redisKey = (String) request.getAttribute(REDIS_IK_ATTRIBUTE);
        if (redisKey != null) {
            if (ex != null || response.getStatus() >= 400) {
                redisTemplate.delete(redisKey);
                log.debug("Deleted Idempotency Key [{}] due to request failure", redisKey);
            } else {
                String finalStatus = String.valueOf(response.getStatus());
                redisTemplate.opsForValue().set(redisKey, finalStatus, KEY_TTL);
                log.debug("Persisted Idempotency Key [{}] with status [{}]", redisKey, finalStatus);
            }
        }
    }
}