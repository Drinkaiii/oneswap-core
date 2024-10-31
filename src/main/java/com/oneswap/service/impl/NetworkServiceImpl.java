package com.oneswap.service.impl;

import com.oneswap.repository.NetworkRepository;
import com.oneswap.repository.impl.NetworkRepositoryImpl;
import com.oneswap.service.NetworkService;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;

import java.math.BigInteger;

@Service
@RequiredArgsConstructor
@Log4j2
public class NetworkServiceImpl implements NetworkService {

    private final NetworkRepository networkRepository;

    public void saveGasFee(BigInteger gasPrice) {

        networkRepository.saveGasFee(gasPrice);

    }

}
