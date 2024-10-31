package com.oneswap.service;

import com.oneswap.model.LimitOrder;
import com.oneswap.model.Transaction;
import org.springframework.stereotype.Service;

import java.math.BigInteger;

@Service
public interface RecordService {

    Transaction saveTransaction(Transaction transaction);

    LimitOrder saveLimitOrder(LimitOrder limitOrder);

    LimitOrder updateLimitOrder(long orderId, String newStatus, BigInteger finalAmountOut);

}
