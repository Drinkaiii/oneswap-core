package com.oneswap.repository;

import com.oneswap.model.LimitOrder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface LimitOrderRepository extends JpaRepository<LimitOrder, Long> {

    LimitOrder findByOrderId(long orderId);

}
