package cn.com.vortexa_script_hub.testnet.r2money;

import cn.com.vortexa.common.constants.BotJobType;
import cn.com.vortexa.common.dto.ACListOptResult;
import cn.com.vortexa.common.dto.BotACJobResult;
import cn.com.vortexa.common.dto.account.AccountContext;
import cn.com.vortexa.common.dto.config.AutoBotConfig;
import cn.com.vortexa.common.util.AnsiColor;
import cn.com.vortexa.common.util.tableprinter.CommandLineTablePrintHelper;
import cn.com.vortexa.script_node.anno.BotApplication;
import cn.com.vortexa.script_node.anno.BotMethod;
import cn.com.vortexa.script_node.bot.AutoLaunchBot;
import cn.com.vortexa.script_node.constants.MapConfigKey;
import cn.com.vortexa.script_node.service.BotApi;
import cn.com.vortexa.web3.dto.OnChainTransactionInfo;
import cn.hutool.core.util.RandomUtil;
import com.alibaba.fastjson.JSONObject;
import org.web3j.protocol.core.methods.response.TransactionReceipt;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * @author helei
 * @since 2025-04-24
 */
@BotApplication(
        name = "R2_Money_Bot",
        configParams = {
                MapConfigKey.MAX_USE_PERCENT_KEY, MapConfigKey.MIN_USE_PERCENT_KEY
        }
)
public class R2MoneyBot extends AutoLaunchBot<R2MoneyBot> {
    private static final String PLUME_GAS_FEE_KEY = "plume_gas_fee";
    private static final String SEPOLIA_GAS_FEE_KEY = "sepolia_gas_fee";

    private static final String RANDOM_DAILY_TASK = "随机执行";
    private static final String CHECK_WALLET_BALANCE = "检查钱包余额";
    private static final String PLUME_TESTNET_USDC_TO_R2USD = "plume测试网 - USDC->R2USD";
    private static final String PLUME_TESTNET_R2USD_TO_USDC = "plume测试网 - R2USD->USDC";
    private static final String PLUME_TESTNET_STAKE_R2USD = "plume测试网 - 质押R2USD";

    private static final String SEPOLIA_TESTNET_USDC_TO_R2USD = "sepolia测试网 - USDC->R2USD";
    private static final String SEPOLIA_TESTNET_R2USD_TO_USDC = "sepolia测试网 - R2USD->USDC";
    private static final String SEPOLIA_TESTNET_STAKE_R2USD = "sepolia测试网 - 质押R2USD";

    private R2MoneyApi r2MoneyApi;

    private Double maxUsePercent;
    private Double minUsePercent;

    @Override
    protected void botInitialized(AutoBotConfig botConfig, BotApi botApi) {
        this.r2MoneyApi = new R2MoneyApi(this);
        addJobExecuteResultHandler(CHECK_WALLET_BALANCE, this::walletBalancePrinter);
        addJobExecuteResultHandler(RANDOM_DAILY_TASK, this::walletBalancePrinter);
        addJobExecuteResultHandler(PLUME_TESTNET_USDC_TO_R2USD, this::transactResultPrinter);
        addJobExecuteResultHandler(PLUME_TESTNET_R2USD_TO_USDC, this::transactResultPrinter);
        addJobExecuteResultHandler(PLUME_TESTNET_STAKE_R2USD, this::transactResultPrinter);
        addJobExecuteResultHandler(SEPOLIA_TESTNET_USDC_TO_R2USD, this::transactResultPrinter);
        addJobExecuteResultHandler(SEPOLIA_TESTNET_R2USD_TO_USDC, this::transactResultPrinter);
        addJobExecuteResultHandler(SEPOLIA_TESTNET_STAKE_R2USD, this::transactResultPrinter);

        this.maxUsePercent = Double.valueOf(botConfig.getConfig(MapConfigKey.MAX_USE_PERCENT_KEY));
        this.minUsePercent = Double.valueOf(botConfig.getConfig(MapConfigKey.MIN_USE_PERCENT_KEY));
    }

    @Override
    protected R2MoneyBot getInstance() {
        return this;
    }

