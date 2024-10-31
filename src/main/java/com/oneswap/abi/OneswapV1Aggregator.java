package com.oneswap.abi;

import io.reactivex.Flowable;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.web3j.abi.EventEncoder;
import org.web3j.abi.TypeReference;
import org.web3j.abi.datatypes.Address;
import org.web3j.abi.datatypes.Event;
import org.web3j.abi.datatypes.Function;
import org.web3j.abi.datatypes.Type;
import org.web3j.abi.datatypes.generated.Uint256;
import org.web3j.abi.datatypes.generated.Uint8;
import org.web3j.crypto.Credentials;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.DefaultBlockParameter;
import org.web3j.protocol.core.RemoteFunctionCall;
import org.web3j.protocol.core.methods.request.EthFilter;
import org.web3j.protocol.core.methods.response.BaseEventResponse;
import org.web3j.protocol.core.methods.response.Log;
import org.web3j.protocol.core.methods.response.TransactionReceipt;
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
public class OneswapV1Aggregator extends Contract {
    public static final String BINARY = "Bin file was not provided";

    public static final String FUNC_BALANCERVAULT = "balancerVault";

    public static final String FUNC_FEEPERCENT = "feePercent";

    public static final String FUNC_OWNER = "owner";

    public static final String FUNC_SWAPTOKENS = "swapTokens";

    public static final String FUNC_UNISWAPROUTER = "uniswapRouter";

    public static final String FUNC_UPDATEFEEPERCENT = "updateFeePercent";

    public static final String FUNC_WITHDRAW = "withdraw";

    public static final Event TRADEEXECUTED_EVENT = new Event("TradeExecuted", 
            Arrays.<TypeReference<?>>asList(new TypeReference<Address>(true) {}, new TypeReference<Address>(true) {}, new TypeReference<Address>(true) {}, new TypeReference<Uint256>() {}, new TypeReference<Uint256>() {}, new TypeReference<Uint8>() {}));
    ;

    @Deprecated
    protected OneswapV1Aggregator(String contractAddress, Web3j web3j, Credentials credentials,
                                  BigInteger gasPrice, BigInteger gasLimit) {
        super(BINARY, contractAddress, web3j, credentials, gasPrice, gasLimit);
    }

    protected OneswapV1Aggregator(String contractAddress, Web3j web3j, Credentials credentials,
                                  ContractGasProvider contractGasProvider) {
        super(BINARY, contractAddress, web3j, credentials, contractGasProvider);
    }

    @Deprecated
    protected OneswapV1Aggregator(String contractAddress, Web3j web3j,
                                  TransactionManager transactionManager, BigInteger gasPrice, BigInteger gasLimit) {
        super(BINARY, contractAddress, web3j, transactionManager, gasPrice, gasLimit);
    }

    protected OneswapV1Aggregator(String contractAddress, Web3j web3j,
                                  TransactionManager transactionManager, ContractGasProvider contractGasProvider) {
        super(BINARY, contractAddress, web3j, transactionManager, contractGasProvider);
    }

    public static List<TradeExecutedEventResponse> getTradeExecutedEvents(
            TransactionReceipt transactionReceipt) {
        List<Contract.EventValuesWithLog> valueList = staticExtractEventParametersWithLog(TRADEEXECUTED_EVENT, transactionReceipt);
        ArrayList<TradeExecutedEventResponse> responses = new ArrayList<TradeExecutedEventResponse>(valueList.size());
        for (Contract.EventValuesWithLog eventValues : valueList) {
            TradeExecutedEventResponse typedResponse = new TradeExecutedEventResponse();
            typedResponse.log = eventValues.getLog();
            typedResponse.trader = (String) eventValues.getIndexedValues().get(0).getValue();
            typedResponse.tokenIn = (String) eventValues.getIndexedValues().get(1).getValue();
            typedResponse.tokenOut = (String) eventValues.getIndexedValues().get(2).getValue();
            typedResponse.amountIn = (BigInteger) eventValues.getNonIndexedValues().get(0).getValue();
            typedResponse.amountOut = (BigInteger) eventValues.getNonIndexedValues().get(1).getValue();
            typedResponse.exchange = (BigInteger) eventValues.getNonIndexedValues().get(2).getValue();
            responses.add(typedResponse);
        }
        return responses;
    }

    public static TradeExecutedEventResponse getTradeExecutedEventFromLog(Log log) {
        Contract.EventValuesWithLog eventValues = staticExtractEventParametersWithLog(TRADEEXECUTED_EVENT, log);
        TradeExecutedEventResponse typedResponse = new TradeExecutedEventResponse();
        typedResponse.log = log;
        typedResponse.trader = (String) eventValues.getIndexedValues().get(0).getValue();
        typedResponse.tokenIn = (String) eventValues.getIndexedValues().get(1).getValue();
        typedResponse.tokenOut = (String) eventValues.getIndexedValues().get(2).getValue();
        typedResponse.amountIn = (BigInteger) eventValues.getNonIndexedValues().get(0).getValue();
        typedResponse.amountOut = (BigInteger) eventValues.getNonIndexedValues().get(1).getValue();
        typedResponse.exchange = (BigInteger) eventValues.getNonIndexedValues().get(2).getValue();
        return typedResponse;
    }

