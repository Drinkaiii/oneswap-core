package com.oneswap.websocket;

import com.oneswap.abi.EventConstants;
import com.oneswap.model.LimitOrder;
import com.oneswap.model.Liquidity;
import com.oneswap.model.Token;
import com.oneswap.model.User;
import com.oneswap.repository.LiquidityRepository;
import com.oneswap.repository.impl.LiquidityRepositoryImpl;
import com.oneswap.service.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
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
import org.web3j.abi.datatypes.generated.Uint8;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.DefaultBlockParameterName;
import org.web3j.protocol.core.methods.request.EthFilter;
import org.web3j.protocol.core.methods.request.Transaction;
import org.web3j.protocol.core.methods.response.EthBlock;
import org.web3j.protocol.core.methods.response.EthCall;
import org.web3j.protocol.core.methods.response.Log;
import org.web3j.utils.Numeric;

import java.math.BigInteger;
import java.util.*;

@Component
@RequiredArgsConstructor
@Log4j2
public class BlockchainEventSubscriber {

    @Value("${blockchain}")
    private String blockchain;
    @Value("${ONESWAP_V1_AGGREGATOR_SEPOLIA_ADDRESS}")
    private String ONESWAP_V1_AGGREGATOR_ADDRESS; // todo make network change infra
    @Value("${ONESWAP_V1_LIMITORDER_SEPOLIA_ADDRESS}")
    private String ONESWAP_V1_LIMITORDER_SEPOLIA_ADDRESS;

    @Autowired
    @Qualifier("web3jWebsocket")
    private Web3j web3j;
    private final UniswapService uniswapService;
    private final BalancerService balancerService;
    private final LiquidityRepository liquidityRepository;
    private final RecordService recordService;
    private final LimitOrderService limitOrderService;
    private final NetworkService networkService;

    // system control
    private boolean start = false;

    List<String> uniswapPairAddresses = new ArrayList<>();
    List balancerPairAddressesAndId = new ArrayList();
    private Set<String> monitoredBalancerPoolAddresses = new HashSet<>();
    String balancerV2VaultContractAddress = "0xBA12222222228d8Ba445958a75a0704d566BF2C8";

    // define Uniswap V2 Router SWAP event
    private static final Event UNISWAP_SWAP_EVENT = EventConstants.UNISWAP_SWAP_EVENT;
    private static final List<TypeReference<Type>> NON_INDEXED_PARAMETERS = UNISWAP_SWAP_EVENT.getNonIndexedParameters();
    // define Balancer V2 Vault SWAP event
    private static final Event BALANCER_VAULT_SWAP_EVENT = EventConstants.BALANCER_VAULT_SWAP_EVENT;
    // define OneSwap SWAP event
    private static final Event ONESWAP_TRADE_EXECUTED_EVENT = EventConstants.ONESWAP_TRADE_EXECUTED_EVENT;
    private static final List<TypeReference<Type>> ONESWAP_TRADE_EXECUTED_NON_INDEXED_PARAMETERS = ONESWAP_TRADE_EXECUTED_EVENT.getNonIndexedParameters();
    // define LimitOrder place event
    private static final Event ORDER_PLACED_EVENT = EventConstants.ORDER_PLACED_EVENT;
    // define LimitOrder cancel event
    private static final Event ORDER_CANCELLED_EVENT = EventConstants.ORDER_CANCELLED_EVENT;
    // define LimitOrder execute event
    private static final Event ORDER_EXECUTED_EVENT = EventConstants.ORDER_EXECUTED_EVENT;

