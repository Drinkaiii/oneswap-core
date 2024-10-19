package com.oneswap.service;

import com.oneswap.model.Liquidity;
import org.springframework.stereotype.Service;

import java.io.IOException;

@Service
public interface UniswapService {

    Liquidity getUniswapV2Reserves(String contractAddress) throws IOException;

}
