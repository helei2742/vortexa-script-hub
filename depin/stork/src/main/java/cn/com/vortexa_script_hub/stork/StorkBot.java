package cn.com.vortexa_script_hub.stork;

import cn.com.vortexa.script_node.anno.BotApplication;
import cn.com.vortexa.script_node.anno.BotMethod;
import cn.com.vortexa.script_node.bot.AutoLaunchBot;
import cn.com.vortexa.common.dto.config.AutoBotConfig;
import cn.com.vortexa.script_node.constants.MapConfigKey;
import cn.com.vortexa.script_node.service.BotApi;
import cn.com.vortexa.common.constants.BotJobType;
import cn.com.vortexa.common.dto.Result;
import cn.com.vortexa.common.dto.account.AccountContext;

import java.util.List;



@BotApplication(name = "stork_bot", accountParams = MapConfigKey.PASSWORD_KEY)
public class StorkBot extends AutoLaunchBot<StorkBot> {

    private StorkBotAPI storkBotAPI;

    private  String inviteCode;

    @Override
    protected void botInitialized(AutoBotConfig botConfig, BotApi botApi) {
        storkBotAPI = new StorkBotAPI(this);
        inviteCode = botConfig.getConfig(MapConfigKey.INVITE_CODE_KEY);
    }

    @Override
    protected StorkBot getInstance() {
        return this;
    }

    @BotMethod(jobType = BotJobType.ONCE_TASK)
    public Result signUp(AccountContext exampleAC, List<AccountContext> sameABIList) {
        return storkBotAPI.signup(exampleAC, sameABIList, inviteCode);
    }

    @BotMethod(jobType = BotJobType.ONCE_TASK, uniqueAccount = true)
    public void resetPassword(AccountContext exampleAC, List<AccountContext> sameABIList) {
        Result result = storkBotAPI.resetPassword(exampleAC, sameABIList);
        if (result.getSuccess()) {
            logger.info(exampleAC.getSimpleInfo() + " reset password success");
        } else {
            logger.error(exampleAC.getSimpleInfo() + " reset password failed, " + result.getErrorMsg() );
        }
    }

    @BotMethod(jobType = BotJobType.TIMED_TASK, intervalInSecond = 60 * 30)
    public void tokenRefresh(AccountContext accountContext) {
        storkBotAPI.refreshToken(accountContext);
    }

    @BotMethod(jobType = BotJobType.TIMED_TASK, intervalInSecond = 60 * 5)
    public void keepAlive(AccountContext accountContext) {
        storkBotAPI.keepAlive(accountContext);
    }
}
