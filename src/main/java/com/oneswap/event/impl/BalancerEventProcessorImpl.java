package com.oneswap.event.impl;

import com.oneswap.abi.EventConstants;
import com.oneswap.event.BalancerEventProcessor;
import com.oneswap.model.LimitOrder;
import com.oneswap.model.Liquidity;
import com.oneswap.repository.LiquidityRepository;
import com.oneswap.repository.impl.LiquidityRepositoryImpl;
import com.oneswap.service.BalancerService;
import com.oneswap.service.LimitOrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;
import org.web3j.abi.EventEncoder;
import org.web3j.abi.FunctionReturnDecoder;
import org.web3j.abi.datatypes.Address;
import org.web3j.abi.datatypes.Event;
import org.web3j.abi.datatypes.Type;
import org.web3j.abi.datatypes.generated.Bytes32;
import org.web3j.abi.datatypes.generated.Uint256;
import org.web3j.protocol.core.methods.response.Log;
import org.web3j.utils.Numeric;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.List;

@Service
@RequiredArgsConstructor
@Log4j2
public class BalancerEventProcessorImpl implements BalancerEventProcessor {

    private final BalancerService balancerService;
    private final LimitOrderService limitOrderService;
    private final LiquidityRepository liquidityRepository;

    // define Balancer V2 Vault SWAP event
    private static final Event BALANCER_VAULT_SWAP_EVENT = EventConstants.BALANCER_VAULT_SWAP_EVENT;

    @Override
    public void processSwapEvent(Log eventLog) {

        // encode event
        String eventSignature = EventEncoder.encode(BALANCER_VAULT_SWAP_EVENT);
        // Check if the event matches the SWAP event signature
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
