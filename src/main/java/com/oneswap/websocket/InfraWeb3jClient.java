package com.oneswap.websocket;

import com.oneswap.service.LiquidityRepository;
import com.oneswap.service.Web3jRestApiService;
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
import org.web3j.abi.datatypes.generated.Uint256;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.DefaultBlockParameterName;
import org.web3j.protocol.core.methods.request.EthFilter;
import org.web3j.protocol.core.methods.request.Transaction;
import org.web3j.protocol.core.methods.response.EthCall;
import org.web3j.protocol.core.methods.response.Log;

import java.math.BigInteger;
import java.util.*;

@Component
@RequiredArgsConstructor
@Log4j2
public class InfraWeb3jClient {

    @Autowired
    @Qualifier("web3jWebsocket")
    private Web3j web3j;
    private final Web3jRestApiService web3jRestApiService;
    private final LiquidityRepository liquidityRepository;

    List<String> contractAddresses = List.of(
            "0x0d4a11d5eeaac28ec3f61d100daf4d40471f1852", // USDT-WETH
            "0xBb2b8038a1640196FbE3e38816F3e67Cba72D940",  // WBTC-WETH
            "0x3041cbd36888becc7bbcbc0045e3b1f144466f5f", // USDT-USDC
            "0x004375dff511095cc5a197a54140a24efef3a416", //WBTC-USDC
            "0x004375Dff511095CC5A197A54140a24eFEF3A416" //WBTC-USDT
    );

    // save the contracts of all token0 and token1
    private Map<String, String> token0Map = new HashMap<>();
    private Map<String, String> token1Map = new HashMap<>();

    // define SWAP event
    private static final Event SWAP_EVENT = new Event("Swap",
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
    private static final List<TypeReference<Type>> NON_INDEXED_PARAMETERS = SWAP_EVENT.getNonIndexedParameters();

    @PostConstruct
    public void init() {
        fetchInitialReserves();
        subscribeToContractEvents();
    }

    // Every 60 seconds refresh data
    @Scheduled(fixedRate = 60000)
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
        for (String contractAddress : contractAddresses) {
            try {
                List<BigInteger> reserves = web3jRestApiService.getUniswapV2Reserves(contractAddress);
                liquidityRepository.saveTokenPair(contractAddress, reserves);
            } catch (Exception e) {
                log.error("Error fetching reserves for contract: " + contractAddress, e);
            }
        }
    }

    private void subscribeToContractEvents() {
        for (String contractAddress : contractAddresses) {
            try {
                // get token0 and token1
                String token0 = getToken0(contractAddress);
                String token1 = getToken1(contractAddress);
                // convert to lower case
                token0Map.put(contractAddress.toLowerCase(), token0);
                token1Map.put(contractAddress.toLowerCase(), token1);
                // use EthFilter set subscription condition
                EthFilter filter = new EthFilter(
                        DefaultBlockParameterName.LATEST,   // 從最新區塊開始
                        DefaultBlockParameterName.LATEST,   // 訂閱所有新的區塊
                        contractAddress                     // 訂閱指定合約地址
                );

                // 訂閱合約事件
                web3j.ethLogFlowable(filter).subscribe(log -> {
                    processContractEvent(log);
                }, error -> {
                    log.error("Error while subscribing to contract events for contract: " + contractAddress, error);
                });

                log.info("Subscribed to contract events for contract: " + contractAddress);
            } catch (Exception e) {
                log.error("Error subscribing to contract events for contract: " + contractAddress, e);
            }
        }
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
        // 編碼 function 以便調用
        String encodedFunction = FunctionEncoder.encode(function);
        // 創建交易對象，並指定合約地址
        Transaction transaction = Transaction.createEthCallTransaction(null, contractAddress, encodedFunction);
        // 執行調用
        EthCall response = web3j.ethCall(transaction, DefaultBlockParameterName.LATEST).send();
        // 檢查返回是否有錯誤
        if (response.isReverted()) {
            throw new RuntimeException("Error calling contract function: " + response.getRevertReason());
        }
        // 解碼返回值
        List<Type> decodedResponse = FunctionReturnDecoder.decode(response.getValue(), function.getOutputParameters());
        // 假設我們期望第一個返回值為 Address
        return decodedResponse.get(0).getValue().toString();
    }

    private void processContractEvent(Log eventLog) {

        String contractAddress = eventLog.getAddress();

        // Check if the event matches the SWAP event signature
        String eventSignature = EventEncoder.encode(SWAP_EVENT);
        if (!eventLog.getTopics().get(0).equals(eventSignature)) {
            return; // This is not a SWAP event
        }

        // Decode indexed parameters
        Address sender = (Address) FunctionReturnDecoder.decodeIndexedValue(
                eventLog.getTopics().get(1), SWAP_EVENT.getIndexedParameters().get(0));
        Address to = (Address) FunctionReturnDecoder.decodeIndexedValue(
                eventLog.getTopics().get(2), SWAP_EVENT.getIndexedParameters().get(1));

        // Decode non-indexed parameters
        List<Type> nonIndexedValues = FunctionReturnDecoder.decode(eventLog.getData(), NON_INDEXED_PARAMETERS);

        Uint256 amount0In = (Uint256) nonIndexedValues.get(0);
        Uint256 amount1In = (Uint256) nonIndexedValues.get(1);
        Uint256 amount0Out = (Uint256) nonIndexedValues.get(2);
        Uint256 amount1Out = (Uint256) nonIndexedValues.get(3);

        String token0 = token0Map.get(contractAddress);
        String token1 = token1Map.get(contractAddress);

        log.info("=======================Swap event detected=======================");
        log.info("Contract Address: " + contractAddress);
        log.info(token0 + " Amount0In: " + amount0In.getValue());
        log.info(token1 + " Amount1In: " + amount1In.getValue());
        log.info(token0 + " Amount0Out: " + amount0Out.getValue());
        log.info(token1 + " Amount1Out: " + amount1Out.getValue());

        Map<String, BigInteger> variations = Map.of("Amount0In", amount0In.getValue(), "Amount0Out", amount0Out.getValue(), "Amount1In", amount1In.getValue(), "Amount1Out", amount1Out.getValue());
        liquidityRepository.updateTokenPair(contractAddress, variations);
    }

}
