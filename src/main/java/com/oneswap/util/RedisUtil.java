package com.oneswap.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.dao.QueryTimeoutException;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Component
@RequiredArgsConstructor
@Log4j2
public class RedisUtil {

    final private RedisTemplate redisTemplate;
    final private ObjectMapper objectMapper;

    public <T> Boolean set(String key, T value, long ttl, TimeUnit timeUnit) {
        try {
            redisTemplate.opsForValue().set(key, objectMapper.writeValueAsString(value), ttl, timeUnit);
            return true;
        } catch (JsonProcessingException e) {
            log.warn(e);
            e.printStackTrace();
            return false;
        } catch (RedisConnectionFailureException | QueryTimeoutException e) {
            log.error("Redis connection failure", e);
            return false;
        }
    }

    @Nullable
    public <T> T get(String key, Class<T> clazz) {
        try {
            String response = (String) redisTemplate.opsForValue().get(key);
            if (response != null)
                return objectMapper.readValue(response, clazz);
            else
                return null;
        } catch (JsonProcessingException e) {
            log.warn("Error processing JSON for key: " + key);
            return null;
        } catch (RedisConnectionFailureException | QueryTimeoutException e) {
            log.error("Redis connection failure");
            return null;
        }
    }

    public void delete(String key) {
        try {
            redisTemplate.delete(key);
        } catch (RedisConnectionFailureException | QueryTimeoutException e) {
            log.error("Redis connection failure");
        }

    }
}