    @BotMethod(
            jobType = BotJobType.TIMED_TASK,
            jobName = RANDOM_DAILY_TASK,
            intervalInSecond = 60 * 60 * 12,
            uniqueAccount = true
    )
    public JSONObject dailyRandoTask(AccountContext accountContext, List<AccountContext> sameACList) {
        int runTimes = 0;
        BigInteger plumeGasUsed = BigInteger.ZERO;
        BigInteger sepoliaGasUsed = BigInteger.ZERO;
        if (RandomUtil.randomBoolean()) {
            runTimes++;
            TransactionReceipt receipt = plumeTestNetUSDCSwap(accountContext, sameACList);
            plumeGasUsed = plumeGasUsed.add(receipt.getGasUsed());
        }
        if (RandomUtil.randomBoolean()) {
            runTimes++;
            plumeGasUsed = plumeGasUsed.add(plumeTestNetR2USDCSwap(accountContext, sameACList).getGasUsed());
        }
        if (RandomUtil.randomBoolean()) {
            runTimes++;
            plumeGasUsed = plumeGasUsed.add(plumeTestNetStake(accountContext, sameACList).getGasUsed());
        }
        if (RandomUtil.randomBoolean()) {
            runTimes++;
            sepoliaGasUsed = sepoliaGasUsed.add(sepoliaTestNetUSDCSwap(accountContext, sameACList).getGasUsed());
        }
        if (RandomUtil.randomBoolean()) {
            runTimes++;
            sepoliaGasUsed = sepoliaGasUsed.add(sepoliaTestNetR2USDCSwap(accountContext, sameACList).getGasUsed());
        }
        if (RandomUtil.randomBoolean()) {
            runTimes++;
            sepoliaGasUsed = sepoliaGasUsed.add(sepoliaTestNetStake(accountContext, sameACList).getGasUsed());
        }
        if (runTimes == 0) {
            runTimes++;
            plumeGasUsed = plumeGasUsed.add(plumeTestNetUSDCSwap(accountContext, sameACList).getGasUsed());
        }

        JSONObject jsonObject = checkWalletBalance(accountContext, sameACList);
        jsonObject.put(PLUME_GAS_FEE_KEY, plumeGasUsed);
        jsonObject.put(SEPOLIA_GAS_FEE_KEY, sepoliaGasUsed);
        return jsonObject;
    }

    @BotMethod(jobType = BotJobType.ONCE_TASK, jobName = CHECK_WALLET_BALANCE, uniqueAccount = true)
    public JSONObject checkWalletBalance(AccountContext accountContext, List<AccountContext> sameACList) {
        try {
            return r2MoneyApi.checkWalletBalance(accountContext);
        } catch (Exception e) {
            logger.error(accountContext.getSimpleInfo() + " balance check error, "
                    + (e.getCause() == null ? e.getMessage() : e.getCause().getMessage()));
            return new JSONObject();
        }
    }

    @BotMethod(jobType = BotJobType.ONCE_TASK, jobName = PLUME_TESTNET_USDC_TO_R2USD, uniqueAccount = true)
    public TransactionReceipt plumeTestNetUSDCSwap(AccountContext accountContext, List<AccountContext> sameACList) {
        try {
            return r2MoneyApi
                    .getR2PlumeTestnetApi()
                    .swap(
                            accountContext,
                            R2PlumeAndSepoliaTestnetApi.USDC_TO_R2USD_CONTRACT,
                            R2PlumeAndSepoliaTestnetApi.USDC_ADDRESS,
                            getUsePercent()
                    );
        } catch (Exception e) {
            String errorMsg = "plume testnet swap error, " + (
                    e.getCause() == null ? e.getMessage() : e.getCause().getMessage()
            );
            logger.error(errorMsg);
            throw new RuntimeException(errorMsg);
        }
    }

    @BotMethod(jobType = BotJobType.ONCE_TASK, jobName = PLUME_TESTNET_R2USD_TO_USDC, uniqueAccount = true)
    public TransactionReceipt plumeTestNetR2USDCSwap(AccountContext accountContext, List<AccountContext> sameACList) {
        try {
            return r2MoneyApi
                    .getR2PlumeTestnetApi()
                    .swap(
                            accountContext,
                            R2PlumeAndSepoliaTestnetApi.R2USD_TO_USDC_CONTRACT,
                            R2PlumeAndSepoliaTestnetApi.R2USD_ADDRESS,
                            getUsePercent()
                    );
        } catch (Exception e) {
            String errorMsg = "plume testnet swap error, " + (
                    e.getCause() == null ? e.getMessage() : e.getCause().getMessage()
            );
            logger.error(errorMsg);
            throw new RuntimeException(errorMsg);
        }
    }

    @BotMethod(jobType = BotJobType.ONCE_TASK, jobName = PLUME_TESTNET_STAKE_R2USD, uniqueAccount = true)
    public TransactionReceipt plumeTestNetStake(AccountContext accountContext, List<AccountContext> sameACList) {
        try {
            return r2MoneyApi
                    .getR2PlumeTestnetApi()
                    .stake(
                            accountContext,
                            getUsePercent()
                    );
        } catch (Exception e) {
            String errorMsg = "plume testnet stake error, " + (
                    e.getCause() == null ? e.getMessage() : e.getCause().getMessage()
            );
            logger.error(errorMsg);
            throw new RuntimeException(errorMsg);
        }
    }


