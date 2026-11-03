package com.gofoody.chatbot;

import com.gofoody.model.MenuItem;
import com.gofoody.service.Db;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * RAG Service — lõi chatbot.
 *
 * Quy trình:
 *  1. Ingestion  : đọc toàn bộ MenuItem từ DB (dùng Db.menu đã có), build VectorStore.
 *  2. Retrieval  : embed câu hỏi -> cosine similarity -> top-K sản phẩm.
 *  3. Augment    : ghép context sản phẩm + câu hỏi thành prompt.
 *  4. Generate   : gọi LLM API (OpenAI-compatible) nếu có API key;
 *                  fallback = trả context sản phẩm trực tiếp nếu chưa có key.
 *
 * KHÔNG sửa bất kỳ file nào hiện có. KHÔNG thêm dependency vào pom.xml
 * (gọi HTTP API qua java.net.HttpURLConnection thuần).
 */
@ApplicationScoped
public class RagService {

    private static final Logger LOG = Logger.getLogger(RagService.class.getName());

    @Inject Db db;
    @Inject ChatbotConfig config;

    private final VectorStore store = new VectorStore();
    private volatile long lastSync = 0;

    @PostConstruct
    public void init() {
        try {
            refresh();
        } catch (Exception e) {
            LOG.log(Level.WARNING, "Không thể sync VectorStore lúc khởi động. Sẽ thử lại khi có request đầu tiên.", e);
        }
    }

    /**
     * Đọc lại toàn bộ menu từ DB và rebuild VectorStore.
     * Có thể gọi thủ công qua POST /api/chatbot/refresh.
     */
    public synchronized int refresh() {
        List<MenuItem> items = db.menu(0, null);
        store.build(items);
        lastSync = System.currentTimeMillis();
        LOG.info("[RAG] VectorStore đã sync " + items.size() + " món ăn.");
        return items.size();
    }

    /**
     * Trả lời câu hỏi theo quy trình RAG đầy đủ.
     *
     * @param question câu hỏi của người dùng
     * @return ChatResponse (không bao giờ null)
     */
    public ChatbotModels.ChatResponse ask(String question) {
        ChatbotModels.ChatResponse resp = new ChatbotModels.ChatResponse();

        // Nếu VectorStore chưa có dữ liệu -> thử lại (lazy sync)
        if (store.isEmpty()) {
            try { refresh(); } catch (Exception e) { /* ignore */ }
        }

        // ── 1. RETRIEVE ──────────────────────────────────────────────────
        List<VectorStore.ScoredDoc> hits = store.retrieve(question, config.topK(), config.minScore());
        resp.retrieved = hits.size();
        resp.products = hits.stream()
                .map(h -> ChatbotModels.ProductRef.of(h.item, h.score))
                .collect(Collectors.toList());

        // ── 2. BUILD PROMPT / CONTEXT ────────────────────────────────────
        if (hits.isEmpty()) {
            // Không tìm thấy sản phẩm liên quan
            if (!config.hasApiKey()) {
                resp.answer = buildNotFoundAnswer(question);
                resp.usedLlm = false;
                return resp;
            }
        }

        String context = buildContext(hits);

        // ── 3. GENERATE ─────────────────────────────────────────────────
        if (config.hasApiKey()) {
            try {
                resp.answer = callLlm(question, context);
                resp.usedLlm = true;
            } catch (Exception e) {
                LOG.log(Level.WARNING, "LLM API lỗi, fallback về context-only.", e);
                resp.answer = buildFallbackAnswer(hits, question);
                resp.usedLlm = false;
                resp.error = "LLM unavailable, showing retrieved results only.";
            }
        } else {
            // Chưa cấu hình API key -> trả kết quả retrieve trực tiếp
            resp.answer = buildFallbackAnswer(hits, question);
            resp.usedLlm = false;
        }
        return resp;
    }

    public long lastSync() { return lastSync; }
    public int storeSize() { return store.size(); }

    // ─── Private Helpers ─────────────────────────────────────────────────

    /** Xây context block từ danh sách sản phẩm đã retrieve. */
    private String buildContext(List<VectorStore.ScoredDoc> hits) {
        if (hits.isEmpty()) return "Không có sản phẩm liên quan.";
        StringBuilder sb = new StringBuilder("Dưới đây là các món ăn có thể liên quan:\n");
        int idx = 1;
        for (VectorStore.ScoredDoc h : hits) {
            MenuItem m = h.item;
            sb.append(idx++).append(". **").append(m.name).append("**");
            if (m.restaurantName != null) sb.append(" (").append(m.restaurantName).append(')');
            sb.append(" — ₹").append(m.price);
            if (m.description != null && !m.description.isBlank())
                sb.append(". Mô tả: ").append(m.description);
            sb.append('\n');
        }
        return sb.toString();
    }

