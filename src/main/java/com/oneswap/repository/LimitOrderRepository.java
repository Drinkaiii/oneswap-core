package com.oneswap.repository;

import com.oneswap.model.LimitOrder;
import com.oneswap.model.Token;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LimitOrderRepository extends JpaRepository<LimitOrder, Long> {

    LimitOrder findByOrderId(long orderId);

    @Query("SELECT l FROM LimitOrder l WHERE l.tokenIn = :tokenIn AND l.tokenOut = :tokenOut AND l.status = 'unfilled'")
    List<LimitOrder> findByTokenInAndTokenOutAndUnfilled(@Param("tokenIn") Token tokenIn, @Param("tokenOut") Token tokenOut);

    @Query("SELECT l FROM LimitOrder l WHERE l.tokenIn = :tokenOut AND l.tokenOut = :tokenIn AND l.status = 'unfilled'")
    List<LimitOrder> findByTokenOutAndTokenInAndUnfilled(@Param("tokenIn") Token tokenIn, @Param("tokenOut") Token tokenOut);

}
