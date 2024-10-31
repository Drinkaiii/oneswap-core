package com.oneswap.repository;

import com.oneswap.model.Liquidity;
import org.springframework.stereotype.Repository;

import java.math.BigInteger;

@Repository
public interface LiquidityRepository {

    String saveTokenPair(Liquidity liquidity);

    Liquidity updateTokenPair(String tokenA, String tokenB, BigInteger amountA, BigInteger amountB, String exchanger);


}
