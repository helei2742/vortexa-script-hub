package cn.com.vortexa_script_hub.testnet.r2money;


import cn.com.vortexa.common.dto.account.AccountContext;
import cn.com.vortexa.script_node.bot.api.ERC20Api;
import cn.com.vortexa.web3.EthWalletUtil;
import cn.com.vortexa.web3.dto.Web3ChainInfo;
import cn.com.vortexa.web3.exception.ABIInvokeException;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.web3j.abi.FunctionEncoder;
import org.web3j.abi.datatypes.Address;
import org.web3j.abi.datatypes.Function;
import org.web3j.abi.datatypes.generated.Uint256;
import org.web3j.protocol.core.methods.response.TransactionReceipt;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.List;

/**
 * @author helei
 * @since 2025-04-26
 */
@Getter
public class R2PlumeAndSepoliaTestnetApi extends ERC20Api {

    public static final String USDC_ADDRESS = "0xef84994ef411c4981328ffce5fda41cd3803fae4";
    public static final String R2USD_ADDRESS = "0x20c54c5f742f123abb49a982bfe0af47edb38756";
    public static final String SR2USD_ADDRESS = "0xbd6b25c4132f09369c354bee0f7be777d7d434fa";

    public static final String USDC_TO_R2USD_CONTRACT = "0x20c54c5f742f123abb49a982bfe0af47edb38756";
    public static final String R2USD_TO_USDC_CONTRACT = "0x07abd582df3d3472aa687a0489729f9f0424b1e3";
    public static final String STAKE_R2USD_CONTRACT = "0xbd6b25c4132f09369c354bee0f7be777d7d434fa";

    public static final String USDC_TO_R2USD_METHOD_ID = "0x095e7a95";
    public static final String R2USD_TO_USDC_METHOD_ID = "0x3df02124";
    public static final String STAKE_R2USD_METHOD_ID = "0x1a5f0f00";

    private final Web3ChainInfo r2MoneyChainInfo;

    public R2PlumeAndSepoliaTestnetApi(R2MoneyBot r2MoneyBot, Web3ChainInfo chainInfo) {
        super(r2MoneyBot);
        this.r2MoneyChainInfo = chainInfo;
    }

    /**
     * 检查钱包余额
     *
     * @param accountContext accountContext
     * @return Result
     */
    public R2Balance checkWalletBalance(AccountContext accountContext) throws ABIInvokeException {
        BigDecimal usdc = erc20TokenBalance(
                r2MoneyChainInfo.getRpcUrl(), accountContext, USDC_ADDRESS
        );
        BigDecimal r2USD = erc20TokenBalance(
                r2MoneyChainInfo.getRpcUrl(), accountContext, R2USD_ADDRESS
        );
        BigDecimal sr2USD = erc20TokenBalance(
                r2MoneyChainInfo.getRpcUrl(), accountContext, SR2USD_ADDRESS
        );
        return new R2Balance(
                "ac-" + accountContext.getId(),
                accountContext.getWallet().getEthAddress(),
                usdc, r2USD, sr2USD, null
        );
    }

    /**
     * 质押
     * @param accountContext    accountContext
     * @return  TransactionReceipt
     * @throws ABIInvokeException   ABIInvokeException
     */
    public TransactionReceipt stake(AccountContext accountContext, double percent)
            throws ABIInvokeException {
        if (!checkAndApproveToken(r2MoneyChainInfo, accountContext, R2USD_ADDRESS, STAKE_R2USD_CONTRACT, BigDecimal.valueOf(50000000000000000000000000000D))) {
            throw new ABIInvokeException("check and approve fail");
        }

        BigDecimal amount = tokenPercentToAmount(r2MoneyChainInfo.getRpcUrl(), accountContext, R2USD_ADDRESS, percent);
        Integer decimal = erc20Decimal(r2MoneyChainInfo.getRpcUrl(), R2USD_ADDRESS);

        Function function = new Function(
                "", // 方法名留空
                Arrays.asList(
                        new Uint256(EthWalletUtil.parseUnits(amount, decimal)),
                        new Uint256(BigInteger.ZERO),
                        new Uint256(BigInteger.ZERO),
                        new Uint256(BigInteger.ZERO),
                        new Uint256(BigInteger.ZERO),
                        new Uint256(BigInteger.ZERO)
                ),
                List.of()
        );

        // 编码参数
        String encodedParameters = FunctionEncoder.encode(function);
        String paramsOnly = encodedParameters.substring(10); // 去掉函数选择器

        String data = STAKE_R2USD_METHOD_ID  + paramsOnly;

        return onChainABIInvoke(r2MoneyChainInfo, accountContext, STAKE_R2USD_CONTRACT, null, data, 3);
    }