    @BotMethod(jobType = BotJobType.ONCE_TASK, jobName = SEPOLIA_TESTNET_USDC_TO_R2USD, uniqueAccount = true)
    public TransactionReceipt sepoliaTestNetUSDCSwap(AccountContext accountContext, List<AccountContext> sameACList) {
        try {
            return r2MoneyApi
                    .getR2SepoliaTestnetApi()
                    .swap(
                            accountContext,
                            R2PlumeAndSepoliaTestnetApi.USDC_TO_R2USD_CONTRACT,
                            R2PlumeAndSepoliaTestnetApi.USDC_ADDRESS,
                            getUsePercent()
                    );
        } catch (Exception e) {
            String errorMsg = "sepolia testnet swap error, " + e.getMessage();
            logger.error(errorMsg);
            throw new RuntimeException(errorMsg);
        }
    }

    @BotMethod(jobType = BotJobType.ONCE_TASK, jobName = SEPOLIA_TESTNET_R2USD_TO_USDC, uniqueAccount = true)
    public TransactionReceipt sepoliaTestNetR2USDCSwap(AccountContext accountContext, List<AccountContext> sameACList) {
        try {
            return r2MoneyApi
                    .getR2SepoliaTestnetApi()
                    .swap(
                            accountContext,
                            R2PlumeAndSepoliaTestnetApi.R2USD_TO_USDC_CONTRACT,
                            R2PlumeAndSepoliaTestnetApi.R2USD_ADDRESS,
                            getUsePercent()
                    );
        } catch (Exception e) {
            String errorMsg = "sepolia testnet swap error, " + e.getMessage();
            logger.error(errorMsg);
            throw new RuntimeException(errorMsg);
        }
    }

    @BotMethod(jobType = BotJobType.ONCE_TASK, jobName = SEPOLIA_TESTNET_STAKE_R2USD, uniqueAccount = true)
    public TransactionReceipt sepoliaTestNetStake(AccountContext accountContext, List<AccountContext> sameACList) {
        try {
            return r2MoneyApi
                    .getR2SepoliaTestnetApi()
                    .stake(
                            accountContext,
                            getUsePercent()
                    );
        } catch (Exception e) {
            String errorMsg = "sepolia testnet stake error, " + (
                    e.getMessage()
            );
            logger.error(errorMsg);
            throw new RuntimeException(errorMsg);
        }
    }

    /**
     * 打印钱包余额
     *
     * @param acListOptResult acListOptResult
     */
    public void walletBalancePrinter(ACListOptResult acListOptResult) {
        if (acListOptResult == null || !acListOptResult.getSuccess()) {
            logger.error("[%s] execute fail".formatted(CHECK_WALLET_BALANCE));
        } else {
            List<BotACJobResult> results = acListOptResult.getResults();
            List<R2Balance> r2ChainBalances = new ArrayList<>();
            List<R2Balance> spoliaChainBalances = new ArrayList<>();
            for (BotACJobResult result : results) {
                JSONObject jb = (JSONObject) result.getData();
                R2Balance balance = jb.getObject("r2PlumeBalance", R2Balance.class);
                balance.setGasFee(jb.getBigInteger(PLUME_GAS_FEE_KEY));
                R2Balance balanceSepolia = jb.getObject("r2SepoliaBalance", R2Balance.class);
                balanceSepolia.setGasFee(jb.getBigInteger(SEPOLIA_GAS_FEE_KEY));
                r2ChainBalances.add(balance);
                spoliaChainBalances.add(balanceSepolia);
            }

            System.out.println("\n<============"
                    + AnsiColor.colorize("Plume Testnet Balance", AnsiColor.GREEN)
                    + "===================>\n"
            );
            System.out.println(CommandLineTablePrintHelper.generateTableString(r2ChainBalances, R2Balance.class));

            System.out.println("\n<============"
                    + AnsiColor.colorize("Sepolia Testnet Balance", AnsiColor.GREEN)
                    + "===================>\n"
            );
            System.out.println(CommandLineTablePrintHelper.generateTableString(spoliaChainBalances, R2Balance.class));
        }
    }

    public void transactResultPrinter(ACListOptResult acListOptResult) {
        if (acListOptResult == null || !acListOptResult.getSuccess()) {
            logger.error("[%s] execute fail".formatted("transaction"));
        } else {
            List<BotACJobResult> results = acListOptResult.getResults();
            List<OnChainTransactionInfo> onChainTransactionInfos = new ArrayList<>();
            for (BotACJobResult result : results) {
                if (!result.getSuccess()) {
                    onChainTransactionInfos.add(OnChainTransactionInfo.FAIL);
                } else {
                    onChainTransactionInfos.add(OnChainTransactionInfo.fromReceipt((TransactionReceipt) result.getData()));
                }
            }
            System.out.println("\n");
            System.out.println(CommandLineTablePrintHelper.generateTableString(onChainTransactionInfos, OnChainTransactionInfo.class));
        }
    }

    public Double getUsePercent() {
        return ThreadLocalRandom.current().nextDouble(minUsePercent, maxUsePercent);
    }
}
