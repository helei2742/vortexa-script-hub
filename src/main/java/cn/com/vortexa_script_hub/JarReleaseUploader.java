package cn.com.vortexa_script_hub;

import cn.com.vortexa.common.dto.IntroduceMD;
import cn.com.vortexa.common.util.IntroduceMDBuilder;
import cn.com.vortexa.common.util.YamlConfigLoadUtil;
import com.alibaba.fastjson.JSONObject;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.*;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.List;
import java.util.Properties;
import java.util.regex.*;

import static cn.com.vortexa.common.util.JarFileResolveUtil.BOT_META_INF_FILE_NAME;

public class JarReleaseUploader {

    private static final String OWNER = "helei2742";
    private static final String REPO = "vortexa-script-hub";
    private static final String TOKEN;

    private static final OkHttpClient client = new OkHttpClient();
    private static final ObjectMapper mapper = new ObjectMapper();
    private static final String baseDir = System.getProperty("user.dir");
    static {
        try (FileInputStream fis = new FileInputStream(baseDir + File.separator + "env.properties")) {
            Properties props = new Properties();
            props.load(fis);
            TOKEN = props.getProperty("github.token");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static void main(String[] args)  {
        List<List<String>> uploadJarDirs = List.of(
//          ReleaseDict.optim_ai
          ReleaseDict.stork,
          ReleaseDict.r2_money,
          ReleaseDict.enos,
          ReleaseDict.haha_wallet,
          ReleaseDict.magic_newton
        );
        for (List<String> contentPaths : uploadJarDirs) {
            try {
                String uploadJarDir = ReleaseDict.buildLocalTargetPath(contentPaths);
                System.out.println("start upload " + uploadJarDir);
                uploadJarRelease(contentPaths, new File(uploadJarDir));
            } catch (IOException e) {
                System.out.println(e.getMessage());
            }
        }
    }

    private static void uploadJarRelease(List<String> contentPaths, File jarDir) throws IOException {
        File[] files = jarDir.listFiles((dir, name) -> name.endsWith(".jar"));
        File iconFile = new File(jarDir.getAbsolutePath() + File.separator + "classes" + File.separator
                + contentPaths.getLast()
                .replace("-", "_") + File.separator + "icon.png");
        File introduceFile = new File(jarDir.getAbsolutePath() + File.separator + "classes" + File.separator
                + contentPaths.getLast()
                .replace("-", "_") + File.separator + BOT_META_INF_FILE_NAME);

        if (files == null) {
            System.out.println("no jar file, " + jarDir);
            return;
        }

        IntroduceMD introduceMD;
        if (introduceFile.exists()) {
            introduceMD = YamlConfigLoadUtil.load(introduceFile, List.of("vortexa","botMetaInfo", "introduce"), IntroduceMD.class);
        } else {
            introduceMD = new IntroduceMD();
        }

        for (File jar : files) {
            String fileName = jar.getName();
            String[] parsed = parseJarName(fileName);
            if (parsed == null) continue;

            String name = parsed[0];
            String version = parsed[1];
            String tag = name + "-" + version;

            introduceMD.setBotName(name);
            introduceMD.setBotVersion(version);

            System.out.println("处理：" + fileName + " -> tag: " + tag);
            String releaseId = getOrCreateRelease(tag, IntroduceMDBuilder.build(introduceMD));
            uploadAsset(releaseId, jar);
            if (iconFile.exists()) {
                uploadAsset(releaseId, iconFile);
            }
        }
    }

    private static String[] parseJarName(String fileName) {
        Pattern pattern = Pattern.compile("^(.+)-([\\d.]+)\\.jar$");
        Matcher matcher = pattern.matcher(fileName);
        if (matcher.matches()) {
            return new String[]{matcher.group(1), matcher.group(2)};
        }
        return null;
    }

    private static String getOrCreateRelease(String tag, String introduce) throws IOException {
        Request getRelease = new Request.Builder()
                .url("https://api.github.com/repos/" + OWNER + "/" + REPO + "/releases/tags/" + tag)
                .header("Authorization", "token " + TOKEN)
                .build();

        try (Response response = client.newCall(getRelease).execute()) {
            if (response.code() == 200) {
                JsonNode json = mapper.readTree(response.body().string());
                return json.get("id").asText();
            }
        }

        JSONObject body = new JSONObject();
        body.put("tag_name", tag);
        body.put("name", tag);
        body.put("body", introduce);
        body.put("draft", false);
        body.put("prerelease", false);

        // 创建 release
        String bodyJson = JSONObject.toJSONString(body);

        System.out.println(tag);
        Request create = new Request.Builder()
                .url("https://api.github.com/repos/" + OWNER + "/" + REPO + "/releases")
                .header("Authorization", "token " + TOKEN)
                .post(RequestBody.create(bodyJson, MediaType.parse("application/json")))
                .build();
        System.out.println(bodyJson);
        try (Response response = client.newCall(create).execute()) {
            if (!response.isSuccessful()) throw new RuntimeException("创建 Release 失败：" + response.body().string());
            JsonNode json = mapper.readTree(response.body().string());
            return json.get("id").asText();
        }
    }

    private static void uploadAsset(String releaseId, File file) throws IOException {
        String uploadUrl = "https://uploads.github.com/repos/" + OWNER + "/" + REPO +
                "/releases/" + releaseId + "/assets?name=" + file.getName();

        Request request = new Request.Builder()
                .url(uploadUrl)
                .header("Authorization", "token " + TOKEN)
                .header("Content-Type", "application/java-archive")
                .post(RequestBody.create(file, MediaType.parse("application/java-archive")))
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new RuntimeException("上传失败：" + response.body().string());
            }
            System.out.println("✅ 上传成功: " + file.getName());
        }
    }
}
