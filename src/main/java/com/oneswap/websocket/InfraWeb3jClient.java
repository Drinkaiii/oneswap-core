package com.oneswap.websocket;

import com.oneswap.model.Liquidity;
import com.oneswap.service.BalancerService;
import com.oneswap.service.LiquidityRepository;
import com.oneswap.service.UniswapService;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.web3j.abi.EventEncoder;
import org.web3j.abi.FunctionEncoder;
import org.web3j.abi.FunctionReturnDecoder;
import org.web3j.abi.TypeReference;
import org.web3j.abi.datatypes.Address;
import org.web3j.abi.datatypes.Event;
import org.web3j.abi.datatypes.Function;
import org.web3j.abi.datatypes.Type;
import org.web3j.abi.datatypes.generated.Bytes32;
import org.web3j.abi.datatypes.generated.Uint256;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.DefaultBlockParameterName;
import org.web3j.protocol.core.methods.request.EthFilter;
import org.web3j.protocol.core.methods.request.Transaction;
import org.web3j.protocol.core.methods.response.EthCall;
import org.web3j.protocol.core.methods.response.Log;
import org.web3j.utils.Numeric;

import java.math.BigInteger;
import java.util.*;

@Component
@RequiredArgsConstructor
@Log4j2
public class InfraWeb3jClient {

    @Autowired
    @Qualifier("web3jWebsocket")
    private Web3j web3j;
    private final UniswapService uniswapService;
    private final BalancerService balancerService;
    private final LiquidityRepository liquidityRepository;

    List<String> uniswapPairAddresses = List.of(
            "0x0d4a11d5eeaac28ec3f61d100daf4d40471f1852", // USDT-WETH
            "0xBb2b8038a1640196FbE3e38816F3e67Cba72D940"  // WBTC-WETH
    );
    // WBTC-WETH、wstETH-WETH Stable Pool、rsETH-WETH Stable Pool
    List balancerPairAddressesAndId = List.of(
            List.of("0xa6f548df93de924d73be7d25dc02554c6bd66db500020000000000000000000e", "0xa6f548df93de924d73be7d25dc02554c6bd66db5"),
            List.of("0x93d199263632a4ef4bb438f1feb99e57b4b5f0bd0000000000000000000005c2", "0x93d199263632a4ef4bb438f1feb99e57b4b5f0bd"),
            List.of("0x58aadfb1afac0ad7fca1148f3cde6aedf5236b6d00000000000000000000067f", "0x58aadfb1afac0ad7fca1148f3cde6aedf5236b6d"),
            List.of("0x3de27efa2f1aa663ae5d458857e731c129069f29000200000000000000000588", "0x3de27efa2f1aa663ae5d458857e731c129069f29")
    );
    private Set<String> monitoredBalancerPoolAddresses = new HashSet<>();

    // save the contracts of all token0 and token1
    private Map<String, String> token0Map = new HashMap<>();
    private Map<String, String> token1Map = new HashMap<>();

    // define SWAP event
    private static final Event UNISWAP_SWAP_EVENT = new Event("Swap",
            Arrays.asList(
                    new TypeReference<Address>(true) {
                    },    // indexed sender
                    new TypeReference<Uint256>() {
                    },        // amount0In
                    new TypeReference<Uint256>() {
                    },        // amount1In
                    new TypeReference<Uint256>() {
                    },        // amount0Out
                    new TypeReference<Uint256>() {
                    },        // amount1Out
                    new TypeReference<Address>(true) {
                    }     // indexed to
            )
    );
    private static final List<TypeReference<Type>> NON_INDEXED_PARAMETERS = UNISWAP_SWAP_EVENT.getNonIndexedParameters();

    private static final Event BALANCER_VAULT_SWAP_EVENT = new Event("Swap",
            Arrays.asList(
                    new TypeReference<Bytes32>(true) {
                    }, // poolId
                    new TypeReference<Address>(true) {
                    }, // tokenIn
                    new TypeReference<Address>(true) {
                    }, // tokenOut
                    new TypeReference<Uint256>() {
                    },     // amountIn
                    new TypeReference<Uint256>() {
                    }      // amountOut
            )
    );

    @PostConstruct
    public void init() throws Exception {

        for (Object AddressAndId : balancerPairAddressesAndId) {
            String contractAddress = (String) ((List) AddressAndId).get(1);
            monitoredBalancerPoolAddresses.add(contractAddress.toLowerCase());
        }

        fetchInitialReserves();
        subscribeToContractEvents();
    }

