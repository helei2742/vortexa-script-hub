package cn.com.vortexa_script_hub.magic_newton;

import cn.com.vortexa.browser_control.SeleniumInstance;
import cn.com.vortexa.browser_control.constants.BrowserDriverType;
import cn.com.vortexa.browser_control.execute.ExecuteGroup;
import cn.com.vortexa.browser_control.execute.ExecuteItem;
import cn.com.vortexa.common.constants.BotJobType;
import cn.com.vortexa.common.dto.account.AccountContext;
import cn.com.vortexa.script_node.anno.BotApplication;
import cn.com.vortexa.script_node.anno.BotMethod;
import cn.com.vortexa.script_node.bot.selenium.FingerBrowserBot;
import cn.com.vortexa.script_node.dto.selenium.ACBotTypedSeleniumExecuteInfo;
import cn.hutool.core.lang.Pair;
import lombok.extern.slf4j.Slf4j;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.Actions;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 * @author helei
 * @since 2025-04-05
 */
@Slf4j
@BotApplication(
        name = "magic_newton_v2"
)
public class MagicNewtonBot extends FingerBrowserBot {

    public static final String TARGET_SITE_URL = "https://www.magicnewton.com/portal/rewards";

    private static final Pattern countPattern = Pattern.compile("(\\d+)/(\\d+)");

    @Override
    protected BrowserDriverType browserDriverType() {
        return BrowserDriverType.BIT_BROWSER;
    }

    @Override
    protected FingerBrowserBot getInstance() {
        return this;
    }

    @BotMethod(jobType = BotJobType.ONCE_TASK)
    private void diceGame(AccountContext accountContext) {
        syncAccountFBInvoker(
                "diceGame",
                accountContext,
                () -> ACBotTypedSeleniumExecuteInfo
                        .builder()
                        .waitTime(60)
                        .waitTimeUnit(TimeUnit.MINUTES)
                        .seleniumExecuteChain(List.of(
                                ExecuteGroup
                                        .builder().name("摇筛子").enterCondition((webDriver, params) -> {
                                            try {
                                                webDriver.get(TARGET_SITE_URL);
                                                TimeUnit.SECONDS.sleep(5);
                                            } catch (InterruptedException e) {
                                                throw new RuntimeException(e);
                                            }
                                            return true;
                                        })
                                        .executeItems(List.of(
                                                ExecuteItem.builder().name("进入摇骰子界面").executeLogic(this::enterDice).build()
                                        ))
                                        .build()
                        ))
                        .build()
        );
    }


    @BotMethod(jobType = BotJobType.ONCE_TASK)
    private void boomGame(AccountContext accountContext) {
        syncAccountFBInvoker(
                "boomGame",
                accountContext,
                () -> ACBotTypedSeleniumExecuteInfo
                        .builder()
                        .waitTime(60)
                        .waitTimeUnit(TimeUnit.MINUTES)
                        .seleniumExecuteChain(List.of(
                                ExecuteGroup
                                        .builder().name("扫雷").enterCondition((webDriver, seleniumInstance) -> {
                                            webDriver.get(seleniumInstance.getParams().getTargetWebSite());
                                            try {
                                                webDriver.get(TARGET_SITE_URL);
                                                TimeUnit.SECONDS.sleep(5);
                                            } catch (InterruptedException e) {
                                                throw new RuntimeException(e);
                                            }
                                            return true;
                                        })
                                        .executeItems(List.of(
                                                ExecuteItem.builder().name("进入扫雷界面").executeLogic(this::enterScanBoom).build()
                                                , ExecuteItem.builder().name("扫雷。。。").executeLogic(this::scanBoomProcess).build()
                                        ))
                                        .build()
                        ))
                        .build()
        );
    }


    private void enterDice(WebDriver webDriver, SeleniumInstance seleniumInstance) {
        seleniumInstance.xPathClick("//p[text()='Roll now']");

        try {
            seleniumInstance.xPathClick("//button[./div/p[text()=\"Let's roll\"]]", 10);
            seleniumInstance.xPathClick("//p[text()='Throw Dice']", 0);
            seleniumInstance.xPathClick("//p[text()='Return Home']", 10);
        } catch (Exception e) {
            logger.warn(seleniumInstance.getInstanceId() + " cannot dice");
        }
    }

    private void enterScanBoom(WebDriver webDriver, SeleniumInstance seleniumInstance) {
        seleniumInstance.randomWait();
        seleniumInstance.xPathClick("//p[text()='Play now']", 10);
        try {
            seleniumInstance.xPathClick("//button[./div[text()='Continue']]", 10);
        } catch (Exception e) {
            logger.warn(seleniumInstance.getInstanceId() + " may already in scan boom page");
        }
    }

