package com.oneswap.service;

import com.oneswap.abi.OneswapLimitOrder;
import com.oneswap.bot.CustomGasProvider;
import com.oneswap.model.LimitOrder;
import com.oneswap.model.Liquidity;
import com.oneswap.model.Token;
import com.oneswap.repository.LimitOrderRepository;
import com.oneswap.repository.TokenRepository;
import com.oneswap.util.Web3TransactionUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.web3j.crypto.Credentials;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.DefaultBlockParameterName;
import org.web3j.protocol.core.RemoteFunctionCall;
import org.web3j.protocol.core.methods.response.EthBlock;
import org.web3j.protocol.core.methods.response.TransactionReceipt;
import org.web3j.tx.gas.ContractGasProvider;
import org.web3j.tx.gas.DefaultGasProvider;
import org.web3j.utils.Convert;
import org.web3j.utils.Numeric;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Service
@RequiredArgsConstructor
@Log4j2
public class LimitOrderService {

    @Value("${ONESWAP_FEE}")
    private double ONESWAP_FEE;
    @Value("${BOT_WALLET_PRIVATE_KEY}")
    private String BOT_WALLET_PRIVATE_KEY;
    @Value("${ONESWAP_V1_LIMITORDER_SEPOLIA_ADDRESS}")
    private String oneswapV1LimitOrderAddress;

    @Autowired
    @Qualifier("web3jHttp")
    private Web3j web3j;
    private final TokenRepository tokenRepository;
    private final LimitOrderRepository limitOrderRepository;
    private final Web3TransactionUtil web3TransactionUtil;
    private final ContractGasProvider gasProvider = new DefaultGasProvider();

    // check all limitOrder in MySQL in 12s
    public List<LimitOrder> findMatchOrder(Liquidity liquidity) {

        Token token0 = tokenRepository.findByAddress(liquidity.getToken0());
        Token token1 = tokenRepository.findByAddress(liquidity.getToken1());

        List<LimitOrder> limitOrders = limitOrderRepository.findByTokenInAndTokenOutAndUnfilled(token0, token1);
        limitOrders.addAll(limitOrderRepository.findByTokenOutAndTokenInAndUnfilled(token0, token1));

        List<LimitOrder> matchConditionLimitOrder = new ArrayList<>();

        for (LimitOrder limitOrder : limitOrders) {

            BigInteger amountIn = limitOrder.getAmountIn();
            BigInteger minAmountOut = limitOrder.getMinAmountOut();
            String tokenIn = limitOrder.getTokenIn().getAddress();
            String tokenOut = limitOrder.getTokenOut().getAddress();

            // process data input correctly
            BigInteger reserveIn, reserveOut;
            if (tokenIn.equalsIgnoreCase(liquidity.getToken0())) {// tokenIn is token0 ==> amountIn = amount0
                reserveIn = liquidity.getAmount0();
                reserveOut = liquidity.getAmount1();
            } else if (tokenIn.equalsIgnoreCase(liquidity.getToken1())) {// tokenIn is token1 ==> amountIn = amount1
                reserveIn = liquidity.getAmount1();
                reserveOut = liquidity.getAmount0();
            } else {
                // skip mismatch pool
                continue;
            }

            // calculate amountOut
            BigInteger resultAmount = calculateAmount(reserveIn, reserveOut, amountIn);
            // check liquidity enough
            if (resultAmount.compareTo(minAmountOut) < 0) {
                // skip the pool
                continue;
            }
            matchConditionLimitOrder.add(limitOrder);
        }
        return matchConditionLimitOrder;
    }

