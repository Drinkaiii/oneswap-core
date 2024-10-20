package com.oneswap.event.impl;

import com.oneswap.abi.EventConstants;
import com.oneswap.event.OneswapLimitOrderEventProcessor;
import com.oneswap.model.LimitOrder;
import com.oneswap.model.Token;
import com.oneswap.model.User;
import com.oneswap.service.RecordService;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;
import org.web3j.abi.EventEncoder;
import org.web3j.abi.FunctionReturnDecoder;
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
public class OneswapLimitOrderEventProcessorImpl implements OneswapLimitOrderEventProcessor {

    private final RecordService recordService;

    // define OneSwap LimitOrder place event
    private static final Event ORDER_PLACED_EVENT = EventConstants.ORDER_PLACED_EVENT;
    // define OneSwap LimitOrder cancel event
    private static final Event ORDER_CANCELLED_EVENT = EventConstants.ORDER_CANCELLED_EVENT;
    // define OneSwap LimitOrder execute event
    private static final Event ORDER_EXECUTED_EVENT = EventConstants.ORDER_EXECUTED_EVENT;

    @Override
    public void processOrderPlacedEvent(Log eventLog) {

        // Decode event log
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

    @Override
    public void processOrderCancelledEvent(Log eventLog) {

        // Decode event log
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

    @Override
    public void processOrderExecutedEvent(Log eventLog) {

        // Decode event log
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
