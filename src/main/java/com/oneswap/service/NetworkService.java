package com.oneswap.service;

import com.oneswap.util.RedisUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigInteger;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Log4j2
public class NetworkService {

    @Value("${blockchain}")
    private String blockchain;

    private final RedisUtil redisUtil;

    public void saveGasFee(BigInteger gasPrice) {

        // save to Redis
        redisUtil.set("gas:" + blockchain, gasPrice, 1000, TimeUnit.MINUTES);

        // Pub/Sub
        Map<String, String> data = Map.of("type", "gas", "data", gasPrice.toString());
        redisUtil.publish("gas:" + blockchain, data);
    }


}
