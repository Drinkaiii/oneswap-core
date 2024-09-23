package com.oneswap.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigInteger;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "limit_order")
public class LimitOrder {

    public static String STATUS_UN_FILLED = "unfilled";
    public static String STATUS_FILLED = "filled";
    public static String STATUS_CANCELED = "canceled";
    public static String STATUS_ERROR = "error";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @Column(name = "status", nullable = false)
    private String status;

    @Column(name = "order_id", nullable = false, unique = true)
    private long orderId;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne
    @JoinColumn(name = "token_in", nullable = false)
    private Token tokenIn;

    @ManyToOne
    @JoinColumn(name = "token_out", nullable = false)
    private Token tokenOut;

    @Column(name = "amount_in", nullable = false)
    private BigInteger amountIn;

    @Column(name = "min_amount_out", nullable = false)
    private BigInteger minAmountOut;

    @Column(name = "final_amount_out")
    private BigInteger finalAmountOut;

}
