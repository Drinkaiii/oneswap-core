package com.oneswap.service;

import com.oneswap.util.RedisUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Repository;

import java.io.IOException;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Repository
@RequiredArgsConstructor
@Log4j2
public class LiquidityRepository {

    private final RedisUtil redisUtil;
    private final Web3jRestApiService web3jRestApiService;

    public boolean saveTokenPair(String tokenAddress, List<BigInteger> reserves) {
        redisUtil.set(tokenAddress, reserves);
        return true;
    }

    public boolean updateTokenPair(String tokenAddress, Map<String, BigInteger> variations) {
        try {
            List reserves = redisUtil.get(tokenAddress, List.class);
            if (reserves == null) {
                reserves = web3jRestApiService.getUniswapV2Reserves(tokenAddress);
            }
            // Convert reserves to BigInteger using toString()
            BigInteger reserve0 = new BigInteger(reserves.get(0).toString());
            BigInteger reserve1 = new BigInteger(reserves.get(1).toString());
            reserve0 = reserve0.add(variations.get("Amount0In")).subtract(variations.get("Amount0Out"));
            reserve1 = reserve1.add(variations.get("Amount1In")).subtract(variations.get("Amount1Out"));
            // Create a new modifiable list
            reserves = new ArrayList();
            reserves.add(reserve0);
            reserves.add(reserve1);
            redisUtil.set(tokenAddress, reserves);
            return true;
        } catch (IOException e) {
            log.warn(e.getMessage());
            return false;
        } catch (Exception e) {
            log.error("Error updating token pair for address {}: {}", tokenAddress, e.getMessage(), e);
            return false;
        }
    }
}
