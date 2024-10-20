package com.oneswap.event;

import org.springframework.stereotype.Service;
import org.web3j.protocol.core.methods.response.Log;

@Service
public interface OneswapLimitOrderEventProcessor {

    void processOrderPlacedEvent(Log eventLog);

    void processOrderCancelledEvent(Log eventLog);

    void processOrderExecutedEvent(Log eventLog);

}