    public Flowable<TradeExecutedEventResponse> tradeExecutedEventFlowable(EthFilter filter) {
        return web3j.ethLogFlowable(filter).map(log -> getTradeExecutedEventFromLog(log));
    }

    public Flowable<TradeExecutedEventResponse> tradeExecutedEventFlowable(
            DefaultBlockParameter startBlock, DefaultBlockParameter endBlock) {
        EthFilter filter = new EthFilter(startBlock, endBlock, getContractAddress());
        filter.addSingleTopic(EventEncoder.encode(TRADEEXECUTED_EVENT));
        return tradeExecutedEventFlowable(filter);
    }

    public RemoteFunctionCall<String> balancerVault() {
        final Function function = new Function(FUNC_BALANCERVAULT, 
                Arrays.<Type>asList(), 
                Arrays.<TypeReference<?>>asList(new TypeReference<Address>() {}));
        return executeRemoteCallSingleValueReturn(function, String.class);
    }

    public RemoteFunctionCall<BigInteger> feePercent() {
        final Function function = new Function(FUNC_FEEPERCENT, 
                Arrays.<Type>asList(), 
                Arrays.<TypeReference<?>>asList(new TypeReference<Uint256>() {}));
        return executeRemoteCallSingleValueReturn(function, BigInteger.class);
    }

    public RemoteFunctionCall<String> owner() {
        final Function function = new Function(FUNC_OWNER, 
                Arrays.<Type>asList(), 
                Arrays.<TypeReference<?>>asList(new TypeReference<Address>() {}));
        return executeRemoteCallSingleValueReturn(function, String.class);
    }

    public RemoteFunctionCall<TransactionReceipt> swapTokens(String tokenIn, String tokenOut,
            BigInteger amountIn, BigInteger minAmountOut, BigInteger deadline, BigInteger exchange,
            List<String> path, byte[] poolId) {
        final Function function = new Function(
                FUNC_SWAPTOKENS, 
                Arrays.<Type>asList(new org.web3j.abi.datatypes.Address(160, tokenIn), 
                new org.web3j.abi.datatypes.Address(160, tokenOut), 
                new org.web3j.abi.datatypes.generated.Uint256(amountIn), 
                new org.web3j.abi.datatypes.generated.Uint256(minAmountOut), 
                new org.web3j.abi.datatypes.generated.Uint256(deadline), 
                new org.web3j.abi.datatypes.generated.Uint8(exchange), 
                new org.web3j.abi.datatypes.DynamicArray<org.web3j.abi.datatypes.Address>(
                        org.web3j.abi.datatypes.Address.class,
                        org.web3j.abi.Utils.typeMap(path, org.web3j.abi.datatypes.Address.class)), 
                new org.web3j.abi.datatypes.generated.Bytes32(poolId)), 
                Collections.<TypeReference<?>>emptyList());
        return executeRemoteCallTransaction(function);
    }

    public RemoteFunctionCall<String> uniswapRouter() {
        final Function function = new Function(FUNC_UNISWAPROUTER, 
                Arrays.<Type>asList(), 
                Arrays.<TypeReference<?>>asList(new TypeReference<Address>() {}));
        return executeRemoteCallSingleValueReturn(function, String.class);
    }

    public RemoteFunctionCall<TransactionReceipt> updateFeePercent(BigInteger newFeePercent) {
        final Function function = new Function(
                FUNC_UPDATEFEEPERCENT, 
                Arrays.<Type>asList(new org.web3j.abi.datatypes.generated.Uint256(newFeePercent)), 
                Collections.<TypeReference<?>>emptyList());
        return executeRemoteCallTransaction(function);
    }

    public RemoteFunctionCall<TransactionReceipt> withdraw(String token, BigInteger amount) {
        final Function function = new Function(
                FUNC_WITHDRAW, 
                Arrays.<Type>asList(new org.web3j.abi.datatypes.Address(160, token), 
                new org.web3j.abi.datatypes.generated.Uint256(amount)), 
                Collections.<TypeReference<?>>emptyList());
        return executeRemoteCallTransaction(function);
    }

    @Deprecated
    public static OneswapV1Aggregator load(String contractAddress, Web3j web3j,
                                           Credentials credentials, BigInteger gasPrice, BigInteger gasLimit) {
        return new OneswapV1Aggregator(contractAddress, web3j, credentials, gasPrice, gasLimit);
    }

    @Deprecated
    public static OneswapV1Aggregator load(String contractAddress, Web3j web3j,
                                           TransactionManager transactionManager, BigInteger gasPrice, BigInteger gasLimit) {
        return new OneswapV1Aggregator(contractAddress, web3j, transactionManager, gasPrice, gasLimit);
    }

    public static OneswapV1Aggregator load(String contractAddress, Web3j web3j,
                                           Credentials credentials, ContractGasProvider contractGasProvider) {
        return new OneswapV1Aggregator(contractAddress, web3j, credentials, contractGasProvider);
    }

    public static OneswapV1Aggregator load(String contractAddress, Web3j web3j,
                                           TransactionManager transactionManager, ContractGasProvider contractGasProvider) {
        return new OneswapV1Aggregator(contractAddress, web3j, transactionManager, contractGasProvider);
    }

    public static class TradeExecutedEventResponse extends BaseEventResponse {
        public String trader;

        public String tokenIn;

        public String tokenOut;

        public BigInteger amountIn;

        public BigInteger amountOut;

        public BigInteger exchange;
    }
}
