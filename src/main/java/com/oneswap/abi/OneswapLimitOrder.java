package com.oneswap.abi;

import io.reactivex.Flowable;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import org.web3j.abi.EventEncoder;
import org.web3j.abi.TypeReference;
import org.web3j.abi.datatypes.Address;
import org.web3j.abi.datatypes.Bool;
import org.web3j.abi.datatypes.Event;
import org.web3j.abi.datatypes.Function;
import org.web3j.abi.datatypes.Type;
import org.web3j.abi.datatypes.generated.Uint256;
import org.web3j.crypto.Credentials;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.DefaultBlockParameter;
import org.web3j.protocol.core.RemoteFunctionCall;
import org.web3j.protocol.core.methods.request.EthFilter;
import org.web3j.protocol.core.methods.response.BaseEventResponse;
import org.web3j.protocol.core.methods.response.Log;
import org.web3j.protocol.core.methods.response.TransactionReceipt;
import org.web3j.tuples.generated.Tuple6;
import org.web3j.tx.Contract;
import org.web3j.tx.TransactionManager;
import org.web3j.tx.gas.ContractGasProvider;

/**
 * <p>Auto generated code.
 * <p><strong>Do not modify!</strong>
 * <p>Please use the <a href="https://docs.web3j.io/command_line.html">web3j command line tools</a>,
 * or the org.web3j.codegen.SolidityFunctionWrapperGenerator in the 
 * <a href="https://github.com/hyperledger/web3j/tree/main/codegen">codegen module</a> to update.
 *
 * <p>Generated with web3j version 1.6.1.
 */
@SuppressWarnings("rawtypes")
public class OneswapLimitOrder extends Contract {
    public static final String BINARY = "Bin file was not provided";

    public static final String FUNC_CANCELORDER = "cancelOrder";

    public static final String FUNC_DEXAGGREGATOR = "dexAggregator";

    public static final String FUNC_EXECUTEORDER = "executeOrder";

    public static final String FUNC_NEXTORDERID = "nextOrderId";

    public static final String FUNC_ORDERS = "orders";

    public static final String FUNC_PLACEORDER = "placeOrder";

    public static final Event ORDERCANCELLED_EVENT = new Event("OrderCancelled", 
            Arrays.<TypeReference<?>>asList(new TypeReference<Uint256>(true) {}, new TypeReference<Address>(true) {}));
    ;

    public static final Event ORDEREXECUTED_EVENT = new Event("OrderExecuted", 
            Arrays.<TypeReference<?>>asList(new TypeReference<Uint256>(true) {}, new TypeReference<Address>(true) {}, new TypeReference<Uint256>() {}));
    ;

    public static final Event ORDERPLACED_EVENT = new Event("OrderPlaced", 
            Arrays.<TypeReference<?>>asList(new TypeReference<Uint256>(true) {}, new TypeReference<Address>(true) {}, new TypeReference<Address>() {}, new TypeReference<Address>() {}, new TypeReference<Uint256>() {}, new TypeReference<Uint256>() {}));
    ;

    @Deprecated
    protected OneswapLimitOrder(String contractAddress, Web3j web3j, Credentials credentials,
                                BigInteger gasPrice, BigInteger gasLimit) {
        super(BINARY, contractAddress, web3j, credentials, gasPrice, gasLimit);
    }

    protected OneswapLimitOrder(String contractAddress, Web3j web3j, Credentials credentials,
                                ContractGasProvider contractGasProvider) {
        super(BINARY, contractAddress, web3j, credentials, contractGasProvider);
    }

    @Deprecated
    protected OneswapLimitOrder(String contractAddress, Web3j web3j,
                                TransactionManager transactionManager, BigInteger gasPrice, BigInteger gasLimit) {
        super(BINARY, contractAddress, web3j, transactionManager, gasPrice, gasLimit);
    }

    protected OneswapLimitOrder(String contractAddress, Web3j web3j,
                                TransactionManager transactionManager, ContractGasProvider contractGasProvider) {
        super(BINARY, contractAddress, web3j, transactionManager, contractGasProvider);
    }

