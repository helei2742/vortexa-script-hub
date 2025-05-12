package cn.com.vortexa_script_hub.stork;

import cn.com.vortexa.common.constants.HttpMethod;
import cn.com.vortexa.common.dto.Result;
import cn.com.vortexa.common.dto.account.AccountContext;
import cn.com.vortexa.mail.constants.MailProtocolType;
import cn.com.vortexa.mail.factory.MailReaderFactory;
import cn.com.vortexa.mail.reader.MailReader;
import cn.com.vortexa.script_node.constants.MapConfigKey;
import cn.hutool.core.util.BooleanUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;

import javax.mail.Message;
import javax.mail.MessagingException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
public class StorkBotAPI {

    private static final String STORK_SIGNED_PRICE_API = "https://app-api.jp.stork-oracle.network/v1/stork_signed_prices";
    private static final String VALIDATE_SIGNED_PRICE_API = "https://app-api.jp.stork-oracle.network/v1/stork_signed_prices/validations";
    private static final String PASSWORD_RESET_API = "https://app-auth.jp.stork-oracle.network/recover";
    private static final String PASSWORD_REST_VERIFY_API = "https://app-auth.jp.stork-oracle.network/verify";
    private static final String REFRESH_TOKEN_URL = "https://app-auth.jp.stork-oracle.network/token";
    private static final String MAIL_FROM = "noreply@stork.network";

    private static final Pattern V_CODE_PATTERN = Pattern.compile("\\b\\d{6}\\b");

    public static final String PASSWORD_KEY = "stork_password";
    public static final String IMAP_PASSWORD_KEY = "imap_password";
    public static final String PASSWORD_RESET_FINISH_KEY = "password_reset_finish";

    private static final MailReader mailReader = MailReaderFactory.getMailReader(MailProtocolType.imap,
            "imap.gmail.com", "993", true);

    private final StorkBot bot;

    public StorkBotAPI(StorkBot bot) {
        this.bot = bot;
    }

    /**
     * 注册
     *
     * @param exampleAC     exampleAC
     * @param sameABIACList sameABIACList
     * @param inviteCode    inviteCode
     * @return Result
     */
    public Result signup(AccountContext exampleAC, List<AccountContext> sameABIACList, String inviteCode) {
        return Result.fail("waiting update");
    }


    public Result resetPassword(AccountContext exampleAC, List<AccountContext> sameABIList) {
        String finish = exampleAC.getParam(PASSWORD_RESET_FINISH_KEY);
        if (BooleanUtil.isTrue(Boolean.parseBoolean(finish))) {
            return Result.ok("reset finish");
        }

        String simpleInfo = exampleAC.getSimpleInfo();
        bot.logger.debug("start reset %s password".formatted(simpleInfo));
        try {
            JSONObject reset = new JSONObject();
            reset.put("email", exampleAC.getAccountBaseInfo().getEmail());
            String checkCode = bot.syncJSONRequest(
                    exampleAC.getProxy(),
                    PASSWORD_RESET_API,
                    HttpMethod.POST,
                    exampleAC.getBrowserEnv().generateHeaders(),
                    null,
                    reset,
                    () -> simpleInfo + " send reset password request"
            ).thenApply(result -> queryCheckCode(exampleAC)).get();
            bot.logger.debug(simpleInfo + " check code take success: " + checkCode);

            JSONObject verify = new JSONObject();
            verify.put("email", exampleAC.getAccountBaseInfo().getEmail());
            verify.put("token", checkCode);
            verify.put("type", "recovery");

            return bot.syncJSONRequest(
                    exampleAC.getProxy(),
                    PASSWORD_REST_VERIFY_API,
                    HttpMethod.POST,
                    exampleAC.getBrowserEnv().generateHeaders(),
                    null,
                    verify,
                    () -> simpleInfo + " send reset password verify request"
            ).thenApply(result -> {
                String accessToken = result.getString("access_token");
                Map<String, String> headers = exampleAC.getBrowserEnv().generateHeaders();
                headers.put("Authorization", "Bearer " + accessToken);
                headers.put("origin", "https://app.stork.network");
                headers.put("referer", "https://app.stork.network/");
                headers.put("content-type", "application/json");
                String newPassword = exampleAC.getParam(PASSWORD_KEY);
                JSONObject body = new JSONObject();
                body.put("password", newPassword);

                try {
                    JSONObject resetResult = bot.syncJSONRequest(
                            exampleAC.getProxy(),
                            "https://app-auth.jp.stork-oracle.network/user",
                            HttpMethod.PUT,
                            headers,
                            null,
                            body,
                            () -> simpleInfo + " send reset password put request"
                    ).get();
                    log.debug(simpleInfo + " reset password put request finish, " + resetResult);
                    exampleAC.setParam(PASSWORD_RESET_FINISH_KEY, true);
                    return Result.ok();
                } catch (InterruptedException | ExecutionException e) {
                    if (e.getMessage().contains("same_password")) {
                        exampleAC.setParam(PASSWORD_RESET_FINISH_KEY, true);
                    }
                    throw new RuntimeException("put password error", e);
                }
            }).get();
        } catch (InterruptedException | ExecutionException e) {
            return Result.fail(e.getMessage());
        }
    }

