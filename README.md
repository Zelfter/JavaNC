# Go Foody - Website đặt đồ ăn bằng Java Quarkus

Bản chuyển đổi từ project Python/Flask sang Java Quarkus, chạy bằng MySQL.

## Cách chạy nhanh trên NetBeans

1. Mở MySQL Workbench và tạo database:

```sql
CREATE DATABASE IF NOT EXISTS gofoody_quarkus CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

2. Mở project bằng NetBeans: File -> Open Project.
3. Sửa `src/main/resources/application.properties` nếu MySQL có mật khẩu.
4. Chuột phải project -> Run Maven -> Goals... -> nhập:

```text
quarkus:dev
```

5. Mở trình duyệt: http://localhost:8080

## Tài khoản mẫu

- Admin: `admin` / `123`
- Manager: `manager` / `123456`
- Customer: `customer` / `123456`
- Tài khoản tương thích project Python: `kg28` / `Jammu@123`

## Lưu ý

Bản này không dùng Hibernate ORM để tránh lỗi ByteBuddy khi NetBeans đang chạy JDK 25. Database được tạo bằng JDBC thuần trong Java.
