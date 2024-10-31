package com.oneswap.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.methods.response.NetVersion;
import org.web3j.protocol.http.HttpService;
import org.web3j.protocol.websocket.WebSocketService;

import java.io.IOException;

@Configuration
public class Web3jConfig {

    @Value("${INFRA_ETHEREUM_WEBSOCKET_URL}")
    private String infraEthereumWebsocketUrl;

    @Value("${INFRA_ETHEREUM_HTTP_URL}")
    private String infraEthereumHttpUrl;

    @Value("${INFRA_SEPOLIA_WEBSOCKET_URL}")
    private String infraSepoliaWebsocketUrl;

    @Value("${INFRA_SEPOLIA_HTTP_URL}")
    private String infraSepoliaHttpUrl;

    @Value("${blockchain:Ethereum}")
    private String blockchain;

    @Bean
    @Lazy
    public Web3j web3jWebsocket() throws IOException {
        WebSocketService webSocketService;
        switch (blockchain) {
            case "Ethereum":
                webSocketService = new WebSocketService(infraEthereumWebsocketUrl, true);

                break;
            case "Sepolia":
                webSocketService = new WebSocketService(infraSepoliaWebsocketUrl, true);
                break;
            default:
                webSocketService = new WebSocketService(infraEthereumWebsocketUrl, true);
        }
        webSocketService.connect();
        return Web3j.build(webSocketService);
    }

    @Bean
    public Web3j web3jHttp() {
        Web3j web3j;
        switch (blockchain) {
            case "Ethereum":
                web3j = Web3j.build(new HttpService(infraEthereumHttpUrl));
                break;
            case "Sepolia":
                web3j = Web3j.build(new HttpService(infraSepoliaHttpUrl));
                break;
            default:
                web3j = Web3j.build(new HttpService(infraEthereumHttpUrl));
        }
        return web3j;
    }

}