    public static List<OrderCancelledEventResponse> getOrderCancelledEvents(
            TransactionReceipt transactionReceipt) {
        List<Contract.EventValuesWithLog> valueList = staticExtractEventParametersWithLog(ORDERCANCELLED_EVENT, transactionReceipt);
        ArrayList<OrderCancelledEventResponse> responses = new ArrayList<OrderCancelledEventResponse>(valueList.size());
        for (Contract.EventValuesWithLog eventValues : valueList) {
            OrderCancelledEventResponse typedResponse = new OrderCancelledEventResponse();
            typedResponse.log = eventValues.getLog();
            typedResponse.orderId = (BigInteger) eventValues.getIndexedValues().get(0).getValue();
            typedResponse.user = (String) eventValues.getIndexedValues().get(1).getValue();
            responses.add(typedResponse);
        }
        return responses;
    }

    public static OrderCancelledEventResponse getOrderCancelledEventFromLog(Log log) {
        Contract.EventValuesWithLog eventValues = staticExtractEventParametersWithLog(ORDERCANCELLED_EVENT, log);
        OrderCancelledEventResponse typedResponse = new OrderCancelledEventResponse();
        typedResponse.log = log;
        typedResponse.orderId = (BigInteger) eventValues.getIndexedValues().get(0).getValue();
        typedResponse.user = (String) eventValues.getIndexedValues().get(1).getValue();
        return typedResponse;
    }

    public Flowable<OrderCancelledEventResponse> orderCancelledEventFlowable(EthFilter filter) {
        return web3j.ethLogFlowable(filter).map(log -> getOrderCancelledEventFromLog(log));
    }

    public Flowable<OrderCancelledEventResponse> orderCancelledEventFlowable(
            DefaultBlockParameter startBlock, DefaultBlockParameter endBlock) {
        EthFilter filter = new EthFilter(startBlock, endBlock, getContractAddress());
        filter.addSingleTopic(EventEncoder.encode(ORDERCANCELLED_EVENT));
        return orderCancelledEventFlowable(filter);
    }

    public static List<OrderExecutedEventResponse> getOrderExecutedEvents(
            TransactionReceipt transactionReceipt) {
        List<Contract.EventValuesWithLog> valueList = staticExtractEventParametersWithLog(ORDEREXECUTED_EVENT, transactionReceipt);
        ArrayList<OrderExecutedEventResponse> responses = new ArrayList<OrderExecutedEventResponse>(valueList.size());
        for (Contract.EventValuesWithLog eventValues : valueList) {
            OrderExecutedEventResponse typedResponse = new OrderExecutedEventResponse();
            typedResponse.log = eventValues.getLog();
            typedResponse.orderId = (BigInteger) eventValues.getIndexedValues().get(0).getValue();
            typedResponse.user = (String) eventValues.getIndexedValues().get(1).getValue();
            typedResponse.amountOut = (BigInteger) eventValues.getNonIndexedValues().get(0).getValue();
            responses.add(typedResponse);
        }
        return responses;
    }

    public static OrderExecutedEventResponse getOrderExecutedEventFromLog(Log log) {
        Contract.EventValuesWithLog eventValues = staticExtractEventParametersWithLog(ORDEREXECUTED_EVENT, log);
        OrderExecutedEventResponse typedResponse = new OrderExecutedEventResponse();
        typedResponse.log = log;
        typedResponse.orderId = (BigInteger) eventValues.getIndexedValues().get(0).getValue();
        typedResponse.user = (String) eventValues.getIndexedValues().get(1).getValue();
        typedResponse.amountOut = (BigInteger) eventValues.getNonIndexedValues().get(0).getValue();
        return typedResponse;
    }

    public Flowable<OrderExecutedEventResponse> orderExecutedEventFlowable(EthFilter filter) {
        return web3j.ethLogFlowable(filter).map(log -> getOrderExecutedEventFromLog(log));
    }

    public Flowable<OrderExecutedEventResponse> orderExecutedEventFlowable(
            DefaultBlockParameter startBlock, DefaultBlockParameter endBlock) {
        EthFilter filter = new EthFilter(startBlock, endBlock, getContractAddress());
        filter.addSingleTopic(EventEncoder.encode(ORDEREXECUTED_EVENT));
        return orderExecutedEventFlowable(filter);
    }

