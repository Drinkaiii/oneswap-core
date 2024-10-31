package com.oneswap.service;

import com.oneswap.model.Liquidity;
import org.springframework.stereotype.Service;

import java.io.IOException;

@Service
public interface UniswapService {

    Liquidity getUniswapV2Reserves(String contractAddress) throws IOException;

    String getToken0(String contractAddress);

    String getToken1(String contractAddress);

}