    // Every 600 seconds refresh data
    @Scheduled(fixedRate = 60000 * 10)
    public void resynchronizeReserves() {
        fetchInitialReserves();
    }

    @PreDestroy
    public void close() {
        if (web3j != null) {
            web3j.shutdown();
            log.info("Web3j connection closed.");
        }
    }

    private void fetchInitialReserves() {
        // Uniswap V2
        for (String contractAddress : uniswapPairAddresses) {
            try {
                Liquidity liquidity = uniswapService.getUniswapV2Reserves(contractAddress);
                liquidityRepository.saveTokenPair(liquidity);
            } catch (Exception e) {
                log.error("Error fetching reserves for contract: " + contractAddress, e);
            }
        }
        // Balancer V2
        for (Object AddressAndId : balancerPairAddressesAndId) {
            String poolId = (String) ((List) AddressAndId).get(0);
            String contractAddress = (String) ((List) AddressAndId).get(1);
            try {
                Liquidity liquidity = balancerService.getPoolTokensAndBalances(poolId, "0xBA12222222228d8Ba445958a75a0704d566BF2C8"); //todo
                liquidityRepository.saveTokenPair(liquidity);
            } catch (Exception e) {
                log.error("Error fetching reserves for contract: " + contractAddress, e);
            }
        }
    }

    private void subscribeToContractEvents() throws Exception {

        // listen Uniswap V2 SWAP event
        for (String contractAddress : uniswapPairAddresses) {
            try {
                // get token0 and token1
                String token0 = getToken0(contractAddress);
                String token1 = getToken1(contractAddress);
                // convert to lower case
                token0Map.put(contractAddress.toLowerCase(), token0);
                token1Map.put(contractAddress.toLowerCase(), token1);
                // use EthFilter set subscription condition
                EthFilter filter = new EthFilter(
                        DefaultBlockParameterName.LATEST,
                        DefaultBlockParameterName.LATEST,
                        contractAddress
                );

                // subscript events
                web3j.ethLogFlowable(filter).subscribe(log -> {
                    processUniswapSwapEvent(log);
                }, error -> {
                    log.error("Error while subscribing to contract events for contract: " + contractAddress, error);
                });
                log.info("Subscribed to contract events for contract: " + contractAddress);
            } catch (Exception e) {
                log.error("Error subscribing to contract events for contract: " + contractAddress, e);
            }
        }

        // listen Balancer V2 SWAP event
        EthFilter filter = new EthFilter(
                DefaultBlockParameterName.LATEST,
                DefaultBlockParameterName.LATEST,
                "0xBA12222222228d8Ba445958a75a0704d566BF2C8"
        );
        String swapEventSignature = EventEncoder.encode(BALANCER_VAULT_SWAP_EVENT);
        filter.addSingleTopic(swapEventSignature);
        web3j.ethLogFlowable(filter).subscribe(log -> {
            processBalancerVaultSwapEvent(log);
        }, error -> {
            log.error("Error while subscribing to Vault contract events", error);
        });
    }

    private String getToken0(String contractAddress) throws Exception {
        Function function = new Function("token0", Arrays.asList(), Arrays.asList(new TypeReference<Address>() {
        }));
        String token0 = callContractFunction(function, contractAddress);
        return token0;
    }

    private String getToken1(String contractAddress) throws Exception {
        Function function = new Function("token1", Arrays.asList(), Arrays.asList(new TypeReference<Address>() {
        }));
        String token1 = callContractFunction(function, contractAddress);
        return token1;
    }

    private String callContractFunction(Function function, String contractAddress) throws Exception {
        // encode function
        String encodedFunction = FunctionEncoder.encode(function);
        // sign contract
        Transaction transaction = Transaction.createEthCallTransaction(null, contractAddress, encodedFunction);
        EthCall response = web3j.ethCall(transaction, DefaultBlockParameterName.LATEST).send();
        if (response.isReverted())
            throw new RuntimeException("Error calling contract function: " + response.getRevertReason());
        // decode return value
        List<Type> decodedResponse = FunctionReturnDecoder.decode(response.getValue(), function.getOutputParameters());
        // assume the first return value is address
        return decodedResponse.get(0).getValue().toString();
    }

