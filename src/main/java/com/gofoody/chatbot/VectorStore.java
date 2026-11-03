package com.gofoody.chatbot;

import com.gofoody.model.MenuItem;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * In-memory Vector Store cho RAG.
 *
 * - Embedding: TF-IDF (Term Frequency - Inverse Document Frequency).
 * - Similarity: Cosine similarity giữa vector câu hỏi và vector sản phẩm.
 * - Hoàn toàn self-contained: KHÔNG cần thư viện ML ngoài, KHÔNG ghi vào database gốc.
 * - Chạy offline (không cần API key cho phần retrieval).
 *
 * Đây là một dạng "vector embedding" cổ điển nhưng đủ dùng để tư vấn sản phẩm
 * cho catalog nhỏ/trung bình như GoFoody.
 */
public class VectorStore {

    /** Một tài liệu được index = một món ăn (MenuItem) kèm vector TF-IDF đã chuẩn hoá. */
    public static final class Doc {
        public final MenuItem item;
        /** Vector TF-IDF đã L2-normalize: token -> trọng số. */
        public final Map<String, Double> vector;
        Doc(MenuItem item, Map<String, Double> vector) { this.item = item; this.vector = vector; }
    }

    /** Sản phẩm kèm điểm tương đồng (kết quả retrieval). */
    public static final class ScoredDoc {
        public final MenuItem item;
        public final double score;
        ScoredDoc(MenuItem item, double score) { this.item = item; this.score = score; }
    }

    private final List<Doc> docs = new ArrayList<>();
    /** idf cho từng token (tính từ corpus hiện tại). */
    private final Map<String, Double> idf = new HashMap<>();

    /** Văn bản dùng để embedding của một món: ghép tên + mô tả + tên nhà hàng. */
    public static String docText(MenuItem m) {
        StringBuilder sb = new StringBuilder();
        if (m.name != null) sb.append(m.name).append(' ');
        if (m.description != null) sb.append(m.description).append(' ');
        if (m.restaurantName != null) sb.append(m.restaurantName).append(' ');
        return sb.toString().trim();
    }

    /** Xây lại index từ danh sách sản phẩm. */
    public synchronized void build(List<MenuItem> items) {
        docs.clear();
        idf.clear();
        if (items == null || items.isEmpty()) return;

        // 1) tokenize từng doc + đếm document frequency
        List<List<String>> tokenized = new ArrayList<>();
        Map<String, Integer> df = new HashMap<>();
        for (MenuItem m : items) {
            List<String> toks = tokenize(docText(m));
            tokenized.add(toks);
            for (String t : new java.util.HashSet<>(toks)) df.merge(t, 1, Integer::sum);
        }
        // 2) idf = ln((1+N)/(1+df)) + 1  (smooth, tránh chia 0)
        int n = items.size();
        for (Map.Entry<String, Integer> e : df.entrySet()) {
            idf.put(e.getKey(), Math.log((1.0 + n) / (1.0 + e.getValue())) + 1.0);
        }
        // 3) vector TF-IDF cho từng doc + L2 normalize
        for (int i = 0; i < items.size(); i++) {
            docs.add(new Doc(items.get(i), tfidf(tokenized.get(i), idf)));
        }
    }

    /** Trả về top-K sản phẩm liên quan nhất (cosine similarity >= minScore), giảm dần theo score. */
    public synchronized List<ScoredDoc> retrieve(String query, int topK, double minScore) {
        if (docs.isEmpty()) return List.of();
        Map<String, Double> qvec = tfidf(tokenize(query == null ? "" : query), idf);
        double qnorm = norm(qvec);
        if (qnorm == 0) return List.of(); // câu hỏi không có token nào thuộc corpus

        List<ScoredDoc> scored = new ArrayList<>();
        for (Doc d : docs) {
            // d.vector đã L2-normalize nên cosine = dot(qvec, d.vector) / norm(qvec)
            double score = dot(qvec, d.vector) / qnorm;
            if (score >= minScore) scored.add(new ScoredDoc(d.item, score));
        }
        scored.sort((a, b) -> Double.compare(b.score, a.score));
        return scored.subList(0, Math.min(topK, scored.size()));
    }

    public int size() { return docs.size(); }
    public boolean isEmpty() { return docs.isEmpty(); }

