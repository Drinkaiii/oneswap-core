package com.oneswap.websocket;

import com.oneswap.abi.EventConstants;
import com.oneswap.event.BalancerEventProcessor;
import com.oneswap.event.OneswapAggregatorEventProcessor;
import com.oneswap.event.OneswapLimitOrderEventProcessor;
import com.oneswap.event.UniswapEventProcessor;
import com.oneswap.model.Liquidity;
import com.oneswap.repository.LiquidityRepository;
import com.oneswap.service.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.web3j.abi.EventEncoder;
import org.web3j.abi.TypeReference;
import org.web3j.abi.datatypes.Event;
import org.web3j.abi.datatypes.Type;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.DefaultBlockParameterName;
import org.web3j.protocol.core.methods.request.EthFilter;
import org.web3j.protocol.core.methods.response.EthBlock;

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
    private final NetworkService networkService;
    private final UniswapEventProcessor uniswapEventProcessor;
    private final BalancerEventProcessor balancerEventProcessor;
    private final OneswapAggregatorEventProcessor oneswapAggregatorEventProcessor;
    private final OneswapLimitOrderEventProcessor oneswapLimitOrderEventProcessor;

    // system control
    private boolean start = false;

    List<String> uniswapPairAddresses = new ArrayList<>();
    List balancerPairAddressesAndId = new ArrayList();
    private Set<String> monitoredBalancerPoolAddresses = new HashSet<>();
    String balancerV2VaultContractAddress = "0xBA12222222228d8Ba445958a75a0704d566BF2C8";

    // define Uniswap V2 Router SWAP event
    private static final Event UNISWAP_SWAP_EVENT = EventConstants.UNISWAP_SWAP_EVENT;
    // define Balancer V2 Vault SWAP event
    private static final Event BALANCER_VAULT_SWAP_EVENT = EventConstants.BALANCER_VAULT_SWAP_EVENT;
    // define OneSwap Aggregator SWAP event
    private static final Event ONESWAP_TRADE_EXECUTED_EVENT = EventConstants.ONESWAP_TRADE_EXECUTED_EVENT;
    // define OneSwap LimitOrder place event
    private static final Event ORDER_PLACED_EVENT = EventConstants.ORDER_PLACED_EVENT;
    // define OneSwap LimitOrder cancel event
    private static final Event ORDER_CANCELLED_EVENT = EventConstants.ORDER_CANCELLED_EVENT;
    // define OneSwap LimitOrder execute event
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

        // listen Uniswap V2 Router SWAP event
        for (String contractAddress : uniswapPairAddresses) {
            // subscript events
            listenToEvent(UNISWAP_SWAP_EVENT, contractAddress, uniswapEventProcessor::processSwapEvent);
        }

        // listen Balancer V2 Valut SWAP event
        listenToEvent(BALANCER_VAULT_SWAP_EVENT, balancerV2VaultContractAddress, balancerEventProcessor::processSwapEvent);

        // listen Oneswap V1 Aggregator SWAP event
        listenToEvent(ONESWAP_TRADE_EXECUTED_EVENT, ONESWAP_V1_AGGREGATOR_ADDRESS, oneswapAggregatorEventProcessor::processTradeEvent);

        // listen LimitOrder V1 LimitOrder OrderPlaced event
        listenToEvent(ORDER_PLACED_EVENT, ONESWAP_V1_LIMITORDER_SEPOLIA_ADDRESS, oneswapLimitOrderEventProcessor::processOrderPlacedEvent);

        // listen LimitOrder V1 LimitOrder OrderCancelled event
        listenToEvent(ORDER_CANCELLED_EVENT, ONESWAP_V1_LIMITORDER_SEPOLIA_ADDRESS, oneswapLimitOrderEventProcessor::processOrderCancelledEvent);

        // listen LimitOrder V1 LimitOrder OrderExecuted event
        listenToEvent(ORDER_EXECUTED_EVENT, ONESWAP_V1_LIMITORDER_SEPOLIA_ADDRESS, oneswapLimitOrderEventProcessor::processOrderExecutedEvent);

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

}
