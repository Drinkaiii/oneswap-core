package com.oneswap.repository.impl;

import com.oneswap.model.Liquidity;
import com.oneswap.repository.LiquidityRepository;
import com.oneswap.util.RedisUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Repository;

import java.math.BigInteger;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Repository
@RequiredArgsConstructor
@Log4j2
public class LiquidityRepositoryImpl implements LiquidityRepository {

    @Value("${blockchain}")
    private String blockchain;

    private final RedisUtil redisUtil;

    public static String EXCHANGER_UNISWAP = "Uniswap";
    public static String EXCHANGER_BALANCER = "Balancer";

    public String saveTokenPair(Liquidity liquidity) {

        String tokenA = liquidity.getToken0();
        String tokenB = liquidity.getToken1();
        BigInteger amountA = liquidity.getAmount0();
        BigInteger amountB = liquidity.getAmount1();

        // comfirm token0 and token1
        String token0, token1;
        BigInteger amount0, amount1;

        if (tokenA.compareTo(tokenB) < 0) {
            token0 = tokenA;
            token1 = tokenB;
            amount0 = amountA;
            amount1 = amountB;
        } else {
            token0 = tokenB;
            token1 = tokenA;
            amount0 = amountB;
            amount1 = amountA;
        }

        // combine two token address as key
        String key = "liquidity:" + token0 + ":" + token1 + ":" + liquidity.getExchanger() + ":" + blockchain;
        // update data
        liquidity.setToken0(token0);
        liquidity.setToken1(token1);
        liquidity.setAmount0(amount0);
        liquidity.setAmount1(amount1);

        // save to Redis
        redisUtil.set(key, liquidity, 1000, TimeUnit.MINUTES);
        return key;
    }

    @Nullable
    public Liquidity updateTokenPair(String tokenA, String tokenB, BigInteger amountA, BigInteger amountB, String exchanger) {

        // comfirm token0 and token1
        String token0, token1;
        BigInteger amount0, amount1;
        if (tokenA.compareTo(tokenB) < 0) {
            token0 = tokenA;
            token1 = tokenB;
            amount0 = amountA;
            amount1 = amountB;
        } else {
            token0 = tokenB;
            token1 = tokenA;
            amount0 = amountB;
            amount1 = amountA;
        }

        // combine two token address as key
        String key = "liquidity:" + token0 + ":" + token1 + ":" + exchanger + ":" + blockchain;
        // update data
        Liquidity liquidity;
        try {
            // get data from Redis
            liquidity = redisUtil.get(key, Liquidity.class);
            if (liquidity == null)//TODO
                return null;
            // update data
            BigInteger reserve0 = new BigInteger(liquidity.getAmount0().toString());
            BigInteger reserve1 = new BigInteger(liquidity.getAmount1().toString());
            liquidity.setAmount0(reserve0.add(amount0));
            liquidity.setAmount1(reserve1.add(amount1));
            // save to Redis
            redisUtil.set(key, liquidity, 1000, TimeUnit.MINUTES);
            Map<String,String> data = Map.of("type","liquidity","data",token0 + ":" + token1);
            redisUtil.publish(token0 + ":" + token1, data);
        } catch (Exception e) {
            log.error("Error updating token pair for address {} | {}: {}", token0, token1, e.getMessage(), e);
            return null;
        }
        return liquidity;
    }
}
