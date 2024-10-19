package com.oneswap.service;

import com.oneswap.model.Liquidity;
import org.springframework.stereotype.Service;

import java.io.IOException;

@Service
public interface BalancerService {

    Liquidity getPoolTokensAndBalances(String poolId, String vaultAddress) throws IOException;
}
