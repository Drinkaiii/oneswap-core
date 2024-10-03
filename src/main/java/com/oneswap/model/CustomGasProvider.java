package com.oneswap.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import org.web3j.tx.gas.ContractGasProvider;

import java.math.BigInteger;

@Data
@AllArgsConstructor
@Builder
public class CustomGasProvider implements ContractGasProvider {

    private BigInteger gasLimit;
    private BigInteger maxFeePerGas;
    private BigInteger maxPriorityFeePerGas;

    private BigInteger gasPrice;

    @Override
    public BigInteger getGasPrice(String contractFunc) {
        return this.gasPrice;
    }

    @Override
    public BigInteger getGasPrice() {
        return this.gasPrice;
    }

    @Override
    public BigInteger getGasLimit(String contractFunc) {
        return gasLimit;
    }

    @Override
    public BigInteger getGasLimit() {
        return gasLimit;
    }

    public BigInteger getMaxFeePerGas() {
        return maxFeePerGas;
    }

    public BigInteger getMaxPriorityFeePerGas() {
        return maxPriorityFeePerGas;
    }
}
