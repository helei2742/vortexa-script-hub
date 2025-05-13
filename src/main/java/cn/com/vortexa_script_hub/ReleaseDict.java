package cn.com.vortexa_script_hub;


import java.io.File;
import java.util.List;

/**
 * @author helei
 * @since 2025-05-13
 */
public class ReleaseDict {
    private static final String BASE_PATH = System.getProperty("user.dir");

    public static final List<String> optim_ai = List.of("depin", "optim-ai");
    public static final List<String> stork = List.of("depin", "stork");
    public static final List<String> r2_money = List.of("testnet", "r2_money");
    public static final List<String> enos = List.of("testnet", "selenium", "enos");
    public static final List<String> haha_wallet = List.of("testnet", "selenium", "haha_wallet");
    public static final List<String> magic_newton = List.of("testnet", "selenium", "magic_newton");


    public static String buildLocalTargetPath(List<String> prefix) {
        return BASE_PATH + File.separator  + String.join(File.separator, prefix) + File.separator + "target";
    }
}
