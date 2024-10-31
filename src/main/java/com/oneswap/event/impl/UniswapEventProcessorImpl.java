package com.oneswap.event.impl;

import com.oneswap.abi.EventConstants;
import com.oneswap.event.UniswapEventProcessor;
import com.oneswap.model.LimitOrder;
import com.oneswap.model.Liquidity;
import com.oneswap.repository.LiquidityRepository;
import com.oneswap.repository.impl.LiquidityRepositoryImpl;
import com.oneswap.service.LimitOrderService;
import com.oneswap.service.UniswapService;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;
import org.web3j.abi.EventEncoder;
import org.web3j.abi.FunctionReturnDecoder;
import org.web3j.abi.TypeReference;
import org.web3j.abi.datatypes.Address;
import org.web3j.abi.datatypes.Event;
import org.web3j.abi.datatypes.Type;
import org.web3j.abi.datatypes.generated.Uint256;
import org.web3j.protocol.core.methods.response.Log;

import java.math.BigInteger;
import java.util.List;

@Service
@RequiredArgsConstructor
@Log4j2
public class UniswapEventProcessorImpl implements UniswapEventProcessor {

    private final UniswapService uniswapService;
    private final LimitOrderService limitOrderService;
    private final LiquidityRepository liquidityRepository;

    // define Uniswap V2 Router SWAP event
    private static final Event UNISWAP_SWAP_EVENT = EventConstants.UNISWAP_SWAP_EVENT;
    private static final List<TypeReference<Type>> NON_INDEXED_PARAMETERS = UNISWAP_SWAP_EVENT.getNonIndexedParameters();

    @Override
    public void processSwapEvent(Log eventLog) {

        // get contract address
        String contractAddress = eventLog.getAddress();

        // encode event
        String eventSignature = EventEncoder.encode(UNISWAP_SWAP_EVENT);
        // Check if the event matches the SWAP event signature
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

        String tokenA = uniswapService.getToken0(contractAddress);
        String tokenB = uniswapService.getToken1(contractAddress);

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
}
