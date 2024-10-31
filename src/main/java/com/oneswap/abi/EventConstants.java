package com.oneswap.abi;

import org.web3j.abi.TypeReference;
import org.web3j.abi.datatypes.Address;
import org.web3j.abi.datatypes.Event;
import org.web3j.abi.datatypes.Type;
import org.web3j.abi.datatypes.generated.Bytes32;
import org.web3j.abi.datatypes.generated.Uint256;
import org.web3j.abi.datatypes.generated.Uint8;

import java.util.Arrays;
import java.util.List;

public class EventConstants {

    // define Uniswap V2 Router SWAP event
    public static final Event UNISWAP_SWAP_EVENT = new Event("Swap",
            Arrays.asList(
                    new TypeReference<Address>(true) {
                    },    // indexed sender
                    new TypeReference<Uint256>() {
                    },        // amount0In
                    new TypeReference<Uint256>() {
                    },        // amount1In
                    new TypeReference<Uint256>() {
                    },        // amount0Out
                    new TypeReference<Uint256>() {
                    },        // amount1Out
                    new TypeReference<Address>(true) {
                    }     // indexed to
            )
    );
    public static final List<TypeReference<Type>> NON_INDEXED_PARAMETERS = UNISWAP_SWAP_EVENT.getNonIndexedParameters();

    // define Balancer V2 Vault SWAP event
    public static final Event BALANCER_VAULT_SWAP_EVENT = new Event("Swap",
            Arrays.asList(
                    new TypeReference<Bytes32>(true) {
                    }, // poolId
                    new TypeReference<Address>(true) {
                    }, // tokenIn
                    new TypeReference<Address>(true) {
                    }, // tokenOut
                    new TypeReference<Uint256>() {
                    },     // amountIn
                    new TypeReference<Uint256>() {
                    }      // amountOut
            )
    );

    // define OneSwap SWAP event
    public static final Event ONESWAP_TRADE_EXECUTED_EVENT = new Event("TradeExecuted",
            Arrays.asList(
                    new TypeReference<Address>(true) {
                    },  // trader
                    new TypeReference<Address>(true) {
                    },  // tokenIn
                    new TypeReference<Address>(true) {
                    },  // tokenOut
                    new TypeReference<Uint256>() {
                    },      // amountIn
                    new TypeReference<Uint256>() {
                    },      // amountOut
                    new TypeReference<Uint8>() {
                    }         // exchange (enum type)
            )
    );
    public static final List<TypeReference<Type>> ONESWAP_TRADE_EXECUTED_NON_INDEXED_PARAMETERS = ONESWAP_TRADE_EXECUTED_EVENT.getNonIndexedParameters();

    // define LimitOrder place event
    public static final Event ORDER_PLACED_EVENT = new Event("OrderPlaced",
            Arrays.asList(
                    new TypeReference<Uint256>(true) {
                    },  // orderId (indexed)
                    new TypeReference<Address>(true) {
                    },  // user (indexed)
                    new TypeReference<Address>() {
                    },      // tokenIn
                    new TypeReference<Address>() {
                    },      // tokenOut
                    new TypeReference<Uint256>() {
                    },      // amountIn
                    new TypeReference<Uint256>() {
                    }       // minAmountOut
            )
    );
    // define LimitOrder cancel event
    public static final Event ORDER_CANCELLED_EVENT = new Event("OrderCancelled",
            Arrays.asList(
                    new TypeReference<Uint256>(true) {
                    },  // orderId (indexed)
                    new TypeReference<Address>(true) {
                    }   // user (indexed)
            )
    );
    // define LimitOrder execute event
    public static final Event ORDER_EXECUTED_EVENT = new Event("OrderExecuted",
            Arrays.asList(
                    new TypeReference<Uint256>(true) {
                    },  // orderId (indexed)
                    new TypeReference<Address>(true) {
                    },  // user (indexed)
                    new TypeReference<Uint256>() {
                    }       // amountOut
            )
    );

}