    /**
     * 刷新token
     *
     * @param accountContext accountContext
     */
    public void refreshToken(AccountContext accountContext) {
        String simpleInfo = accountContext.getSimpleInfo();
        bot.logger.debug(simpleInfo + " start refresh token");
        JSONObject params = new JSONObject();
        params.put("grant_type", "password");
        JSONObject body = new JSONObject();
        body.put("email", accountContext.getAccountBaseInfo().getEmail());
        body.put("password", accountContext.getParam(PASSWORD_KEY));
        Map<String, String> headers = accountContext.getBrowserEnv().generateHeaders();
        headers.put("origin", "https://app.stork.network");
        headers.put("referer", "https://app.stork.network/");
        headers.put("content-type", "application/json");
        try {
            JSONObject result = bot.syncJSONRequest(
                    accountContext.getProxy(),
                    REFRESH_TOKEN_URL,
                    HttpMethod.POST,
                    headers,
                    params,
                    body,
                    ()-> simpleInfo + " start send refresh token request"
                    ).get();
            String accessToken = result.getString("access_token");
            if (accessToken != null) {
                accountContext.setParam(MapConfigKey.ACCESS_TOKEN_KEY_V1, accessToken);
                bot.logger.info(simpleInfo + " refresh token finish, " + result);
            } else {
                bot.logger.error(simpleInfo + " refresh token fail, " + result);
            }
        } catch (InterruptedException | ExecutionException e) {
            bot.logger.error(simpleInfo + " refresh token error, " + e.getMessage());
        }
    }


    /**
     * keepAlive
     *
     * @param accountContext accountContext
     */
    public void keepAlive(AccountContext accountContext) {
        try {
            String token = accountContext.getParam(MapConfigKey.ACCESS_TOKEN_KEY_V1);

            if (token == null) {
                bot.logger.warn(accountContext.getSimpleInfo() + " token is null, skip it");
                return;
            }

            String msgHash = getSignedPrice(accountContext, token).get();

            String response = validateSignedPrice(accountContext, token, msgHash).get();

            bot.logger.info(accountContext.getSimpleInfo() + " keep alive success, " + response);
        } catch (InterruptedException | ExecutionException e) {
            bot.logger.error(accountContext.getSimpleInfo() + " keep alive error " + (e.getCause() == null ? e.getMessage() : e.getCause().getMessage()));
        }
    }


    private CompletableFuture<String> validateSignedPrice(AccountContext accountContext, String token, String msgHash) {
        bot.logger.debug(accountContext.getSimpleInfo() + " start validate signed price ");

        Map<String, String> headers = accountContext.getBrowserEnv().generateHeaders();
        headers.put("Authorization", "Bearer " + token);

        JSONObject body = new JSONObject();
        body.put("msg_hash", msgHash);
        body.put("valid", true);

        return bot.syncRequest(
                accountContext.getProxy(),
                VALIDATE_SIGNED_PRICE_API,
                HttpMethod.POST,
                headers,
                null,
                body
        );
    }


    private CompletableFuture<String> getSignedPrice(AccountContext accountContext, String token) {
        bot.logger.debug(accountContext.getSimpleInfo() + " start get signed price ");


        Map<String, String> headers = accountContext.getBrowserEnv().generateHeaders();
        headers.put("Authorization", "Bearer " + token);

        return bot.syncRequest(
                accountContext.getProxy(),
                STORK_SIGNED_PRICE_API,
                HttpMethod.GET,
                headers,
                null,
                null
        ).thenApplyAsync(responseStr -> {
            JSONObject signedPrices = JSONObject.parseObject(responseStr);
            bot.logger.debug(accountContext.getSimpleInfo() + " signed price get success");

            JSONObject prices = signedPrices.getJSONObject("data");
            for (String symbol : prices.keySet()) {
                JSONObject price = prices.getJSONObject(symbol);
                JSONObject timestampedSignature = price.getJSONObject("timestamped_signature");
                if (timestampedSignature != null) {
                    return timestampedSignature.getString("msg_hash");
                }
            }

            throw new RuntimeException("signed price is empty");
        });
    }

    /**
     * Step 2 从邮箱获取验证码
     *
     * @param exampleAC exampleAC
     * @return String
     */
    private @NotNull String queryCheckCode(AccountContext exampleAC) {
        bot.logger.info(exampleAC.getSimpleInfo() + " start query check code");

        String email = exampleAC.getAccountBaseInfo().getEmail();
        String imapPassword = (String) exampleAC.getAccountBaseInfo().getParams().get(IMAP_PASSWORD_KEY);

        AtomicReference<String> checkCode = new AtomicReference<>();
        mailReader.stoppableReadMessage(email, imapPassword, 3, message -> {
            try {
                String newValue = resolveVerifierCodeFromMessage(message);
                checkCode.set(newValue);
                return StrUtil.isNotBlank(newValue);
            } catch (MessagingException e) {
                throw new RuntimeException("email check code query error", e);
            }
        });

        if (StrUtil.isBlank(checkCode.get())) {
            throw new RuntimeException("check code is empty");
        }

        return checkCode.get();
    }

    private String resolveVerifierCodeFromMessage(Message message) throws MessagingException {
        boolean b = Arrays.stream(message.getFrom())
                .anyMatch(address -> address.toString().contains(MAIL_FROM));
        if (!b) return null;

        String context = MailReader.getTextFromMessage(message);
        Matcher matcher = V_CODE_PATTERN.matcher(context);

        if (matcher.find()) {
            return matcher.group();
        }
        return null;
    }
}