    /** Câu trả lời fallback khi không có API key. */
    private String buildFallbackAnswer(List<VectorStore.ScoredDoc> hits, String question) {
        if (hits.isEmpty()) return buildNotFoundAnswer(question);
        StringBuilder sb = new StringBuilder("Tôi tìm thấy một số món ăn có thể phù hợp với bạn:\n\n");
        for (VectorStore.ScoredDoc h : hits) {
            MenuItem m = h.item;
            sb.append("• **").append(m.name).append("**");
            if (m.restaurantName != null) sb.append(" tại ").append(m.restaurantName);
            sb.append(" — giá ₹").append(m.price);
            if (m.description != null && !m.description.isBlank()) sb.append(" — ").append(m.description);
            sb.append('\n');
        }
        sb.append("\nBạn có muốn thêm món nào vào giỏ hàng không?");
        return sb.toString();
    }

    private String buildNotFoundAnswer(String question) {
        return "Xin lỗi, tôi không tìm thấy món ăn nào phù hợp với yêu cầu của bạn. "
             + "Bạn có thể thử tìm kiếm trực tiếp trên trang chủ hoặc duyệt theo khu vực (TLY, KANNUR, CALICUT).";
    }

    /**
     * Gọi LLM API theo chuẩn OpenAI Chat Completions.
     * Base URL có thể là OpenAI, Azure OpenAI, hoặc bất kỳ provider tương thích.
     * Dùng java.net.HttpURLConnection thuần — KHÔNG cần dependency ngoài.
     */
    private String callLlm(String question, String context) throws Exception {
        String systemPrompt = """
                Bạn là trợ lý tư vấn món ăn cho ứng dụng Go Foody.
                Hãy trả lời ngắn gọn, thân thiện bằng tiếng Việt (hoặc tiếng Anh nếu người dùng hỏi bằng tiếng Anh).
                Chỉ tư vấn dựa trên thông tin sản phẩm được cung cấp. Không bịa đặt.
                Nếu không chắc, hãy hướng người dùng duyệt menu trực tiếp.
                """.stripIndent().trim();

        String userMessage = context + "\n\nCâu hỏi của khách: " + question;

        // Xây JSON request thủ công (tránh phụ thuộc thư viện JSON)
        String json = jsonChatRequest(config.model(), systemPrompt, userMessage);

        String endpoint = config.baseUrl().replaceAll("/$", "") + "/chat/completions";
        URL url = URI.create(endpoint).toURL();
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setConnectTimeout(5000);
        conn.setReadTimeout((int)(config.timeoutSeconds() * 1000));
        conn.setDoOutput(true);
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json; charset=utf-8");
        conn.setRequestProperty("Authorization", "Bearer " + config.apiKey());
        conn.setRequestProperty("User-Agent", "GoFoody-RAG/1.0");

        try (OutputStream os = conn.getOutputStream()) {
            os.write(json.getBytes(StandardCharsets.UTF_8));
        }

        int status = conn.getResponseCode();
        InputStream is = (status >= 200 && status < 300) ? conn.getInputStream() : conn.getErrorStream();
        String body = new String(is.readAllBytes(), StandardCharsets.UTF_8);

        if (status < 200 || status >= 300) {
            throw new RuntimeException("LLM API error " + status + ": " + body);
        }

        // Parse: "content" field từ JSON response OpenAI-compatible
        return parseContent(body);
    }

    /** Xây JSON cho Chat Completions (tự viết để không cần thư viện JSON). */
    private static String jsonChatRequest(String model, String system, String user) {
        return "{\"model\":\"" + escJson(model) + "\","
             + "\"max_tokens\":512,"
             + "\"temperature\":0.7,"
             + "\"messages\":["
             +   "{\"role\":\"system\",\"content\":\"" + escJson(system) + "\"},"
             +   "{\"role\":\"user\",\"content\":\"" + escJson(user) + "\"}"
             + "]}";
    }

    /** Escape chuỗi cho JSON. */
    private static String escJson(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }

    /**
     * Parse trường "content" từ JSON OpenAI Chat Completions response.
     * Dùng regex đơn giản (tránh dependency Jackson).
     *
     * Response shape:
     * {"choices":[{"message":{"role":"assistant","content":"..."}, ...}]}
     */
    private static String parseContent(String json) {
        // Tìm "content":"..." gần nhất trong choices[0].message
        int idx = json.indexOf("\"content\"");
        if (idx < 0) return "(Bot không trả về nội dung — kiểm tra LLM API response)";
        int colon = json.indexOf(':', idx);
        if (colon < 0) return json;
        int start = json.indexOf('"', colon + 1);
        if (start < 0) return json;
        // tìm dấu " đóng (tránh escaped)
        int pos = start + 1;
        StringBuilder sb = new StringBuilder();
        while (pos < json.length()) {
            char ch = json.charAt(pos);
            if (ch == '"') break;
            if (ch == '\\' && pos + 1 < json.length()) {
                char next = json.charAt(pos + 1);
                switch (next) {
                    case '"' -> sb.append('"');
                    case '\\' -> sb.append('\\');
                    case 'n' -> sb.append('\n');
                    case 'r' -> sb.append('\r');
                    case 't' -> sb.append('\t');
                    default -> sb.append(next);
                }
                pos += 2;
                continue;
            }
            sb.append(ch);
            pos++;
        }
        return sb.toString().isBlank() ? "(Bot trả về câu trả lời trống)" : sb.toString();
    }
}
