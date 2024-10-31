package com.oneswap.repository;

import org.springframework.stereotype.Repository;

import java.math.BigInteger;

@Repository
public interface NetworkRepository {

    void saveGasFee(BigInteger gasPrice);

}
