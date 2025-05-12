package cn.com.vortexa_script_hub.testnet.r2money;


import cn.com.vortexa.common.dto.account.AccountContext;
import cn.com.vortexa.web3.constants.Web3ChainDict;
import cn.com.vortexa.web3.exception.ABIInvokeException;
import com.alibaba.fastjson.JSONObject;
import lombok.Getter;

import java.io.File;

/**
 * @author helei
 * @since 2025-04-24
 */
public class R2MoneyApi {
    private static final String R2MONEY_CHAIN_FILE_NAME = "r2-chain-info.yaml";
    private static final String SEPOLIA_CHAIN_NAME = "Sepolia";
    public static final String R2MONEY_CHAIN_NAME = "Plume_Testnet";

    private final R2MoneyBot r2MoneyBot;
    @Getter
    private final Web3ChainDict web3ChainDict;
    @Getter
    private final R2PlumeAndSepoliaTestnetApi r2PlumeTestnetApi;
    @Getter
    private final R2PlumeAndSepoliaTestnetApi r2SepoliaTestnetApi;

    public R2MoneyApi(R2MoneyBot r2MoneyBot) {
        this.r2MoneyBot = r2MoneyBot;
        Web3ChainDict defaultChainDict = Web3ChainDict.INSTANCE;
        Web3ChainDict customChainDict = Web3ChainDict.loadCustomConfigDict(
                r2MoneyBot.getAutoBotConfig().getMetaInfo().getResourceDir() + File.separator + R2MONEY_CHAIN_FILE_NAME
        );
        customChainDict.marge(defaultChainDict);
        web3ChainDict = customChainDict;

        r2PlumeTestnetApi = new R2PlumeAndSepoliaTestnetApi(r2MoneyBot, web3ChainDict.getChainInfo(R2MONEY_CHAIN_NAME));
        r2SepoliaTestnetApi = new R2PlumeAndSepoliaTestnetApi(r2MoneyBot, web3ChainDict.getChainInfo(SEPOLIA_CHAIN_NAME));
    }

    /**
     * 检查钱包资金
     *
     * @param accountContext    accountContext
     * @return  Result
     * @throws ABIInvokeException  ABIInvokeException
     */
    public JSONObject checkWalletBalance(AccountContext accountContext) throws ABIInvokeException {
        R2Balance r2PlumeBalance = r2PlumeTestnetApi.checkWalletBalance(accountContext);
        R2Balance r2SepoliaBalance = r2SepoliaTestnetApi.checkWalletBalance(accountContext);

        JSONObject jb = new JSONObject();
        jb.put("r2PlumeBalance", r2PlumeBalance);
        jb.put("r2SepoliaBalance", r2SepoliaBalance);

        return jb;
    }
}