    public static List<OrderPlacedEventResponse> getOrderPlacedEvents(
            TransactionReceipt transactionReceipt) {
        List<Contract.EventValuesWithLog> valueList = staticExtractEventParametersWithLog(ORDERPLACED_EVENT, transactionReceipt);
        ArrayList<OrderPlacedEventResponse> responses = new ArrayList<OrderPlacedEventResponse>(valueList.size());
        for (Contract.EventValuesWithLog eventValues : valueList) {
            OrderPlacedEventResponse typedResponse = new OrderPlacedEventResponse();
            typedResponse.log = eventValues.getLog();
            typedResponse.orderId = (BigInteger) eventValues.getIndexedValues().get(0).getValue();
            typedResponse.user = (String) eventValues.getIndexedValues().get(1).getValue();
            typedResponse.tokenIn = (String) eventValues.getNonIndexedValues().get(0).getValue();
            typedResponse.tokenOut = (String) eventValues.getNonIndexedValues().get(1).getValue();
            typedResponse.amountIn = (BigInteger) eventValues.getNonIndexedValues().get(2).getValue();
            typedResponse.minAmountOut = (BigInteger) eventValues.getNonIndexedValues().get(3).getValue();
            responses.add(typedResponse);
        }
        return responses;
    }

    public static OrderPlacedEventResponse getOrderPlacedEventFromLog(Log log) {
        Contract.EventValuesWithLog eventValues = staticExtractEventParametersWithLog(ORDERPLACED_EVENT, log);
        OrderPlacedEventResponse typedResponse = new OrderPlacedEventResponse();
        typedResponse.log = log;
        typedResponse.orderId = (BigInteger) eventValues.getIndexedValues().get(0).getValue();
        typedResponse.user = (String) eventValues.getIndexedValues().get(1).getValue();
        typedResponse.tokenIn = (String) eventValues.getNonIndexedValues().get(0).getValue();
        typedResponse.tokenOut = (String) eventValues.getNonIndexedValues().get(1).getValue();
        typedResponse.amountIn = (BigInteger) eventValues.getNonIndexedValues().get(2).getValue();
        typedResponse.minAmountOut = (BigInteger) eventValues.getNonIndexedValues().get(3).getValue();
        return typedResponse;
    }

    public Flowable<OrderPlacedEventResponse> orderPlacedEventFlowable(EthFilter filter) {
        return web3j.ethLogFlowable(filter).map(log -> getOrderPlacedEventFromLog(log));
    }

    public Flowable<OrderPlacedEventResponse> orderPlacedEventFlowable(
            DefaultBlockParameter startBlock, DefaultBlockParameter endBlock) {
        EthFilter filter = new EthFilter(startBlock, endBlock, getContractAddress());
        filter.addSingleTopic(EventEncoder.encode(ORDERPLACED_EVENT));
        return orderPlacedEventFlowable(filter);
    }

    public RemoteFunctionCall<TransactionReceipt> cancelOrder(BigInteger orderId) {
        final Function function = new Function(
                FUNC_CANCELORDER, 
                Arrays.<Type>asList(new org.web3j.abi.datatypes.generated.Uint256(orderId)), 
                Collections.<TypeReference<?>>emptyList());
        return executeRemoteCallTransaction(function);
    }

    public RemoteFunctionCall<String> dexAggregator() {
        final Function function = new Function(FUNC_DEXAGGREGATOR, 
                Arrays.<Type>asList(), 
                Arrays.<TypeReference<?>>asList(new TypeReference<Address>() {}));
        return executeRemoteCallSingleValueReturn(function, String.class);
    }

    public RemoteFunctionCall<TransactionReceipt> executeOrder(BigInteger orderId,
            List<String> path, BigInteger exchange, byte[] poolId) {
        final Function function = new Function(
                FUNC_EXECUTEORDER, 
                Arrays.<Type>asList(new org.web3j.abi.datatypes.generated.Uint256(orderId), 
                new org.web3j.abi.datatypes.DynamicArray<org.web3j.abi.datatypes.Address>(
                        org.web3j.abi.datatypes.Address.class,
                        org.web3j.abi.Utils.typeMap(path, org.web3j.abi.datatypes.Address.class)), 
                new org.web3j.abi.datatypes.generated.Uint8(exchange), 
                new org.web3j.abi.datatypes.generated.Bytes32(poolId)), 
                Collections.<TypeReference<?>>emptyList());
        return executeRemoteCallTransaction(function);
    }

