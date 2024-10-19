package com.oneswap.service;

import com.oneswap.model.LimitOrder;
import com.oneswap.model.Liquidity;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public interface LimitOrderService {

    List<LimitOrder> findMatchOrder(Liquidity liquidity);

    String execute(LimitOrder limitOrder, Liquidity liquidity) throws Exception;

}
