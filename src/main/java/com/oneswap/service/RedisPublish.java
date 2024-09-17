package com.oneswap.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RedisPublish {

    public static final String LIQUIDITY_TOPIC = "liquidityTopic";
    public static final String RECORD_TOPIC = "recordTopic";

    private final RedisTemplate redisTemplate;

    public void publish(String topic, String message) {
        redisTemplate.convertAndSend(topic, message);
    }

}