    public void init() {
        if ("Ethereum".equals(blockchain)) {
            uniswapPairAddresses = List.of( // USDT-WETH、WBTC-WETH
                    "0x0d4a11d5eeaac28ec3f61d100daf4d40471f1852",
                    "0xBb2b8038a1640196FbE3e38816F3e67Cba72D940"
            );
            balancerPairAddressesAndId = List.of( // WBTC-WETH、wstETH-WETH Stable Pool、rsETH-WETH Stable Pool
                    List.of("0xa6f548df93de924d73be7d25dc02554c6bd66db500020000000000000000000e", "0xa6f548df93de924d73be7d25dc02554c6bd66db5"),
                    List.of("0x93d199263632a4ef4bb438f1feb99e57b4b5f0bd0000000000000000000005c2", "0x93d199263632a4ef4bb438f1feb99e57b4b5f0bd"),
                    List.of("0x58aadfb1afac0ad7fca1148f3cde6aedf5236b6d00000000000000000000067f", "0x58aadfb1afac0ad7fca1148f3cde6aedf5236b6d"),
                    List.of("0x3de27efa2f1aa663ae5d458857e731c129069f29000200000000000000000588", "0x3de27efa2f1aa663ae5d458857e731c129069f29")
            );
        }
        if ("Sepolia".equals(blockchain)) {
            uniswapPairAddresses = List.of( // WBTC-WETH、WETH-USDT、WETH-ZYBD、WBTC-USDT、WBTC-ZYBD、USDT-ZYBD
                    "0x0E5D4672676a325245C483199a717c45A55a63dF",
                    "0x5424040284DE28CAC43Da5d6abF668a81218E7CB",
                    "0xF854391f43b25b3aa500b564EE300926a3590481",
                    "0x1f6F40e934c31f2a0ce2467fb39723f623196a81",
                    "0xD03Bd6287F733BA162dBb2A5A4633eC6C4b3b8fF",
                    "0x968e949ef7926940f8f2C67291E21a183aB0493B"
            );
            balancerPairAddressesAndId = List.of( // WBTC-WETH、WETH-USDT、WETH-ZYBD、WBTC-USDT、WBTC-ZYBD、USDT-ZYBD
                    List.of("0xc1e0942d3babe2ce30a78d0702a8b5ace651505400020000000000000000014d", "0xc1e0942D3bABE2CE30a78D0702a8b5AcE6515054"),
                    List.of("0x57050c60d9bc41d24d110602a63760294041bd1a00020000000000000000014e", "0x57050c60D9Bc41d24D110602A63760294041bD1a"),
                    List.of("0x0474b5f33c0aba6bfa3b454c04e76bb823c565a800020000000000000000014f", "0x0474B5F33C0abA6BFa3B454c04e76BB823C565a8"),
                    List.of("0xf124ed963141f49b13dbbfa00d15f17014886459000200000000000000000150", "0xF124Ed963141f49B13dbBfa00D15F17014886459"),
                    List.of("0xda2b0e89ec51e5d804037b59084deff8dba49058000200000000000000000151", "0xDa2B0e89EC51e5D804037b59084DeFF8Dba49058"),
                    List.of("0x0f0d63ef93b65a42f24d4f6710138b81ef96d05b000200000000000000000152", "0x0f0D63eF93B65a42F24d4f6710138b81eF96D05b")

            );
        }

        for (Object AddressAndId : balancerPairAddressesAndId) {
            String contractAddress = (String) ((List) AddressAndId).get(1);
            monitoredBalancerPoolAddresses.add(contractAddress.toLowerCase());
        }

        subscribeToNewBlocks();
        fetchInitialReserves();
        subscribeToContractEvents();
    }

    // Every 4 hours refresh data
    @Scheduled(fixedRate = 60000 * 60 * 4)
    public void resynchronizeReserves() {
        if (!start) return;
        fetchInitialReserves();
    }

