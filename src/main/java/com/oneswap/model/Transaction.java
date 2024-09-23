package com.oneswap.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigInteger;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "transaction")
public class Transaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "transaction_hash", nullable = false, unique = true)
    private String transactionHash;

    @Column(name = "blockchain", nullable = false)
    private String blockchain;

    @Column(name = "exchanger", nullable = false)
    private int exchanger;

    @ManyToOne
    @JoinColumn(name = "token_in", nullable = false)
    private Token tokenIn;

    @ManyToOne
    @JoinColumn(name = "token_out", nullable = false)
    private Token tokenOut;

    @Column(name = "amount_in", precision = 65, scale = 0, nullable = false)
    private BigInteger amountIn;

    @Column(name = "amount_out", precision = 65, scale = 0, nullable = false)
    private BigInteger amountOut;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

}