    /**
     * 合约swap
     *
     * @param accountContext  accountContext
     * @param contractAddress contractAddress
     * @param percent         percent
     * @return TransactionReceipt
     * @throws ABIInvokeException ABIInvokeException
     */
    public TransactionReceipt swap(
            AccountContext accountContext,
            String contractAddress,
            String tokenAddress,
            double percent
    ) throws ABIInvokeException {
        BigDecimal amount = tokenPercentToAmount(r2MoneyChainInfo.getRpcUrl(), accountContext, tokenAddress, percent);
        return swap(accountContext, contractAddress, tokenAddress, amount);
    }

    /**
     * 合约swap
     *
     * @param accountContext  accountContext
     * @param contractAddress contractAddress
     * @param amount          amount
     * @return TransactionReceipt
     * @throws ABIInvokeException ABIInvokeException
     */
    public TransactionReceipt swap(
            AccountContext accountContext,
            String contractAddress,
            String tokenAddress,
            BigDecimal amount
    ) throws ABIInvokeException {
        Integer decimal = erc20Decimal(r2MoneyChainInfo.getRpcUrl(), tokenAddress);
        String ethAddress = accountContext.getWallet().getEthAddress();

        checkAndApproveToken(r2MoneyChainInfo, accountContext,
                tokenAddress, contractAddress, EthWalletUtil.formatUnits(BigInteger.valueOf(Long.MAX_VALUE), 6));

        String data = null;
        if (USDC_TO_R2USD_CONTRACT.equals(contractAddress)) {
            data = usd2R2Usd(amount, ethAddress, decimal);
        } else if (R2USD_TO_USDC_CONTRACT.equals(contractAddress)) {
            data = r2Usd2Usd(amount, decimal);
        } else {
            throw new ABIInvokeException("contractAddress not supported");
        }

        return onChainABIInvoke(
                r2MoneyChainInfo,
                accountContext,
                contractAddress,
                null,
                data,
                3
        );
    }

    private static @NotNull String usd2R2Usd(BigDecimal amount, String ethAddress, Integer decimal) {
        Function function = new Function(
                "", // 方法名留空
                Arrays.asList(
                        new Address(ethAddress),
                        new Uint256(EthWalletUtil.parseUnits(amount, decimal)),
                        new Uint256(BigInteger.ZERO),
                        new Uint256(BigInteger.ZERO),
                        new Uint256(BigInteger.ZERO),
                        new Uint256(BigInteger.ZERO),
                        new Uint256(BigInteger.ZERO)
                ),
                List.of()
        );

        // 编码参数
        String encodedParameters = FunctionEncoder.encode(function);
        String paramsOnly = encodedParameters.substring(10); // 去掉函数选择器

        // 拼接最终 data
        return USDC_TO_R2USD_METHOD_ID + paramsOnly;
    }

    public static @NotNull String r2Usd2Usd(BigDecimal amount, Integer decimal) {
        BigInteger amountInt = EthWalletUtil.parseUnits(amount, decimal);
        BigInteger minOutput = EthWalletUtil.calculateMinOutput(amountInt, 2);

        // 固定填充
        String zero64 = "0000000000000000000000000000000000000000000000000000000000000000";
        String one64 = "0000000000000000000000000000000000000000000000000000000000000001";

        // 拼接 amountInWei 和 minOutput（去掉0x，补齐64位）
        String amountInWeiHex = String.format("%064x", amountInt);
        String minOutputHex = String.format("%064x", minOutput);

        // 拼接最终 data
        return R2USD_TO_USDC_METHOD_ID
                + zero64
                + one64
                + amountInWeiHex
                + minOutputHex;
    }
}
