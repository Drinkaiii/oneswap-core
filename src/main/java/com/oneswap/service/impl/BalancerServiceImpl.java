package com.oneswap.service.impl;

import com.oneswap.model.Liquidity;
import com.oneswap.service.BalancerService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.web3j.abi.FunctionEncoder;
import org.web3j.abi.FunctionReturnDecoder;
import org.web3j.abi.TypeReference;
import org.web3j.abi.datatypes.Address;
import org.web3j.abi.datatypes.DynamicArray;
import org.web3j.abi.datatypes.Function;
import org.web3j.abi.datatypes.Type;
import org.web3j.abi.datatypes.generated.Bytes32;
import org.web3j.abi.datatypes.generated.Uint256;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.DefaultBlockParameterName;
import org.web3j.protocol.core.methods.request.Transaction;
import org.web3j.protocol.core.methods.response.EthCall;
import org.web3j.utils.Numeric;

import java.io.IOException;
import java.math.BigInteger;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BalancerServiceImpl implements BalancerService {

    @Value("${blockchain}")
    private String blockchain;

    @Autowired
    @Qualifier("web3jHttp")
    private Web3j web3j;

    public Liquidity getPoolTokensAndBalances(String poolId, String vaultAddress) throws IOException {
        // make poolId String convert to 32 byte
        Bytes32 poolIdBytes32 = new Bytes32(Numeric.hexStringToByteArray(poolId));

        // create getPoolTokens function
        Function function = new Function(
                "getPoolTokens",
                Arrays.asList(poolIdBytes32),
                Arrays.asList(
                        new TypeReference<DynamicArray<Address>>() {
                        },
                        new TypeReference<DynamicArray<Uint256>>() {
                        }
                )
        );

        String encodedFunction = FunctionEncoder.encode(function);

        EthCall response = web3j.ethCall(
                Transaction.createEthCallTransaction(null, vaultAddress, encodedFunction),
                DefaultBlockParameterName.LATEST
        ).send();

        if (response.isReverted()) {
            throw new RuntimeException("Error calling getPoolTokens: " + response.getRevertReason());
        }

        // decode return value
        List<Type> result = FunctionReturnDecoder.decode(response.getValue(), function.getOutputParameters());

        // get token address and reserve
        DynamicArray<Address> addresses = (DynamicArray<Address>) result.get(0);
        DynamicArray<Uint256> balancesArray = (DynamicArray<Uint256>) result.get(1);

        List<String> tokenAddresses = addresses.getValue().stream()
                .map(Address::getValue)
                .collect(Collectors.toList());

        List<BigInteger> balances = balancesArray.getValue().stream()
                .map(Uint256::getValue)
                .collect(Collectors.toList());

        // get BPT address from poolId
        byte[] poolIdBytes = Numeric.hexStringToByteArray(poolId);
        byte[] poolAddressBytes = Arrays.copyOfRange(poolIdBytes, 0, 20);
        String poolAddress = "0x" + Numeric.toHexStringNoPrefix(poolAddressBytes).toLowerCase();

        // remove BPT address
        List<String> filteredTokenAddresses = new ArrayList<>();
        List<BigInteger> filteredBalances = new ArrayList<>();

        for (int i = 0; i < tokenAddresses.size(); i++) {
            String tokenAddress = tokenAddresses.get(i).toLowerCase();
            if (!tokenAddress.equals(poolAddress)) {
                filteredTokenAddresses.add(tokenAddress);
                filteredBalances.add(balances.get(i));
            }
        }

        // checkout data format
        if (filteredTokenAddresses.size() < 2) {
            throw new RuntimeException("Less than two tokens found after filtering out pool address");
        }

        // set data
        Liquidity liquidity = new Liquidity();
        liquidity.setNetwork(blockchain);
        liquidity.setToken0(filteredTokenAddresses.get(0));
        liquidity.setAmount0(filteredBalances.get(0));
        liquidity.setToken1(filteredTokenAddresses.get(1));
        liquidity.setAmount1(filteredBalances.get(1));
        liquidity.setExchanger("Balancer");
        liquidity.setAlgorithm("Weighted");
        liquidity.setWeight(1); // TODO: 設定真實權重
        liquidity.setPoolId(poolId);
        return liquidity;
    }

}