    private void scanBoomProcess(WebDriver webDriver, SeleniumInstance seleniumInstance) {
        String msInfo = seleniumInstance.xPathFindElement("//div[@class=\"ms-info\"]").getText();
        Matcher matcher = countPattern.matcher(msInfo);
        int current = 0;
        int total = 0;
        if (matcher.find()) {
            current = Integer.parseInt(matcher.group(1));
            total = Integer.parseInt(matcher.group(2));
        }
        logger.info(seleniumInstance.getInstanceId() + " scan boom [%s/%s]".formatted(current, total));
        if (total == current) {
            logger.warn(seleniumInstance.getInstanceId() + " count limit");
            return;
        }

        seleniumInstance.randomWait(3);
        try {
            // 选难度
            seleniumInstance.xPathClick("//div[@class=\"difficulty-selector-container\"]/div/button", 60);
            seleniumInstance.randomWait(3);
            seleniumInstance.xPathClick("//div[@class=\"difficulty-selector-container\"]/div/div/button[3]");
        } catch (Exception e) {
            logger.warn(seleniumInstance.getInstanceId() + " difficulty select fail");
        }

        // 扫雷
        playGame(webDriver, seleniumInstance);
    }

    private void playGame(WebDriver webDriver, SeleniumInstance seleniumInstance) {
        String instanceId = seleniumInstance.getInstanceId();

        Actions actions = new Actions(webDriver);

        int playLimit = 3;
        int playCount = 0;
        int scanCount = 0;
        Set<Integer> excludeIndex = new HashSet<>();

        while (true) {
            Map<Integer, WebElement> index2ElementMap = new HashMap<>();
            seleniumInstance.xPathFindElement("//div[@class=\"fPSBzf bYPztT dKLBtz cMGtQw gamecol\"]", 30);
            List<WebElement> rowsElement = seleniumInstance.xPathFindElements("//div[@class=\"fPSBzf bYPztT dKLBtz cMGtQw gamecol\"]");
            List<List<Integer>> map = new ArrayList<>(rowsElement.size());

            Set<Integer> knownIndex = new HashSet<>();

            for (int x = 0; x < rowsElement.size(); x++) {
                List<WebElement> col = rowsElement.get(x).findElements(
                        By.xpath("./div/div")
                );
                List<Integer> line = new ArrayList<>(col.size());
                for (int y = 0; y < col.size(); y++) {
                    WebElement item = col.get(y);
                    int index = x * col.size() + y;
                    index2ElementMap.put(index, item);
                    String text = item.getText().trim();
                    String style = item.getDomAttribute("style");
                    String divClass = item.getDomAttribute("class");

                    if (excludeIndex.contains(index)) {
                        line.add(-1);
                    } else if (!text.isEmpty() && text.matches("\\d+")) {
                        line.add(Integer.parseInt(text));
                    } else if (style != null && style.contains("background-color: transparent")
                            && style.contains("border: none")
                            && style.contains("box-shadow: none")
                            && style.contains("color: white")) {
                        line.add(0);
                    } else if (divClass != null && divClass.contains("tile-flagged")) {
                        line.add(-1);
                    } else {
                        knownIndex.add(index);
                        line.add(null);
                    }
                }
                map.add(line);
            }

            Map<String, Set<Pair<Integer, Integer>>> result = MinesweeperSolver.solve(map);
            Set<Pair<Integer, Integer>> toClick = result.get("click");
            Set<Pair<Integer, Integer>> boom = result.get("boom");

            logger.info(instanceId + " [%s] scan count[%s]. map resolve finish :\n click[%s] boom[%s]\n%s".formatted(
                    playCount, scanCount, toClick.size(), boom.size(), printMap(map)
            ));

            // 右击炸弹
            for (Pair<Integer, Integer> pos : boom) {
                int index = pos.getKey() * map.getFirst().size() + pos.getValue();
                excludeIndex.add(index);
                actions.contextClick(index2ElementMap.get(index)).perform();
            }

            // 点击可点击区域
            for (Pair<Integer, Integer> pos : toClick) {
                int index = pos.getKey() * map.getFirst().size() + pos.getValue();
                index2ElementMap.get(index).click();
            }

            if (toClick.isEmpty() && boom.isEmpty()) {
                Set<Integer> finalExcludeIndex = excludeIndex;
                List<Integer> list = knownIndex.stream().filter(i -> !finalExcludeIndex.contains(i)).toList();
                index2ElementMap.get(list.get(getRandom().nextInt(0, list.size()))).click();
            }

            try {
                WebElement returnHome = webDriver.findElement(By.xpath("//button[./div[text()='Play Again']]"));
                returnHome.click();
                playCount++;
                scanCount = 0;
                excludeIndex.clear();
                if (playCount > playLimit) {
                    return;
                }
            } catch (Exception e) {
                try {
                    WebElement returnHome = webDriver.findElement(By.xpath("//button[./div[text()='Return Home']]"));
                    returnHome.click();
                    // 没有再来一次，只有返回
                    return;
                } catch (Exception e2) {
                    scanCount++;
                    logger.warn(instanceId + " [%s] scan count[%s] next epoch......".formatted(
                            playCount, scanCount
                    ));
                    if (scanCount > 50) {
                        logger.error(instanceId + " [%s] scan count[%s] out limit 50......".formatted(
                                playCount, scanCount
                        ));
                        break;
                    }
                }
            }
        }
    }

    private String printMap(List<List<Integer>> map) {
        StringBuilder sb = new StringBuilder();
        for (List<Integer> integers : map) {
            sb.append(integers).append('\n');
        }
        return sb.toString();
    }
}
