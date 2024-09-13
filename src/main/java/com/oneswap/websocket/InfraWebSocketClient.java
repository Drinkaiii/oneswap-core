package com.oneswap.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import jakarta.websocket.*;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;

@ClientEndpoint
@Log4j2
@Component
public class InfraWebSocketClient {

    @Value("${INFRA_ETHEREUM_WEBSOCKET_URL}")
    private String endpoint;

    private Session session;
    private ObjectMapper objectMapper = new ObjectMapper();

    // auto create WebSocket connection
    @PostConstruct
    public void init() {
        connect();
    }

    // create WebSocket connection
    public void connect() {
        try {
            WebSocketContainer container = ContainerProvider.getWebSocketContainer();
            container.connectToServer(this, new URI(endpoint));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // trigger about creating WebSocket connection
    @OnOpen
    public void onOpen(Session session) {
        this.session = session;
        log.info("Connected to Infra WebSocket endpoint successfully.");
        // publish data
        subscribeToPriceChange();
    }

    // trigger about getting WebSocket message
    @OnMessage
    public void onMessage(String message) {
        log.info("Received message: " + message);
    }

    // trigger about closing WebSocket connection
    @OnClose
    public void onClose(Session session, CloseReason closeReason) {
        System.out.println("WebSocket connection to Infra closed: " + closeReason);
    }

    // trigger about occurring error
    @OnError
    public void onError(Throwable throwable) {
        throwable.printStackTrace();
    }

    // publish message infra
    public void sendMessage(String message) {
        try {
            if (session != null && session.isOpen()) {
                session.getBasicRemote().sendText(message);
            } else {
                System.err.println("Session is not open or null.");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // publish query message
    private void subscribeToPriceChange() {
        try {
            Map<String, Object> params = new HashMap<>();
            Map<String, Object> filter = new HashMap<>();
            filter.put("address", "0x0d4a11d5eeaac28ec3f61d100daf4d40471f1852"); // USDT-WETH
            params.put("jsonrpc", "2.0");
            params.put("method", "eth_subscribe");
            params.put("params", new Object[]{"logs", filter});
            params.put("id", 1);
            String subscriptionMessage = objectMapper.writeValueAsString(params);
            log.info("Subscription message: " + subscriptionMessage);
            sendMessage(subscriptionMessage);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
