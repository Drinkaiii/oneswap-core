package com.oneswap.service;

import com.oneswap.model.Transaction;
import com.oneswap.model.User;
import com.oneswap.repository.TransactionRepository;
import com.oneswap.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigInteger;

@Service
@RequiredArgsConstructor
@Transactional
public class RecordService {

    private final TransactionRepository transactionRepository;
    private final UserRepository userRepository;

    public Transaction saveTransaction(Transaction transaction) {
        String userAddress = transaction.getUser().getAddress();
        User user = userRepository.findByAddress(userAddress);
        if (user == null) {
            user = User.builder()
                    .address(userAddress)
                    .build();
            user = userRepository.save(user);
        }
        transaction.setUser(user);
        return transactionRepository.save(transaction);
    }
}