    private void processUniswapSwapEvent(Log eventLog) {

        String contractAddress = eventLog.getAddress();

        // Check if the event matches the SWAP event signature
        String eventSignature = EventEncoder.encode(UNISWAP_SWAP_EVENT);
        if (!eventLog.getTopics().get(0).equals(eventSignature)) {
            return; // This is not a SWAP event
        }

        // Decode indexed parameters
        Address sender = (Address) FunctionReturnDecoder.decodeIndexedValue(
                eventLog.getTopics().get(1), UNISWAP_SWAP_EVENT.getIndexedParameters().get(0));
        Address to = (Address) FunctionReturnDecoder.decodeIndexedValue(
                eventLog.getTopics().get(2), UNISWAP_SWAP_EVENT.getIndexedParameters().get(1));

        // Decode non-indexed parameters
        List<Type> nonIndexedValues = FunctionReturnDecoder.decode(eventLog.getData(), NON_INDEXED_PARAMETERS);

        Uint256 amountAIn = (Uint256) nonIndexedValues.get(0);
        Uint256 amountBIn = (Uint256) nonIndexedValues.get(1);
        Uint256 amountAOut = (Uint256) nonIndexedValues.get(2);
        Uint256 amountBOut = (Uint256) nonIndexedValues.get(3);

        String tokenA = token0Map.get(contractAddress);
        String tokenB = token1Map.get(contractAddress);

        BigInteger amountA = amountAIn.getValue().compareTo(BigInteger.ZERO) != 0
                ? amountAIn.getValue()
                : amountAOut.getValue().negate();

        BigInteger amountB = amountBIn.getValue().compareTo(BigInteger.ZERO) != 0
                ? amountBIn.getValue()
                : amountBOut.getValue().negate();

        liquidityRepository.updateTokenPair(tokenA, tokenB, amountA, amountB, LiquidityRepository.EXCHANGER_UNISWAP);

        log.info("=======================Uniswap Swap event detected=======================");
        log.info("Swap in monitored pool: " + contractAddress);
        if (amountA.compareTo(BigInteger.ZERO) > 0)
            log.info("[In] token: " + tokenA + " , amount: " + amountA);
        else
            log.info("[Out] token: " + tokenA + " , amount: " + amountA.negate());
        if (amountB.compareTo(BigInteger.ZERO) > 0)
            log.info("[In] token: " + tokenB + " , amount: " + amountB);
        else
            log.info("[Out] token: " + tokenB + " , amount: " + amountB.negate());

    }

    private void processBalancerVaultSwapEvent(Log eventLog) {
        // Decode event
        String eventSignature = EventEncoder.encode(BALANCER_VAULT_SWAP_EVENT);
        if (!eventLog.getTopics().get(0).equals(eventSignature)) {
            return;
        }

        // Decode indexed parameters
        Bytes32 poolId = (Bytes32) FunctionReturnDecoder.decodeIndexedValue(eventLog.getTopics().get(1), BALANCER_VAULT_SWAP_EVENT.getIndexedParameters().get(0));
        Address tokenIn = (Address) FunctionReturnDecoder.decodeIndexedValue(eventLog.getTopics().get(2), BALANCER_VAULT_SWAP_EVENT.getIndexedParameters().get(1));
        Address tokenOut = (Address) FunctionReturnDecoder.decodeIndexedValue(eventLog.getTopics().get(3), BALANCER_VAULT_SWAP_EVENT.getIndexedParameters().get(2));

        // Decode non-indexed parameters
        List<Type> nonIndexedValues = FunctionReturnDecoder.decode(eventLog.getData(), BALANCER_VAULT_SWAP_EVENT.getNonIndexedParameters());
        Uint256 amountIn = (Uint256) nonIndexedValues.get(0);
        Uint256 amountOut = (Uint256) nonIndexedValues.get(1);

        // Extract pool address from poolId
        byte[] poolIdBytes = poolId.getValue();
        byte[] poolAddressBytes = Arrays.copyOfRange(poolIdBytes, 0, 20); // first 20 byte
        String poolAddress = "0x" + Numeric.toHexStringNoPrefix(poolAddressBytes);

        // check address and save
        if (monitoredBalancerPoolAddresses.contains(poolAddress.toLowerCase())) {
            log.info("=======================Balancer Swap event detected=======================");
            log.info("Swap in monitored pool: " + poolAddress);
            log.info("[In] token: " + tokenIn.getValue() + " , amount: " + amountIn.getValue());
            log.info("[Out] token: " + tokenOut.getValue() + " , amount: " + amountOut.getValue());
            BigInteger tokenAmountOut = amountOut.getValue().negate();
            liquidityRepository.updateTokenPair(tokenIn.getValue(), tokenOut.getValue(), amountIn.getValue(), tokenAmountOut, LiquidityRepository.EXCHANGER_BALANCER);
        }
    }


}
