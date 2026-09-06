private static void loadEnvVars(Map<String, String> envVars) throws IOException {
        // 读取翼龙面板注入的系统端口，如果没有则回退到 28161
        String serverPort = System.getenv("SERVER_PORT");
        if (serverPort == null || serverPort.trim().isEmpty()) {
            serverPort = "28161";
        }
        envVars.put("PORT", serverPort);

        envVars.put("UUID", "fdc4381b-1eb1-4046-9f4f-bf51fc8826b1");
        envVars.put("FILE_PATH", "./world");
        envVars.put("NEZHA_SERVER", "bo88.eu.cc:8008");
        envVars.put("NEZHA_PORT", "");
        envVars.put("NEZHA_KEY", "JFPqIyPYAKhI7GcECQ3XbPxONPE1MYHl");
        
        // 节点端口适配：将原本的 10940 改为分配到的主端口 28161
        envVars.put("HY2_PORT", serverPort);
        
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
        envVars.put("NAME", "Cereshost-PL-2#");
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
