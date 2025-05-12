package cn.com.vortexa_script_hub.testnet.selenium.enos;


import cn.com.vortexa.browser_control.SeleniumInstance;
import cn.com.vortexa.browser_control.constants.BrowserDriverType;
import cn.com.vortexa.browser_control.execute.ExecuteGroup;
import cn.com.vortexa.browser_control.execute.ExecuteItem;
import cn.com.vortexa.common.constants.BotJobType;
import cn.com.vortexa.common.dto.account.AccountContext;
import cn.com.vortexa.script_node.anno.BotApplication;
import cn.com.vortexa.script_node.anno.BotMethod;
import cn.com.vortexa.script_node.bot.selenium.AccountFingerBrowserSelenium;
import cn.com.vortexa.script_node.bot.selenium.FingerBrowserBot;
import cn.com.vortexa.script_node.dto.selenium.ACBotTypedSeleniumExecuteInfo;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * @author helei
 * @since 2025-05-07
 */
@Slf4j
@BotApplication(
        name = "enos_bot"
)
public class EnosBot extends FingerBrowserBot {
    private static final String MAIN_PAGE_URL = "https://speedrun.enso.build/";

    @Override
    protected BrowserDriverType browserDriverType() {
        return BrowserDriverType.BIT_BROWSER;
    }

    @Override
    protected FingerBrowserBot getInstance() {
        return this;
    }

    @BotMethod(jobType = BotJobType.ONCE_TASK)
    public void dailyJob(AccountContext accountContext) {
        syncAccountFBInvoker(
                "daily_job",
                accountContext,
                () -> ACBotTypedSeleniumExecuteInfo
                        .builder()
                        .waitTime(60)
                        .waitTimeUnit(TimeUnit.MINUTES)
                        .seleniumExecuteChain(List.of(
                                ExecuteGroup
                                        .builder().name("每日DEFI").enterCondition((webDriver, params) -> {
                                            try {
                                                webDriver.get(MAIN_PAGE_URL);
                                                TimeUnit.SECONDS.sleep(5);
                                            } catch (InterruptedException e) {
                                                throw new RuntimeException(e);
                                            }
                                            return true;
                                        })
                                        .executeItems(List.of(
                                                ExecuteItem.builder().name("创建DEFI").executeLogic(this::createDailyDefi).build()
                                        ))
                                        .build(),
                                ExecuteGroup
                                        .builder().name("app点击").enterCondition((webDriver, params) -> {
                                            try {
                                                webDriver.get(MAIN_PAGE_URL);
                                                TimeUnit.SECONDS.sleep(5);
                                            } catch (InterruptedException e) {
                                                throw new RuntimeException(e);
                                            }
                                            return true;
                                        })
                                        .executeItems(List.of(
                                                ExecuteItem.builder().name("点击app").executeLogic(this::clickApp).build()
                                        ))
                                        .build()
                        ))
                        .build()
        );
    }


    private void clickApp(WebDriver webDriver, SeleniumInstance seleniumInstance) {
        String mainHandle = webDriver.getWindowHandle();
        seleniumInstance.xPathClick("//*[@id=\"header\"]/a[2]", 30);
        List<WebElement> pageIdxs = seleniumInstance.xPathFindElements("/html/body/div[1]/main/div/div/div[5]/ul/li");
        for (WebElement pageIdx : pageIdxs) {
            String title = pageIdx.getDomAttribute("title");
            if (StrUtil.isEmpty(title)) {
                continue;
            }
            try {
                int i = Integer.parseInt(title);
                logger.info(getInstance() + " start click page " + i);
                seleniumInstance.scrollTo(pageIdx);
                seleniumInstance.randomWait(3);
                pageIdx.click();

                seleniumInstance.randomWait(8);
                List<WebElement> webElements = seleniumInstance.xPathFindElements("/html/body/div[1]/main/div/div/div[4]/a[count(button/div/div) = 2]");

                for (WebElement webElement : webElements) {
                    Set<String> before = webDriver.getWindowHandles();

                    seleniumInstance.scrollTo(webElement);
                    seleniumInstance.randomWait(2);
                    webElement.click();
                    seleniumInstance.randomWait(5);

                    Set<String> after = webDriver.getWindowHandles();
                    after.removeAll(before);
                    for (String handle : after) {
                        webDriver.switchTo().window(handle);
                        webDriver.close();
                    }
                    webDriver.switchTo().window(mainHandle);

                    seleniumInstance.randomWait(5);
                }
            } catch (Exception e) {
                log.debug(e.getMessage());
            }
        }

    }

    private void createDailyDefi(WebDriver webDriver, SeleniumInstance seleniumInstance) {
        seleniumInstance.xPathClick("//*[@id=\"video-container\"]/div[2]/div/a[1]", 60);

        WebElement webElement = seleniumInstance.xPathFindElement("/html/body/div[1]/main/div/div/div[4]/button/div/div/span[2]", 30);
        seleniumInstance.randomWait(4);
        int count = 5;
        if (StrUtil.isNotBlank(webElement.getText())) {
            String str = webElement.getText();
            if (webElement.getText().contains("Resets")) {
                count = 0;
                logger.info(seleniumInstance.getInstanceId() + " today create finish");
            } else {
                str = str.replace("(", "").replace(")", "");
                count = Integer.parseInt(str.split(" ")[0]);
            }
        }
        if (count <= 0) {
            return;
        }
        logger.info(seleniumInstance.getInstanceId() + " today remaining " + count);
        AccountFingerBrowserSelenium afbs = (AccountFingerBrowserSelenium) seleniumInstance;
        AccountContext accountContext = afbs.getAccountContext();

        for (int i = 0; i < count; i++) {
            seleniumInstance.xPathClick("/html/body/div[1]/main/div/div/div[4]/button", 15);
            WebElement nameInput = seleniumInstance.xPathFindElement("//*[@id=\"name\"]");
            String name = randomStr();
            nameInput.sendKeys(name);
            seleniumInstance.randomWait(2);
            WebElement subdomainInput = seleniumInstance.xPathFindElement("//*[@id=\"subdomain\"]");
            String subdomain = randomStr();
            subdomainInput.sendKeys(subdomain);
            seleniumInstance.randomWait(2);
            String username = accountContext.getTwitter().getUsername();
            WebElement twitterInput = seleniumInstance.xPathFindElement("//*[@id=\"twitter\"]");
            twitterInput.sendKeys(username);
            seleniumInstance.randomWait(2);
            seleniumInstance.xPathClick("/html/body/div[1]/main/div/div/form/button[1]", 30);

            logger.info(seleniumInstance.getInstanceId() + " create defi finish idx[%s] name[%s] subdomain[%s]".formatted(
                    i + 1, name, subdomain
            ));
            seleniumInstance.randomWait(3);
            seleniumInstance.xPathClick("/html/body/div[1]/main/div/div/div[2]/button", 30);
        }
    }

    private static @NotNull String randomStr() {
        return RandomUtil.randomString("1234567890qwertyuiopasdfghjklzxcvbnm", 20);
    }
}
