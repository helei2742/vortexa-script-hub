package cn.com.vortexa_script_hub;


import java.io.File;
import java.util.List;

/**
 * @author helei
 * @since 2025-05-13
 */
public class ReleaseDict {
    private static final String BASE_PATH = System.getProperty("user.dir");

    public static final String optim_ai = buildLocalTargetPath(List.of("depin", "optim-ai"));
    public static final String stork = buildLocalTargetPath(List.of("depin", "stork"));
    public static final String r2_money = buildLocalTargetPath(List.of("testnet", "r2_money"));
    public static final String enos = buildLocalTargetPath(List.of("testnet", "selenium", "enos"));
    public static final String haha_wallet = buildLocalTargetPath(List.of("testnet", "selenium", "haha_wallet"));
    public static final String magic_newton = buildLocalTargetPath(List.of("testnet", "selenium", "magic_newton"));


    public static String buildLocalTargetPath(List<String> prefix) {
        return BASE_PATH + File.separator  + String.join(File.separator, prefix) + File.separator + "target";
    }
}