    // ---- hàm tiện ích ----

    /**
     * Bảng từ đồng nghĩa song ngữ (Anh <-> Việt) cho thuật ngữ đồ ăn.
     * Giúp câu hỏi tiếng Việt ("có món gà không") khớp với món tiếng Anh ("Chicken")
     * trong corpus. Áp dụng cho cả doc và query (đối xứng).
     */
    private static final Map<String, List<String>> SYN = new HashMap<>();
    static {
        // Mỗi group: tất cả từ trong group là đồng nghĩa của nhau (đa hướng).
        group("chicken", "gà", "ga");
        group("rice", "cơm", "com");
        group("coffee", "cà phê", "ca phe", "cafe", "càphê", "caphe");
        group("ice", "kem");
        group("cream", "kem");
        group("burger", "bánh burger", "hamburger", "bánh mì kẹp");
        group("naan", "bánh naan");
        group("dosa", "bánh dosa");
        group("veg", "veggie", "vegetable", "chay", "rau");
        group("fried", "chiên", "rán");
        group("butter", "bơ");
        group("pizza", "bánh pizza");
        group("chocolate", "socola", "sôcôla");
        group("crispy", "giòn");
        group("hot", "nóng");
        group("spicy", "cay");
        group("food", "đồ ăn", "mon an");
        group("menu", "thực đơn", "thuc don");
        group("dish", "món", "món ăn", "mon an");
        group("drink", "đồ uống", "do uong");
        group("cheap", "rẻ", "re");
        group("price", "giá", "gia");
        group("dosa", "bánh kếp");
        group("combo", "phần", "phan");
    }

    /** Đăng ký một nhóm từ đồng nghĩa (multi-word được tách thành từng token đơn). */
    private static void group(String... args) {
        java.util.Set<String> terms = new java.util.LinkedHashSet<>();
        for (String a : args) {
            for (String w : a.trim().toLowerCase(Locale.ROOT).split("\\s+")) {
                if (!w.isEmpty()) terms.add(w);
            }
        }
        for (String a : terms) for (String b : terms) {
            if (!a.equals(b)) SYN.computeIfAbsent(a, k -> new ArrayList<>()).add(b);
        }
    }

    /**
     * Tách từ: giữ chữ cái/số mọi ngôn ngữ (bao gồm tiếng Việt có dấu),
     * bỏ từ thuần số & quá ngắn; đồng thời mở rộng bằng từ đồng nghĩa song ngữ.
     */
    static List<String> tokenize(String s) {
        List<String> out = new ArrayList<>();
        if (s == null || s.isBlank()) return out;
        for (String t : s.toLowerCase(Locale.ROOT).split("[^\\p{L}\\p{N}]+")) {
            if (t.length() < 2 || t.matches("\\d+")) continue;
            out.add(t);
            List<String> syns = SYN.get(t);
            if (syns != null) out.addAll(syns);
        }
        return out;
    }

    /** TF-IDF + L2 normalize cho một danh sách token (dùng idf chung). */
    private static Map<String, Double> tfidf(List<String> tokens, Map<String, Double> idf) {
        Map<String, Double> tf = new HashMap<>();
        for (String t : tokens) tf.merge(t, 1.0, Double::sum);
        Map<String, Double> v = new HashMap<>();
        for (Map.Entry<String, Double> e : tf.entrySet()) {
            Double w = idf.get(e.getKey());
            if (w == null) continue; // token không thuộc corpus (với query) -> bỏ qua
            v.put(e.getKey(), e.getValue() * w);
        }
        double n = norm(v);
        if (n > 0) for (Map.Entry<String, Double> e : v.entrySet()) e.setValue(e.getValue() / n);
        return v;
    }

    private static double dot(Map<String, Double> a, Map<String, Double> b) {
        if (a.size() > b.size()) { Map<String, Double> t = a; a = b; b = t; }
        double sum = 0;
        for (Map.Entry<String, Double> e : a.entrySet()) {
            Double y = b.get(e.getKey());
            if (y != null) sum += e.getValue() * y;
        }
        return sum;
    }

    private static double norm(Map<String, Double> v) {
        double s = 0;
        for (double w : v.values()) s += w * w;
        return Math.sqrt(s);
    }
}
