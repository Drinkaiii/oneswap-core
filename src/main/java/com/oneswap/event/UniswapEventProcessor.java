package com.oneswap.event;

import org.springframework.stereotype.Service;
import org.web3j.protocol.core.methods.response.Log;

@Service
public interface UniswapEventProcessor {

    void processSwapEvent(Log eventLog);

}
