package com.oneswap.service;

import com.oneswap.model.Liquidity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.web3j.abi.FunctionEncoder;
import org.web3j.abi.FunctionReturnDecoder;
import org.web3j.abi.TypeReference;
import org.web3j.abi.datatypes.Function;
import org.web3j.abi.datatypes.Type;
import org.web3j.abi.datatypes.generated.Uint256;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.DefaultBlockParameterName;
import org.web3j.protocol.core.methods.request.Transaction;
import org.web3j.protocol.core.methods.response.EthCall;

import java.io.IOException;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@Service
public class UniswapService {

    @Value("${blockchain}")
    private String blockchain;

    @Autowired
    @Qualifier("web3jHttp")
    private Web3j web3j;

    // call Uniswap V2 getReserves()
    public Liquidity getUniswapV2Reserves(String contractAddress) throws IOException {

        if (!contractAddress.startsWith("0x"))
            contractAddress = "0x" + contractAddress;

        String token0 = getTokenAddress(contractAddress, "token0");
        String token1 = getTokenAddress(contractAddress, "token1");

        Function function = new Function("getReserves", Collections.emptyList(),
                Arrays.asList(
                        new TypeReference<Uint256>() {},  // reserve0
                        new TypeReference<Uint256>() {},  // reserve1
                        new TypeReference<Uint256>() {}   // blockTimestampLast
                )
        );

        // encode and call function
        String encodedFunction = FunctionEncoder.encode(function);
        Transaction transaction = Transaction.createEthCallTransaction(null, contractAddress, encodedFunction);
        EthCall response = web3j.ethCall(transaction, DefaultBlockParameterName.LATEST).send();

        // check response
        if (response.isReverted())
            throw new RuntimeException("Error calling getReserves: " + response.getRevertReason());

        // decode and get reserve0 and reserve1
        List<Type> decodedResponse = FunctionReturnDecoder.decode(response.getValue(), function.getOutputParameters());
        BigInteger reserve0 = (BigInteger) decodedResponse.get(0).getValue();
        BigInteger reserve1 = (BigInteger) decodedResponse.get(1).getValue();

        Liquidity liquidity = new Liquidity();
        liquidity.setNetwork(blockchain);
        liquidity.setToken0(token0);
        liquidity.setToken1(token1);
        liquidity.setAmount0(reserve0);
        liquidity.setAmount1(reserve1);
        liquidity.setExchanger("Uniswap");
        liquidity.setAlgorithm("Weighted");
        liquidity.setWeight(1.0);

        return liquidity;
    }

    private String getTokenAddress(String contractAddress, String tokenFunction) throws IOException {
        Function function = new Function(
                tokenFunction,
                Collections.emptyList(),
                Collections.singletonList(new TypeReference<org.web3j.abi.datatypes.Address>() {})
        );

        String encodedFunction = FunctionEncoder.encode(function);
        Transaction transaction = Transaction.createEthCallTransaction(null, contractAddress, encodedFunction);
        EthCall response = web3j.ethCall(transaction, DefaultBlockParameterName.LATEST).send();

        if (response.isReverted())
            throw new RuntimeException("Error calling " + tokenFunction + ": " + response.getRevertReason());

        List<Type> decodedResponse = FunctionReturnDecoder.decode(response.getValue(), function.getOutputParameters());
        return (String) decodedResponse.get(0).getValue();
    }
}
