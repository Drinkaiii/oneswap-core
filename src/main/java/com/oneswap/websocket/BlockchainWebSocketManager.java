package com.oneswap.websocket;

import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;
import org.web3j.protocol.Web3j;

@Component
@RequiredArgsConstructor
@Log4j2
public class BlockchainWebSocketManager {

    @Qualifier("web3jWebsocket")
    private Web3j web3j;
    private final ApplicationContext applicationContext;
    private final BlockchainEventSubscriber blockchainEventSubscriber;

    private boolean start = false;

    public void setMonitoring(boolean start) {

        if (this.start == start) {
            log.info("WebSocket monitoring is already " + (start ? "started" : "stopped") + ".");
            return;
        }

        this.start = start;
        if (start) {
            try {
                if (web3j == null) {
                    web3j = applicationContext.getBean("web3jWebsocket", Web3j.class);
                }
                blockchainEventSubscriber.init();
                log.info("WebSocket monitoring started.");
            } catch (Exception e) {
                log.error("Error while starting WebSocket monitoring", e);
            }
        } else {
            close();
            log.info("WebSocket monitoring stopped.");
        }
    }

    @PreDestroy
    public void close() {
        if (web3j != null) {
            web3j.shutdown();
            log.info("Web3j connection closed.");
        }
    }

}
