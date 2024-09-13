package com.oneswap.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.web3j.abi.FunctionEncoder;
import org.web3j.abi.FunctionReturnDecoder;
import org.web3j.abi.TypeReference;
import org.web3j.abi.datatypes.Function;
import org.web3j.abi.datatypes.Type;
import org.web3j.abi.datatypes.generated.Uint112;
import org.web3j.abi.datatypes.generated.Uint32;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.DefaultBlockParameterName;
import org.web3j.protocol.core.methods.request.Transaction;
import org.web3j.protocol.core.methods.response.EthCall;

import java.io.IOException;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@Service
public class Web3jRestApiService {

    @Autowired
    @Qualifier("web3jHttp")
    private Web3j web3j;

    // call Uniswap V2 getReserves()
    public List<BigInteger> getUniswapV2Reserves(String contractAddress) throws IOException {

        if (!contractAddress.startsWith("0x"))
            contractAddress = "0x" + contractAddress;

        Function function = new Function("getReserves", Collections.emptyList(),
                Arrays.asList(
                        new TypeReference<Uint112>() {
                        },  // reserve0
                        new TypeReference<Uint112>() {
                        },  // reserve1
                        new TypeReference<Uint32>() {
                        }    // blockTimestampLast
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

        // return two token's reserve
        return new ArrayList<>(Arrays.asList(reserve0, reserve1));
    }
}
