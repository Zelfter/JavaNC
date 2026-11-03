# 🍔 Go Foody — Website đặt đồ ăn bằng Java Quarkus

Bản chuyển đổi từ project Python/Flask sang **Java Quarkus**, dùng MySQL. Bao gồm
tích hợp **AI Chatbot tư vấn món ăn bằng RAG (Retrieval-Augmented Generation)**.

---

## ✨ Tính năng

### Tính năng chính (E-commerce)
- Đăng ký / đăng nhập theo vai trò (Admin, Manager, Customer).
- Duyệt nhà hàng theo khu vực (TLY, KANNUR, CALICUT).
- Xem menu, thêm vào giỏ hàng, đặt hàng, thanh toán (COD / Card / UPI).
- Quản lý nhà hàng & menu (Manager), quản lý đơn hàng & phản hồi liên hệ (Admin).
- Đánh giá nhà hàng, gửi liên hệ, xem thống kê doanh thu.
- Swagger UI cho API REST tại `/swagger-ui`.

### AI Chatbot RAG (tích hợp mới)
- Widget chat pop-up nổi ở góc dưới bên phải mọi trang.
- Tư vấn món ăn dựa trên dữ liệu thực tế trong database (tên, mô tả, giá, nhà hàng).
- **RAG pipeline** đầy đủ: Ingestion → Embedding → Retrieval (top-K) → Prompt → LLM Generation.
- Hoạt động ngay cả **chưa có API key** (chế độ trả lời từ context sản phẩm).
- Tìm kiếm song ngữ Anh–Vi nhờ bảng từ đồng nghĩa (ví dụ hỏi "có món gà không" sẽ khớp "Chicken").

---

## 🧱 Tech Stack

| Thành phần | Công nghệ |
|---|---|
| Backend | Java 17, Quarkus 3.15.3, JAX-RS (`jakarta.ws.rs`), CDI |
| Database | MySQL qua JDBC thuần (không dùng Hibernate ORM) |
| Build | Maven |
| Frontend | Server-side rendered HTML (Quarkus static resources) |
| AI / RAG | In-memory Vector Store (TF-IDF + cosine similarity), LLM API OpenAI-compatible |

---

## 📁 Cấu trúc thư mục

```
src/main/java/com/gofoody/
├── model/            # POJO: MenuItem, Restaurant, User, CartItem, ...
├── resource/         # ApiResource (JSON), WebResource (HTML pages)
├── service/          # Db.java — toàn bộ truy vấn JDBC + seed dữ liệu
├── util/             # Html.java — layout & helper (chèn ChatbotWidget tại đây)
└── chatbot/          # ✨ AI Chatbot RAG (thư mục mới, tách biệt)
    ├── ChatbotConfig.java     # đọc config từ chatbot.properties / env var
    ├── VectorStore.java        # in-memory TF-IDF + cosine + từ đồng nghĩa song ngữ
    ├── RagService.java         # ingestion, retrieval, build prompt, gọi LLM
    ├── ChatbotModels.java      # DTO request/response
    ├── ChatbotResource.java    # API: POST /api/chatbot/ask (+ /refresh, /health)
    └── ChatbotWidget.java      # HTML/CSS/JS widget pop-up

src/main/resources/
├── application.properties     # cấu hình MySQL (file gốc, KHÔNG sửa)
├── chatbot.properties         # ✨ cấu hình LLM API key (file mới)
└── META-INF/resources/static/ # CSS, ảnh món ăn, logo
```

---

## 🚀 Cách chạy

### Yêu cầu
- JDK 17 trở lên
- MySQL (XAMPP/MySQL Workbench) đang chạy
- Maven

### Bước 1 — Tạo database
Mở MySQL Workbench và chạy:

```sql
CREATE DATABASE IF NOT EXISTS gofoody_quarkus
CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

> Ứng dụng cũng tự tạo các bảng và seed dữ liệu mẫu khi khởi động lần đầu
> (xem `Db.java`). Không cần chạy file SQL thủ công.

### Bước 2 — Cấu hình kết nối MySQL
Sửa `src/main/resources/application.properties` nếu MySQL có mật khẩu:

```properties
quarkus.datasource.username=root
quarkus.datasource.password=          # điền mật khẩu của bạn
quarkus.datasource.jdbc.url=jdbc:mysql://localhost:3306/gofoody_quarkus?...
```

### Bước 3 — Chạy ứng dụng

Chế độ dev (tự reload):

```bash
mvn quarkus:dev
```

Hoặc build rồi chạy:

```bash
mvn package -DskipTests
java -jar target/quarkus-app/quarkus-run.jar
```

Mở trình duyệt: **http://localhost:8080**

### Chạy trên NetBeans
1. File → Open Project.
2. Chuột phải project → Run Maven → Goals... → nhập `quarkus:dev`.

---

## 🔐 Tài khoản mẫu

| Vai trò | Username | Password |
|---|---|---|
| Admin | `admin` | `123` |
| Manager | `manager` | `123456` |
| Customer | `customer` | `123456` |
| Tương thích project Python cũ | `kg28` | `Jammu@123` |

---

## 🤖 Cấu hình AI Chatbot RAG

Chatbot dùng **2 thành phần**:

1. **Retrieval** (luôn chạy, offline): Vector Store TF-IDF trong bộ nhớ — không cần
   thư viện ML ngoài, không ghi vào database gốc.
2. **Generation** (tùy chọn): gọi LLM API theo chuẩn OpenAI-compatible. Cần API key.

### Điền LLM API Key

Mở file mới `src/main/resources/chatbot.properties`:

```properties
# >>> ĐIỀN API KEY VÀO ĐÂY <<<
chatbot.llm.api.key=sk-xxxxxxxxxxxxxxxx

