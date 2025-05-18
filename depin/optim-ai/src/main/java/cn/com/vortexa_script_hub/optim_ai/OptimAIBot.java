package cn.com.vortexa_script_hub.optim_ai;

import cn.com.vortexa.common.constants.BotJobType;
import cn.com.vortexa.common.dto.Result;
import cn.com.vortexa.common.dto.account.AccountContext;
import cn.com.vortexa.common.dto.config.AutoBotConfig;
import cn.com.vortexa.script_node.anno.BotApplication;
import cn.com.vortexa.script_node.anno.BotMethod;
import cn.com.vortexa.script_node.bot.AutoLaunchBot;
import cn.com.vortexa.script_node.constants.MapConfigKey;
import cn.com.vortexa.script_node.dto.AccountRewordSnapshot;
import cn.com.vortexa.script_node.service.BotApi;

import java.util.List;

/**
 * @author helei
 * @since 2025/3/24 17:14
 */
@BotApplication(configParams = {OptimAIBot.TWO_CAPTCHA_API_KEY})
public class OptimAIBot extends AutoLaunchBot<OptimAIBot> {

    public static final String TWO_CAPTCHA_API_KEY = "two_captcha_api_key";
    public static final String PASSWORD_KEY = "password";

    private OptimAIAPI optimAIAPI;

    private String inviteCode;

    @Override
    protected void botInitialized(AutoBotConfig botConfig, BotApi botApi) {
        this.optimAIAPI = new OptimAIAPI(this);
        this.inviteCode = botConfig.getConfig(MapConfigKey.INVITE_CODE_KEY);
    }

    @Override
    protected OptimAIBot getInstance() {
        return this;
    }

    @BotMethod(
            jobType = BotJobType.ONCE_TASK
    )
    public Result registry(AccountContext uniAC, List<AccountContext> sameIdACList) {
        return optimAIAPI.registry(uniAC, inviteCode);
    }

    @BotMethod(
            jobType = BotJobType.ONCE_TASK
    )
    public Result login(AccountContext accountContext) throws Exception {
        return optimAIAPI.login(accountContext);
    }

    @BotMethod(jobType = BotJobType.QUERY_REWARD, intervalInSecond = 24 * 60 * 60, concurrentCount = 10, uniqueAccount = true)
    public AccountRewordSnapshot queryReword(AccountContext accountContext, List<AccountContext> sameAC) {
        return optimAIAPI.queryReword(accountContext);
    }

    @BotMethod(jobType = BotJobType.TIMED_TASK, intervalInSecond = 24 * 60 * 60 * 3, concurrentCount = 5)
    public void tokenRefresh(AccountContext accountContext) {
        Result result = null;
        try {
            result = optimAIAPI.login(accountContext);
        } catch (Exception e) {
            logger.error(accountContext.getSimpleInfo() + " refresh token error, " + e.getMessage());
        }
        if (result.getSuccess()) {
            logger.info(accountContext.getSimpleInfo() + " refresh token success");
        } else {
            logger.error(accountContext.getSimpleInfo() + " refresh token error, " + result.getErrorMsg());
        }
    }

    @BotMethod(
            jobType = BotJobType.TIMED_TASK,
            intervalInSecond = 8 * 60,
            concurrentCount = 50
    )
    public void keepAlive(AccountContext accountContext) {
        Result result = optimAIAPI.keepAlive(accountContext);
        if (result.getSuccess()) {
            logger.info(accountContext.getSimpleInfo() + " keepalive success");
        } else {
            logger.error(accountContext.getSimpleInfo() + " keepalive error, " + result.getErrorMsg());
        }
    }
}