    private void subscribeToNewBlocks() {
        web3j.blockFlowable(false).subscribe(block -> {
            EthBlock.Block b = block.getBlock();
            BigInteger gasPrice = b.getBaseFeePerGas();
            if (gasPrice != null) {
                networkService.saveGasFee(gasPrice);
            }
        }, error -> {
            log.error("Error in block subscription", error);
        });
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

    private void subscribeToContractEvents() {

        // listen Uniswap V2 SWAP event
        for (String contractAddress : uniswapPairAddresses) {
            // subscript events
            listenToEvent(UNISWAP_SWAP_EVENT, contractAddress, this::processUniswapSwapEvent);
        }

        // listen Balancer V2 SWAP event
        listenToEvent(BALANCER_VAULT_SWAP_EVENT, balancerV2VaultContractAddress, this::processBalancerVaultSwapEvent);

        // listen Oneswap V1 SWAP event
        listenToEvent(ONESWAP_TRADE_EXECUTED_EVENT, ONESWAP_V1_AGGREGATOR_ADDRESS, this::processOneswapTradeExecutedEvent);

        // listen LimitOrder V1 OrderPlaced event
        listenToEvent(ORDER_PLACED_EVENT, ONESWAP_V1_LIMITORDER_SEPOLIA_ADDRESS, this::processOrderPlacedEvent);

        // listen LimitOrder V1 OrderCancelled event
        listenToEvent(ORDER_CANCELLED_EVENT, ONESWAP_V1_LIMITORDER_SEPOLIA_ADDRESS, this::processOrderCancelledEvent);

        // listen LimitOrder V1 OrderExecuted event
        listenToEvent(ORDER_EXECUTED_EVENT, ONESWAP_V1_LIMITORDER_SEPOLIA_ADDRESS, this::processOrderExecutedEvent);

    }

    private String getToken0(String contractAddress) {
        Function function = new Function("token0", Arrays.asList(), Arrays.asList(new TypeReference<Address>() {
        }));
        String token0 = null;
        try {
            token0 = callContractFunction(function, contractAddress);
        } catch (Exception e) {
            log.warn(e.getMessage());
        }
        return token0;
    }

    private String getToken1(String contractAddress) {
        Function function = new Function("token1", Arrays.asList(), Arrays.asList(new TypeReference<Address>() {
        }));
        String token1 = null;
        try {
            token1 = callContractFunction(function, contractAddress);
        } catch (Exception e) {
            log.warn(e.getMessage());
        }
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

    private void listenToEvent(Event event, String contractAddress, EventHandler eventHandler) {
        String eventSignature = EventEncoder.encode(event);
        EthFilter filter = new EthFilter(
                DefaultBlockParameterName.LATEST,
                DefaultBlockParameterName.LATEST,
                contractAddress
        );
        filter.addSingleTopic(eventSignature);

        web3j.ethLogFlowable(filter).subscribe(eventLog -> {
            eventHandler.handleLog(eventLog);
        }, error -> {
            log.error("Error while subscribing to event for contract: " + contractAddress, error);
        });
        log.info("Subscribed to contract events for contract: " + contractAddress);
    }

    public void processUniswapSwapEvent(Log eventLog) {

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

        String tokenA = getToken0(contractAddress);
        String tokenB = getToken1(contractAddress);

        BigInteger amountA = amountAIn.getValue().compareTo(BigInteger.ZERO) != 0
                ? amountAIn.getValue()
                : amountAOut.getValue().negate();

        BigInteger amountB = amountBIn.getValue().compareTo(BigInteger.ZERO) != 0
                ? amountBIn.getValue()
                : amountBOut.getValue().negate();

        Liquidity liquidity = liquidityRepository.updateTokenPair(tokenA, tokenB, amountA, amountB, LiquidityRepositoryImpl.EXCHANGER_UNISWAP);

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

        // find match limit orders
        List<LimitOrder> matchingOrders = limitOrderService.findMatchOrder(liquidity);
        for (LimitOrder order : matchingOrders) {
            // execute the limit order
            String hash = null;
            try {
                // execute the order by the newest liquidity info
                hash = limitOrderService.execute(order, liquidity);
                log.info("Order executed: " + order.getOrderId() + " , hash: " + hash);
            } catch (Exception e) {
                log.warn("Order executed failed: " + order.getOrderId());
                log.warn(e);
            }
        }

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
            Liquidity liquidity = liquidityRepository.updateTokenPair(tokenIn.getValue(), tokenOut.getValue(), amountIn.getValue(), tokenAmountOut, LiquidityRepositoryImpl.EXCHANGER_BALANCER);

            // find match limit orders
            List<LimitOrder> matchingOrders = limitOrderService.findMatchOrder(liquidity);
            for (LimitOrder order : matchingOrders) {
                // execute the limit order
                String hash = null;
                try {
                    // execute the order by the newest liquidity info
                    hash = limitOrderService.execute(order, liquidity);
                    log.info("Order executed: " + order.getOrderId() + " , hash: " + hash);
                } catch (Exception e) {
                    log.warn("Order executed failed: " + order.getOrderId());
                    log.warn(e);
                }
            }

        }
    }

    private void processOneswapTradeExecutedEvent(Log eventLog) {
        // Decode event log
        String eventSignature = EventEncoder.encode(ONESWAP_TRADE_EXECUTED_EVENT);
        if (!eventLog.getTopics().get(0).equals(eventSignature)) {
            return;
        }

        // Decode indexed parameters
        Address trader = (Address) FunctionReturnDecoder.decodeIndexedValue(eventLog.getTopics().get(1), ONESWAP_TRADE_EXECUTED_EVENT.getIndexedParameters().get(0));
        Address tokenIn = (Address) FunctionReturnDecoder.decodeIndexedValue(eventLog.getTopics().get(2), ONESWAP_TRADE_EXECUTED_EVENT.getIndexedParameters().get(1));
        Address tokenOut = (Address) FunctionReturnDecoder.decodeIndexedValue(eventLog.getTopics().get(3), ONESWAP_TRADE_EXECUTED_EVENT.getIndexedParameters().get(2));

        // Decode non-indexed parameters
        List<Type> nonIndexedValues = FunctionReturnDecoder.decode(eventLog.getData(), ONESWAP_TRADE_EXECUTED_NON_INDEXED_PARAMETERS);
        Uint256 amountIn = (Uint256) nonIndexedValues.get(0);
        Uint256 amountOut = (Uint256) nonIndexedValues.get(1);
        Uint8 exchange = (Uint8) nonIndexedValues.get(2);

        User user = User.builder().address(trader.getValue()).build();
        Token tokenInObject = Token.builder().address(tokenIn.getValue()).build();
        Token tokenOutObject = Token.builder().address(tokenOut.getValue()).build();
        com.oneswap.model.Transaction transaction = com.oneswap.model.Transaction.builder()
                .user(user)
                .transactionHash(eventLog.getTransactionHash())
                .blockchain(blockchain)
                .exchanger(exchange.getValue().intValue())
                .tokenIn(tokenInObject)
                .tokenOut(tokenOutObject)
                .amountIn(amountIn.getValue())
                .amountOut(amountOut.getValue())
                .build();
        recordService.saveTransaction(transaction);
    }

    private void processOrderPlacedEvent(Log eventLog) {
        String eventSignature = EventEncoder.encode(ORDER_PLACED_EVENT);
        if (!eventLog.getTopics().get(0).equals(eventSignature)) {
            return;
        }

        // Decode indexed parameters
        Uint256 orderId = (Uint256) FunctionReturnDecoder.decodeIndexedValue(
                eventLog.getTopics().get(1), ORDER_PLACED_EVENT.getIndexedParameters().get(0));
        Address trader = (Address) FunctionReturnDecoder.decodeIndexedValue(
                eventLog.getTopics().get(2), ORDER_PLACED_EVENT.getIndexedParameters().get(1));

        // Decode non-indexed parameters
        List<Type> nonIndexedValues = FunctionReturnDecoder.decode(eventLog.getData(), ORDER_PLACED_EVENT.getNonIndexedParameters());
        Address tokenIn = (Address) nonIndexedValues.get(0);
        Address tokenOut = (Address) nonIndexedValues.get(1);
        Uint256 amountIn = (Uint256) nonIndexedValues.get(2);
        Uint256 minAmountOut = (Uint256) nonIndexedValues.get(3);

        log.info("=======================OrderPlaced event detected=======================");
        log.info("OrderId: " + orderId.getValue());
        log.info("User: " + trader.getValue());
        log.info("TokenIn: " + tokenIn.getValue());
        log.info("TokenOut: " + tokenOut.getValue());
        log.info("AmountIn: " + amountIn.getValue());
        log.info("MinAmountOut: " + minAmountOut.getValue());

        User user = User.builder().address(trader.getValue()).build();
        Token tokenInObject = Token.builder().address(tokenIn.getValue()).build();
        Token tokenOutObject = Token.builder().address(tokenOut.getValue()).build();
        LimitOrder limitOrder = LimitOrder.builder()
                .status(LimitOrder.STATUS_UN_FILLED)
                .orderId(orderId.getValue().longValue())
                .user(user)
                .tokenIn(tokenInObject)
                .tokenOut(tokenOutObject)
                .amountIn(amountIn.getValue())
                .minAmountOut(minAmountOut.getValue())
                .finalAmountOut(BigInteger.ZERO)
                .build();
        recordService.saveLimitOrder(limitOrder);
    }

    private void processOrderCancelledEvent(Log eventLog) {
        String eventSignature = EventEncoder.encode(ORDER_CANCELLED_EVENT);
        if (!eventLog.getTopics().get(0).equals(eventSignature)) {
            return;
        }

        // Decode indexed parameters
        Uint256 orderId = (Uint256) FunctionReturnDecoder.decodeIndexedValue(
                eventLog.getTopics().get(1), ORDER_CANCELLED_EVENT.getIndexedParameters().get(0));
        Address userAddress = (Address) FunctionReturnDecoder.decodeIndexedValue(
                eventLog.getTopics().get(2), ORDER_CANCELLED_EVENT.getIndexedParameters().get(1));

        log.info("=======================OrderCancelled event detected=======================");
        log.info("OrderId: " + orderId.getValue());
        log.info("User: " + userAddress.getValue());

        recordService.updateLimitOrder(orderId.getValue().longValue(), LimitOrder.STATUS_CANCELED, BigInteger.ZERO);

    }

    private void processOrderExecutedEvent(Log eventLog) {
        String eventSignature = EventEncoder.encode(ORDER_EXECUTED_EVENT);
        if (!eventLog.getTopics().get(0).equals(eventSignature)) {
            return;
        }

        // Decode indexed parameters
        Uint256 orderId = (Uint256) FunctionReturnDecoder.decodeIndexedValue(
                eventLog.getTopics().get(1), ORDER_EXECUTED_EVENT.getIndexedParameters().get(0));
        Address user = (Address) FunctionReturnDecoder.decodeIndexedValue(
                eventLog.getTopics().get(2), ORDER_EXECUTED_EVENT.getIndexedParameters().get(1));

        // Decode non-indexed parameters
        List<Type> nonIndexedValues = FunctionReturnDecoder.decode(eventLog.getData(), ORDER_EXECUTED_EVENT.getNonIndexedParameters());
        Uint256 amountOut = (Uint256) nonIndexedValues.get(0);

        log.info("=======================OrderExecuted event detected=======================");
        log.info("OrderId: " + orderId.getValue());
        log.info("User: " + user.getValue());
        log.info("AmountOut: " + amountOut.getValue());

        recordService.updateLimitOrder(orderId.getValue().longValue(), LimitOrder.STATUS_FILLED, amountOut.getValue());

    }

}
