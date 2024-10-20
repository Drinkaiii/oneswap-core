package com.oneswap.event;

import org.springframework.stereotype.Service;
import org.web3j.protocol.core.methods.response.Log;

@Service
public interface OneswapAggregatorEventProcessor {

    void processTradeEvent(Log eventLog);

}