    public RemoteFunctionCall<BigInteger> nextOrderId() {
        final Function function = new Function(FUNC_NEXTORDERID, 
                Arrays.<Type>asList(), 
                Arrays.<TypeReference<?>>asList(new TypeReference<Uint256>() {}));
        return executeRemoteCallSingleValueReturn(function, BigInteger.class);
    }

    public RemoteFunctionCall<Tuple6<String, String, String, BigInteger, BigInteger, Boolean>> orders(
            BigInteger param0) {
        final Function function = new Function(FUNC_ORDERS, 
                Arrays.<Type>asList(new org.web3j.abi.datatypes.generated.Uint256(param0)), 
                Arrays.<TypeReference<?>>asList(new TypeReference<Address>() {}, new TypeReference<Address>() {}, new TypeReference<Address>() {}, new TypeReference<Uint256>() {}, new TypeReference<Uint256>() {}, new TypeReference<Bool>() {}));
        return new RemoteFunctionCall<Tuple6<String, String, String, BigInteger, BigInteger, Boolean>>(function,
                new Callable<Tuple6<String, String, String, BigInteger, BigInteger, Boolean>>() {
                    @Override
                    public Tuple6<String, String, String, BigInteger, BigInteger, Boolean> call()
                            throws Exception {
                        List<Type> results = executeCallMultipleValueReturn(function);
                        return new Tuple6<String, String, String, BigInteger, BigInteger, Boolean>(
                                (String) results.get(0).getValue(), 
                                (String) results.get(1).getValue(), 
                                (String) results.get(2).getValue(), 
                                (BigInteger) results.get(3).getValue(), 
                                (BigInteger) results.get(4).getValue(), 
                                (Boolean) results.get(5).getValue());
                    }
                });
    }

    public RemoteFunctionCall<TransactionReceipt> placeOrder(String tokenIn, String tokenOut,
            BigInteger amountIn, BigInteger minAmountOut) {
        final Function function = new Function(
                FUNC_PLACEORDER, 
                Arrays.<Type>asList(new org.web3j.abi.datatypes.Address(160, tokenIn), 
                new org.web3j.abi.datatypes.Address(160, tokenOut), 
                new org.web3j.abi.datatypes.generated.Uint256(amountIn), 
                new org.web3j.abi.datatypes.generated.Uint256(minAmountOut)), 
                Collections.<TypeReference<?>>emptyList());
        return executeRemoteCallTransaction(function);
    }

    @Deprecated
    public static OneswapLimitOrder load(String contractAddress, Web3j web3j,
                                         Credentials credentials, BigInteger gasPrice, BigInteger gasLimit) {
        return new OneswapLimitOrder(contractAddress, web3j, credentials, gasPrice, gasLimit);
    }

    @Deprecated
    public static OneswapLimitOrder load(String contractAddress, Web3j web3j,
                                         TransactionManager transactionManager, BigInteger gasPrice, BigInteger gasLimit) {
        return new OneswapLimitOrder(contractAddress, web3j, transactionManager, gasPrice, gasLimit);
    }

    public static OneswapLimitOrder load(String contractAddress, Web3j web3j,
                                         Credentials credentials, ContractGasProvider contractGasProvider) {
        return new OneswapLimitOrder(contractAddress, web3j, credentials, contractGasProvider);
    }

    public static OneswapLimitOrder load(String contractAddress, Web3j web3j,
                                         TransactionManager transactionManager, ContractGasProvider contractGasProvider) {
        return new OneswapLimitOrder(contractAddress, web3j, transactionManager, contractGasProvider);
    }

    public static class OrderCancelledEventResponse extends BaseEventResponse {
        public BigInteger orderId;

        public String user;
    }

    public static class OrderExecutedEventResponse extends BaseEventResponse {
        public BigInteger orderId;

        public String user;

        public BigInteger amountOut;
    }

    public static class OrderPlacedEventResponse extends BaseEventResponse {
        public BigInteger orderId;

        public String user;

        public String tokenIn;

        public String tokenOut;

        public BigInteger amountIn;

        public BigInteger minAmountOut;
    }
}
