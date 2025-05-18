package cn.com.vortexa_script_hub;


import cn.com.vortexa.common.dto.BotMetaInfo;
import cn.com.vortexa.common.dto.RemoteBotAsserts;
import cn.com.vortexa.common.util.VersionUtil;
import cn.com.vortexa.common.util.YamlConfigLoadUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson.JSONObject;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import static cn.com.vortexa.common.util.JarFileResolveUtil.BOT_META_INFO_PREFIX;

/**
 * @author helei
 * @since 2025-05-12
 */
public class IndexJsonBuilder {
    private static final List<String> scanDirNames = List.of(
            "depin", "testnet"
    );
    private static final String baseDir = System.getProperty("user.dir");

    public static final String metaInfoFIleName = "bot-meta-info.yaml";

    private static final String reposUrl = "https://github.com/helei2742/vortexa-script-hub";

    public static void main(String[] args) {
        List<RemoteBotAsserts.Asserts> list = new ArrayList<>();
        for (String scanDirName : scanDirNames) {
            Path botInstanceConfigPath = Paths.get(baseDir, scanDirName);
            try (Stream<Path> walk = Files.walk(botInstanceConfigPath, 20)) {
                walk.filter(p -> Files.isRegularFile(p)
                        && p.toString().endsWith(metaInfoFIleName)
                        && !p.toString().contains(File.separator + "target" + File.separator)
                ).forEach(configFile -> {
                    try {
                        BotMetaInfo metaInfo = YamlConfigLoadUtil.load(configFile.toFile(),
                                BOT_META_INFO_PREFIX, BotMetaInfo.class);

                        String versionCode = metaInfo.getVersionCode();
                        String botName = metaInfo.getBotName();
                        if (StrUtil.isBlank(versionCode)) {
                            throw new RuntimeException(configFile + " versionCode is empty");
                        }
                        if (StrUtil.isBlank(botName)) {
                            throw new RuntimeException(botName + " botName is empty");
                        }

                        String jarName = VersionUtil.getBotJarFileName(botName, versionCode);
                        String jarGitHubDownLoadPath = reposUrl +
                                "/releases/download/" + botName + "-" + versionCode + "/" + jarName;
                        String iconDownloadPath = reposUrl +
                                "/releases/download/" + botName + "-" + versionCode + "/icon.png";
                                RemoteBotAsserts.Asserts asserts = new RemoteBotAsserts.Asserts();
                        asserts.setBotName(botName);
                        asserts.setVersion(versionCode);
                        asserts.setDownloadUrl(jarGitHubDownLoadPath);
                        asserts.setIconUrl(iconDownloadPath);
                        System.out.printf("[%s]-[%s] add into asserts%n", botName, versionCode);
                        list.add(asserts);
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                });
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        RemoteBotAsserts remoteBotAsserts = new RemoteBotAsserts();
        remoteBotAsserts.setAssertsList(list);

        try (BufferedWriter bw = new BufferedWriter(new FileWriter(
                baseDir + File.separator + "index.json"
        ))){
            bw.write(JSONObject.toJSONString(remoteBotAsserts));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
