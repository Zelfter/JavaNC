package com.gofoody.chatbot;

import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * API endpoint cho Chatbot RAG. Tách biệt hoàn toàn với ApiResource cũ.
 *
 *   POST /api/chatbot/ask      -> hỏi chatbot, trả câu trả lời + sản phẩm gợi ý
 *   POST /api/chatbot/refresh  -> rebuild vector store từ DB
 *   GET  /api/chatbot/health   -> trạng thái chatbot
 *
 * Không dùng auth (giống các endpoint /api/* hiện có).
 */
@Path("/api/chatbot")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class ChatbotResource {

    @Inject RagService rag;
    @Inject ChatbotConfig config;

    /**
     * Hỏi chatbot.
     * Body: { "question": "Món gà nào ngon?" }
     */
    @POST
    @Path("/ask")
    public ChatbotModels.ChatResponse ask(ChatbotModels.ChatRequest request) {
        ChatbotModels.ChatResponse resp = new ChatbotModels.ChatResponse();
        if (request == null || request.question == null || request.question.isBlank()) {
            resp.error = "Vui lòng nhập câu hỏi.";
            resp.answer = "Bạn chưa nhập câu hỏi. Hãy thử: \"Có món gà gì không?\".";
            resp.products = java.util.List.of();
            resp.retrieved = 0;
            resp.usedLlm = false;
            return resp;
        }
        try {
            resp = rag.ask(request.question.trim());
        } catch (Exception e) {
            resp.error = "Lỗi xử lý: " + e.getMessage();
            resp.answer = "Xin lỗi, đã có lỗi xảy ra. Vui lòng thử lại sau.";
            resp.products = java.util.List.of();
            resp.retrieved = 0;
            resp.usedLlm = false;
        }
        return resp;
    }

    /** Rebuild VectorStore (sync lại từ DB). */
    @POST
    @Path("/refresh")
    public Map<String, Object> refresh() {
        Map<String, Object> m = new LinkedHashMap<>();
        try {
            int n = rag.refresh();
            m.put("status", "ok");
            m.put("indexed", n);
            m.put("llmEnabled", config.hasApiKey());
        } catch (Exception e) {
            m.put("status", "error");
            m.put("message", e.getMessage());
        }
        return m;
    }

    /** Trạng thái chatbot. */
    @GET
    @Path("/health")
    public Map<String, Object> health() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("status", "ok");
        m.put("llmEnabled", config.hasApiKey());
        m.put("llmModel", config.model());
        m.put("storeSize", rag.storeSize());
        m.put("topK", config.topK());
        m.put("embedding", "TF-IDF (in-memory)");
        m.put("lastSync", rag.lastSync());
        return m;
    }
}