chatbot.llm.base.url=https://api.openai.com/v1
chatbot.llm.model=gpt-4o-mini
chatbot.rag.topk=4
chatbot.rag.min.score=0.01
chatbot.llm.timeout.seconds=30
```

Hoặc dùng biến môi trường (ưu tiên hơn file):

```bash
export CHATBOT_LLM_API_KEY=sk-xxxxxxxxxxxxxxxx
export CHATBOT_LLM_BASE_URL=https://api.openai.com/v1
export CHATBOT_LLM_MODEL=gpt-4o-mini
```

### Nhà cung cấp LLM hỗ trợ (chuẩn OpenAI-compatible)

| Provider | `base.url` | `model` ví dụ |
|---|---|---|
| OpenAI | `https://api.openai.com/v1` | `gpt-4o-mini`, `gpt-3.5-turbo` |
| OpenRouter | `https://openrouter.ai/api/v1` | `openai/gpt-4o-mini` |
| Ollama (local) | `http://localhost:11434/v1` | `llama3.1:8b` |
| Azure OpenAI | endpoint của bạn | deployment name |

> **Bỏ trống `api.key`**: chatbot vẫn hoạt động — trả lời trực tiếp từ danh sách
> sản phẩm được retrieve (không gọi LLM). Phù hợp để demo nhanh.

### API Endpoints của Chatbot

| Method | Path | Mô tả |
|---|---|---|
| `POST` | `/api/chatbot/ask` | Hỏi chatbot. Body: `{"question":"có món gà không"}` |
| `POST` | `/api/chatbot/refresh` | Rebuild vector store từ DB (sau khi thêm/sửa menu) |
| `GET` | `/api/chatbot/health` | Trạng thái: số món đã index, LLM enable, model... |

Ví dụ:

```bash
curl -X POST http://localhost:8080/api/chatbot/ask \
  -H "Content-Type: application/json" \
  -d '{"question":"có món gà không"}'
```

Response:

```json
{
  "answer": "Tôi tìm thấy một số món ăn phù hợp...",
  "usedLlm": false,
  "retrieved": 4,
  "products": [
    {"name":"Butter Chicken","restaurant":"Cabane Bambou","price":220.0,"link":"/restaurant/16"},
    ...
  ],
  "error": null
}
```

---

## 🛠️ Kiến trúc RAG

```
Câu hỏi user
    │
    ▼
┌──────────────────┐    db.menu(0,null)     ┌──────────────────┐
│  RagService       │◄──────────────────────│  MySQL (menu_items)│
│  (ingestion)     │   đọc 1 lần khi start  └──────────────────┘
└──────────────────┘
    │ build TF-IDF vectors
    ▼
┌──────────────────┐
│  VectorStore     │  in-memory, KHÔNG ghi DB
│  (TF-IDF + cosine)│
└──────────────────┘
    │ retrieve top-K (4)
    ▼
Context (sản phẩm) + Câu hỏi  ──►  Prompt  ──►  LLM API  ──►  Câu trả lời
                                                    │
                                          (nếu chưa có key → fallback
                                           trả context sản phẩm trực tiếp)
```

### Cách chatbot tích hợp vào giao diện
Chỉ **một dòng** được thêm vào file gốc `Html.java` (gọi fully-qualified, không thêm import):

```java
return """...""".formatted(esc(title), admin, manager, auth, body)
       + com.gofoody.chatbot.ChatbotWidget.widget();   // ← dòng duy nhất thêm vào
```

Toàn bộ logic chatbot nằm trong thư mục mới `com.gofoody.chatbot`, tách biệt hoàn toàn
với code cũ — không sửa/xóa/refactor bất kỳ file, schema hay logic hiện có nào.

---

## ⚠️ Lưu ý

- Bản này **không dùng Hibernate ORM** để tránh lỗi ByteBuddy khi NetBeans chạy JDK 25.
  Database tạo bằng JDBC thuần trong `Db.java`.
- **Phần Retrieval dùng TF-IDF (keyword-based)** nên truy vấn theo *loại món* hoạt động
  tốt (gà, cơm, pizza, cà phê...). Truy vấn theo *ý nghĩa giá* ("món rẻ nhất") cần có
  giá trong văn bản embedding — nếu muốn, bổ sung trường giá vào `VectorStore.docText()`.
- **Runtime cần MySQL** đang chạy (yêu cầu vốn có của dự án). Phần compile không cần DB.
- AI Chatbot endpoint `/api/chatbot/*` không yêu cầu đăng nhập (giống các endpoint `/api/*` khác).
