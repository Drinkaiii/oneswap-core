package com.oneswap.controller;

import com.oneswap.websocket.BlockchainWebSocketManager;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/1.0/system")
public class SystemController {

    private final BlockchainWebSocketManager blockchainWebSocketManager;

    @GetMapping("/health")
    public ResponseEntity<?> healthCheck(){
        return new ResponseEntity("data", HttpStatus.OK);
    }

    @GetMapping("/monitoring")
    public ResponseEntity<String> toggleMonitoring(@RequestParam boolean enable) {
        blockchainWebSocketManager.setMonitoring(enable);
        String message = enable ? "WebSocket monitoring started." : "WebSocket monitoring stopped.";
        return new ResponseEntity<>(message, HttpStatus.OK);
    }

}
