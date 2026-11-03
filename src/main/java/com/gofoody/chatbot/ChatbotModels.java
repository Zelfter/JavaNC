package com.gofoody.chatbot;

import java.util.List;

/**
 * DTO cho API /api/chatbot/ask. Tách biệt khỏi model cũ (KHÔNG sửa model hiện có).
 */
public class ChatbotModels {

    /** Request body: { "question": "..." } */
    public static class ChatRequest {
        public String question;
    }

    /** Một sản phẩm được bot gợi ý (tham chiếu để frontend render link/thẻ). */
    public static class ProductRef {
        public long id;
        public String name;
        public String restaurant;
        public double price;
        public String description;
        public String image;
        public String link;     // "/restaurant/{restaurantId}" để xem nhà hàng
        public double score;    // độ liên quan (0..1)

        public static ProductRef of(com.gofoody.model.MenuItem m, double score) {
            ProductRef r = new ProductRef();
            r.id = m.id;
            r.name = m.name;
            r.restaurant = m.restaurantName;
            r.price = m.price;
            r.description = m.description;
            r.image = m.image;
            r.link = "/restaurant/" + m.restaurantId;
            r.score = Math.round(score * 1000.0) / 1000.0;
            return r;
        }
    }

    /** Response body. */
    public static class ChatResponse {
        public String answer;
        public boolean usedLlm;     // true nếu câu trả lời sinh bởi LLM
        public int retrieved;       // số sản phẩm được retrieve
        public List<ProductRef> products;
        public String error;        // null khi OK
    }
}
