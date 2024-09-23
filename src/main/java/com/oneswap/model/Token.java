package com.oneswap.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "token")
public class Token {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @Column(name = "name") // , nullable = false todo
    private String name;

    @Column(name = "symbol") // , nullable = false todo
    private String symbol;

    @Column(name = "address", nullable = false, unique = true)
    private String address;

    @Column(name = "decimals") // , nullable = false todo
    private int decimals;

    @Column(name = "blockchain") // , nullable = false todo
    private String blockchain;

}

