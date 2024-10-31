package com.oneswap.websocket;

import org.web3j.protocol.core.methods.response.Log;

public interface EventHandler {

    void handleLog(Log eventLog);

}
