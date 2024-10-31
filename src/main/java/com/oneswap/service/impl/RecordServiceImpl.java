package com.oneswap.service.impl;

import com.oneswap.model.LimitOrder;
import com.oneswap.model.Token;
import com.oneswap.model.Transaction;
import com.oneswap.model.User;
import com.oneswap.repository.LimitOrderRepository;
import com.oneswap.repository.TokenRepository;
import com.oneswap.repository.TransactionRepository;
import com.oneswap.repository.UserRepository;
import com.oneswap.service.RecordService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigInteger;

@Service
@RequiredArgsConstructor
@Transactional
public class RecordServiceImpl implements RecordService {

    private final TransactionRepository transactionRepository;
    private final LimitOrderRepository limitOrderRepository;
    private final UserRepository userRepository;
    private final TokenRepository tokenRepository;

    @Transactional
    public Transaction saveTransaction(Transaction transaction) {

        // User
        String userAddress = transaction.getUser().getAddress();
        User user = userRepository.findByAddress(userAddress);
        if (user == null) {
            user = User.builder()
                    .address(userAddress)
                    .build();
            user = userRepository.save(user);
        }
        transaction.setUser(user);

        // TokenIn and TokenOut
        Token tokenIn = saveOrGetToken(transaction.getTokenIn());
        Token tokenOut = saveOrGetToken(transaction.getTokenOut());
        transaction.setTokenIn(tokenIn);
        transaction.setTokenOut(tokenOut);

        // save data
        return transactionRepository.save(transaction);
    }

    private Token saveOrGetToken(Token token) {

        Token existingToken = tokenRepository.findByAddress(token.getAddress());
        if (existingToken == null) {
            existingToken = tokenRepository.save(token);
        }
        return existingToken;
    }

    public LimitOrder saveLimitOrder(LimitOrder limitOrder) {

        // User
        String userAddress = limitOrder.getUser().getAddress();
        User user = userRepository.findByAddress(userAddress);
        if (user == null) {
            user = User.builder()
                    .address(userAddress)
                    .build();
            user = userRepository.save(user);
        }
        limitOrder.setUser(user);

        // TokenIn and TokenOut
        Token tokenIn = saveOrGetToken(limitOrder.getTokenIn());
        Token tokenOut = saveOrGetToken(limitOrder.getTokenOut());
        limitOrder.setTokenIn(tokenIn);
        limitOrder.setTokenOut(tokenOut);

        // save data
        return limitOrderRepository.save(limitOrder);
    }

    public LimitOrder updateLimitOrder(long orderId, String newStatus, BigInteger finalAmountOut) {

        LimitOrder limitOrder = limitOrderRepository.findByOrderId(orderId);
        if (limitOrder == null)
            throw new IllegalArgumentException("Order with ID " + orderId + " not found.");

        if (LimitOrder.STATUS_FILLED.equals(newStatus))
            limitOrder.setFinalAmountOut(finalAmountOut);
        limitOrder.setStatus(newStatus);

        return limitOrderRepository.save(limitOrder);
    }

}

