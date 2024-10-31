package com.oneswap.event.impl;

import com.oneswap.abi.EventConstants;
import com.oneswap.event.OneswapAggregatorEventProcessor;
import com.oneswap.model.Token;
import com.oneswap.model.User;
import com.oneswap.service.RecordService;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.web3j.abi.EventEncoder;
import org.web3j.abi.FunctionReturnDecoder;
import org.web3j.abi.TypeReference;
import org.web3j.abi.datatypes.Address;
import org.web3j.abi.datatypes.Event;
import org.web3j.abi.datatypes.Type;
import org.web3j.abi.datatypes.generated.Uint256;
import org.web3j.abi.datatypes.generated.Uint8;
import org.web3j.protocol.core.methods.response.Log;

import java.util.List;

@Service
@RequiredArgsConstructor
@Log4j2
public class OneswapAggregatorEventProcessorImpl implements OneswapAggregatorEventProcessor {

    @Value("${blockchain}")
    private String blockchain;

    private final RecordService recordService;

    // define OneSwap Aggregator SWAP event
    private static final Event ONESWAP_TRADE_EXECUTED_EVENT = EventConstants.ONESWAP_TRADE_EXECUTED_EVENT;
    private static final List<TypeReference<Type>> ONESWAP_TRADE_EXECUTED_NON_INDEXED_PARAMETERS = ONESWAP_TRADE_EXECUTED_EVENT.getNonIndexedParameters();

    @Override
    public void processTradeEvent(Log eventLog) {

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

}
