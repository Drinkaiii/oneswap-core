package com.oneswap.util;

import org.springframework.stereotype.Component;
import org.web3j.crypto.Credentials;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.DefaultBlockParameterName;
import org.web3j.protocol.core.RemoteFunctionCall;
import org.web3j.protocol.core.methods.request.Transaction;
import org.web3j.protocol.core.methods.response.EthCall;
import org.web3j.protocol.core.methods.response.EthEstimateGas;
import org.web3j.protocol.core.methods.response.TransactionReceipt;
import org.web3j.tx.Contract;

import java.io.IOException;
import java.math.BigInteger;

@Component
public class Web3TransactionUtil {

    // Estimate gas for the swap transaction
    public BigInteger estimate(Web3j web3j, Credentials credentials, String targetAddress, RemoteFunctionCall remoteFunctionCall, BigInteger maxFee) throws IOException {

        String ownerAddress = credentials.getAddress();
        String data = remoteFunctionCall.encodeFunctionCall();

        // create a transaction for estimating gas
        // ownerAddress, nonce, maxFee, gas limit, targetAddress, ETH transfer, transaction data
        Transaction transaction = Transaction.createFunctionCallTransaction(
                ownerAddress,
                null,
                maxFee,
                null,
                targetAddress,
                BigInteger.ZERO,
                data
        );

        // call estimate function
        EthEstimateGas estimateGas = web3j.ethEstimateGas(transaction).send();
        if (estimateGas.hasError())
            throw new RuntimeException("Gas estimation failed: " + estimateGas.getError().getMessage());

        // calculate gas limit
        BigInteger gasUsed = estimateGas.getAmountUsed();
        BigInteger adjustedGasLimit = gasUsed.multiply(BigInteger.valueOf(15)).divide(BigInteger.valueOf(10));

        // calculate gas total
//        BigInteger gasPrice = web3j.ethGasPrice().send().getGasPrice();
//        BigInteger totalFeeInWei = adjustedGasLimit.multiply(gasPrice);
//        BigDecimal totalFeeInEth = Convert.fromWei(new BigDecimal(totalFeeInWei), Convert.Unit.ETHER);
//        System.out.println("Estimated Transaction Fee in ETH: " + totalFeeInEth.toPlainString());

        // return adjusted gas limit
        return adjustedGasLimit;
    }

    // check the swap transaction
    public boolean check(Web3j web3j, Credentials credentials, Contract contract, RemoteFunctionCall remoteFunctionCall) throws IOException {

        // create a simulate transaction
        Transaction transaction = Transaction.createEthCallTransaction(
                credentials.getAddress(),
                contract.getContractAddress(),
                remoteFunctionCall.encodeFunctionCall()
        );

        // run eth_call to simulate transaction
        EthCall ethCall = web3j.ethCall(transaction, DefaultBlockParameterName.LATEST).send();
        if (ethCall.isReverted()) {
            String revertReason = ethCall.getRevertReason();
            System.out.println("transaction reverted：" + (revertReason != null ? revertReason : "no reason"));
            return false;
        }

        // return result
        return !ethCall.isReverted();

    }

    public String send(RemoteFunctionCall remoteFunctionCall) throws Exception {
        TransactionReceipt receipt = (TransactionReceipt) remoteFunctionCall.send();
        return receipt.getTransactionHash();
    }

}
