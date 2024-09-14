package com.oneswap.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigInteger;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Liquidity {

    private String token0;
    private String token1;
    private BigInteger amount0;
    private BigInteger amount1;
    private String exchanger;
    private String algorithm;
    private double weight;

}
