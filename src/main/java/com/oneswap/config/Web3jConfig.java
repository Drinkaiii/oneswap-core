package com.oneswap.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.http.HttpService;
import org.web3j.protocol.websocket.WebSocketService;

import java.io.IOException;

@Configuration
public class Web3jConfig {

    @Value("${INFRA_ETHEREUM_WEBSOCKET_URL}")
    private String websocketUrl;

    @Value("${INFRA_ETHEREUM_HTTP_URL}")
    private String httpUrl;

    @Bean
    public Web3j web3jWebsocket() throws IOException {
        WebSocketService webSocketService = new WebSocketService(websocketUrl, true);
        webSocketService.connect();
        return Web3j.build(webSocketService);
    }

    @Bean
    public Web3j web3jHttp() {
        return Web3j.build(new HttpService(httpUrl));
    }
}