    // send the executed limit order transaction
    public String execute(LimitOrder limitOrder, Liquidity liquidity) throws Exception {

        // prepare data about address and credentials
        Credentials credentials = Credentials.create(BOT_WALLET_PRIVATE_KEY);
        OneswapLimitOrder forEstimateContract = OneswapLimitOrder.load(oneswapV1LimitOrderAddress, web3j, credentials, gasProvider);

        // prepare transaction parameters for estimate
        BigInteger orderId = BigInteger.valueOf(limitOrder.getOrderId());
        List<String> path = Arrays.asList(limitOrder.getTokenIn().getAddress(), limitOrder.getTokenOut().getAddress());
        BigInteger exchange = BigInteger.ZERO; // default is 0
        if ("Uniswap".equals(liquidity.getExchanger())) {
            exchange = BigInteger.ZERO; // Uniswap is 0
        } else if ("Balancer".equals(liquidity.getExchanger())) {
            exchange = BigInteger.ONE; // Balancer is 1
        }
        String poolId = liquidity.getPoolId() == null ? "" : liquidity.getPoolId();
        if (poolId.startsWith("0x"))
            poolId = poolId.substring(2);
        byte[] poolIdBytes = Numeric.hexStringToByteArray(poolId);

        // send the transaction
        RemoteFunctionCall<TransactionReceipt> forEstimateCall = forEstimateContract.executeOrder(
                orderId,
                path,
                exchange,
                poolIdBytes
        );

        // prepare ContractGasProvider object
        EthBlock.Block latestBlock = web3j.ethGetBlockByNumber(DefaultBlockParameterName.LATEST, false).send().getBlock();
        BigInteger baseFee = latestBlock.getBaseFeePerGas();
        baseFee = baseFee.multiply(BigInteger.valueOf(15)).divide(BigInteger.valueOf(10));
        if (baseFee == null)
            throw new Exception("Failed to fetch block base fee");
        BigInteger maxPriorityFeePerGas = Convert.toWei("3", Convert.Unit.GWEI).toBigInteger();  // 3 Gwei
        BigInteger maxFeePerGas = baseFee.multiply(BigInteger.valueOf(20)).divide(BigInteger.valueOf(10)).add(maxPriorityFeePerGas);  // baseFee's 200%
        BigInteger gasLimit;
        CustomGasProvider customGasProvider;
        try {
            gasLimit = web3TransactionUtil.estimate(web3j, credentials, oneswapV1LimitOrderAddress, forEstimateCall, maxFeePerGas);
            BigInteger gasPrice = web3j.ethGasPrice().send().getGasPrice();
            BigInteger moreGasPrice = gasPrice.multiply(BigInteger.valueOf(4));

            BigInteger totalFeeInWei = gasLimit.multiply(moreGasPrice);
            BigDecimal totalFeeInEth = Convert.fromWei(new BigDecimal(totalFeeInWei), Convert.Unit.ETHER);
            log.info("Estimated Transaction Fee in ETH: " + totalFeeInEth.toPlainString());

            customGasProvider = new CustomGasProvider(gasLimit, maxFeePerGas, maxPriorityFeePerGas, moreGasPrice);
        } catch (Exception e) {
            e.printStackTrace();
            return "Gas estimation failed: " + e.getMessage();
        }

        // create true Contract object for transaction
        OneswapLimitOrder limitOrderContract = OneswapLimitOrder.load(oneswapV1LimitOrderAddress, web3j, credentials, customGasProvider);

        // create new remoteFunctionCall object for right ContractGasProvider object
        RemoteFunctionCall<TransactionReceipt> transaction = limitOrderContract.executeOrder(
                orderId,
                path,
                exchange,
                poolIdBytes
        );

        // check transaction formation
        if (!web3TransactionUtil.check(web3j, credentials, limitOrderContract, transaction))
            return "transaction checkout failed.";

        // send the transaction
        String hash = web3TransactionUtil.send(transaction);

        log.info("Transaction was done!! The tx is:" + hash);
        return hash;

    }

    private BigInteger calculateAmount(BigInteger reserveIn, BigInteger reserveOut, BigInteger amountIn) {

        // Oneswap fee 0.2%
        double feeMultiplier = (100.0 - ONESWAP_FEE) / 100.0;

        // Uniswap fee 0.3%
        BigInteger amountInAfterYourFee = new BigDecimal(amountIn).multiply(BigDecimal.valueOf(feeMultiplier)).toBigInteger();
        BigInteger amountInWithUniswapFee = amountInAfterYourFee.multiply(BigInteger.valueOf(997));

        // calculate
        BigInteger numerator = amountInWithUniswapFee.multiply(reserveOut);
        BigInteger denominator = reserveIn.multiply(BigInteger.valueOf(1000)).add(amountInWithUniswapFee);

        // return value
        return numerator.divide(denominator);
    }
}
