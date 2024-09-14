package com.oneswap.service;

import com.oneswap.model.Liquidity;
import com.oneswap.util.RedisUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;

import java.math.BigInteger;
import java.util.concurrent.TimeUnit;

@Repository
@RequiredArgsConstructor
@Log4j2
public class LiquidityRepository {

    private final RedisUtil redisUtil;

    public static String EXCHANGER_UNISWAP = "Uniswap";
    public static String EXCHANGER_BALANCER = "Balancer";

    public boolean saveTokenPair(Liquidity liquidity) {

        // 暂时使用 tokenA 和 tokenB
        String tokenA = liquidity.getToken0();
        String tokenB = liquidity.getToken1();
        BigInteger amountA = liquidity.getAmount0();
        BigInteger amountB = liquidity.getAmount1();

        // 根據 tokenA 和 tokenB 的顺序确定 token0 和 token1
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
        String key = token0 + ":" + token1 + ":" + liquidity.getExchanger();
        // update data
        liquidity.setToken0(token0);
        liquidity.setToken1(token1);
        liquidity.setAmount0(amount0);
        liquidity.setAmount1(amount1);

        // save to Redis
        redisUtil.set(key, liquidity, 10, TimeUnit.MINUTES);
        return true;
    }


    public boolean updateTokenPair(String tokenA, String tokenB, BigInteger amountA, BigInteger amountB, String exchanger) {

        // 根據 tokenA 和 tokenB 的顺序确定 token0 和 token1
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
        String key = token0 + ":" + token1+ ":" + exchanger;
        // update data
        try {
            // get data from Redis
            Liquidity liquidity = redisUtil.get(key, Liquidity.class);
            if (liquidity == null)
                return false;
            // update data
            BigInteger reserve0 = new BigInteger(liquidity.getAmount0().toString());
            BigInteger reserve1 = new BigInteger(liquidity.getAmount1().toString());
            liquidity.setAmount0(reserve0.add(amount0));
            liquidity.setAmount1(reserve1.add(amount1));
            // save to Redis
            redisUtil.set(key, liquidity, 10, TimeUnit.MINUTES);
            return true;
        } catch (Exception e) {
            log.error("Error updating token pair for address {} | {}: {}", token0, token1, e.getMessage(), e);
            return false;
        }
    }
}
