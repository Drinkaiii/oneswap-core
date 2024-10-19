package com.oneswap.service;

import com.oneswap.repository.impl.NetworkRepository;
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

    private final NetworkRepository networkRepository;

    public void saveGasFee(BigInteger gasPrice) {

        networkRepository.saveGasFee(gasPrice);

    }

}
