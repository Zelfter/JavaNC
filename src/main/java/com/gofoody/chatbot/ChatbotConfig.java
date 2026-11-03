package com.gofoody.chatbot;

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;

import java.io.InputStream;
import java.util.Properties;

/**
 * Cấu hình cho Chatbot RAG.
 *
 * Đọc từ file mới "chatbot.properties" (classpath) + override bằng biến môi trường.
 * KHÔNG sửa "application.properties" hiện có của dự án.
 *
 *  ==>  ĐIỀN LLM API KEY CỦA BẠN VÀO FILE: src/main/resources/chatbot.properties
 *       (hoặc đặt biến môi trường CHATBOT_LLM_API_KEY)
 *       Nếu bỏ trống, chatbot vẫn hoạt động ở chế độ trả lời từ context (không gọi LLM).
 */
@ApplicationScoped
public class ChatbotConfig {

    private String apiKey = "";
    private String baseUrl = "https://api.openai.com/v1";
    private String model = "gpt-4o-mini";
    private int topK = 4;            // số sản phẩm retrieve (yêu cầu 3-5 => mặc định 4)
    private double minScore = 0.01;  // ngưỡng cosine tối thiểu để xét là liên quan
    private long timeoutSeconds = 30;

    @PostConstruct
    void init() {
        Properties p = new Properties();
        try (InputStream in = Thread.currentThread().getContextClassLoader()
                .getResourceAsStream("chatbot.properties")) {
            if (in != null) p.load(in);
        } catch (Exception ignored) {
            // không có file thì dùng mặc định
        }
        apiKey       = envOr(p, "chatbot.llm.api.key",          apiKey);
        baseUrl      = envOr(p, "chatbot.llm.base.url",         baseUrl);
        model        = envOr(p, "chatbot.llm.model",            model);
        topK         = parseInt(envOr(p, "chatbot.rag.topk",    String.valueOf(topK)), topK);
        minScore     = parseDouble(envOr(p, "chatbot.rag.min.score", String.valueOf(minScore)), minScore);
        timeoutSeconds = parseLong(envOr(p, "chatbot.llm.timeout.seconds", String.valueOf(timeoutSeconds)), timeoutSeconds);
    }

    /** Quy tắc env var: chatbot.llm.api.key -> CHATBOT_LLM_API_KEY */
    private String envOr(Properties p, String key, String def) {
        String envKey = "CHATBOT_" + key.replace("chatbot.", "").replace(".", "_").toUpperCase();
        String v = System.getenv(envKey);
        if (v != null && !v.isBlank()) return v.trim();
        v = p.getProperty(key);
        return (v != null && !v.isBlank()) ? v.trim() : def;
    }

    public boolean hasApiKey() { return apiKey != null && !apiKey.isBlank(); }
    public String apiKey() { return apiKey; }
    public String baseUrl() { return baseUrl; }
    public String model() { return model; }
    public int topK() { return topK; }
    public double minScore() { return minScore; }
    public long timeoutSeconds() { return timeoutSeconds; }

    private static int parseInt(String s, int d) { try { return Integer.parseInt(s.trim()); } catch (Exception e) { return d; } }
    private static long parseLong(String s, long d) { try { return Long.parseLong(s.trim()); } catch (Exception e) { return d; } }
    private static double parseDouble(String s, double d) { try { return Double.parseDouble(s.trim()); } catch (Exception e) { return d; } }
}
