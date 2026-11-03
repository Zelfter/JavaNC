# Hướng dẫn chạy bằng NetBeans + MySQL Workbench

## 1. Tạo database trong MySQL Workbench

Mở MySQL Workbench, mở tab SQL và chạy:

```sql
CREATE DATABASE IF NOT EXISTS gofoody_quarkus CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

Không cần tạo bảng. Ứng dụng sẽ tự tạo bảng khi chạy.

## 2. Sửa mật khẩu MySQL

Mở file:

```text
src/main/resources/application.properties
```

Nếu MySQL của bạn có mật khẩu, sửa:

```properties
quarkus.datasource.password=
```

thành ví dụ:

```properties
quarkus.datasource.password=123456
```

## 3. Chạy project

Trong NetBeans:

```text
File -> Open Project -> chọn thư mục gofoody-quarkus-final
Chuột phải project -> Run Maven -> Goals...
Nhập: quarkus:dev
Bấm OK
```

## 4. Mở website

```text
http://localhost:8080
```

## 5. Tài khoản mẫu

```text
Admin: admin / 123
Manager: manager / 123456
Customer: customer / 123456
Customer gốc Python: kg28 / Jammu@123
```

## 6. Nếu port 8080 bị chiếm

Mở `application.properties` và đổi:

```properties
quarkus.http.port=8081
```

Sau đó mở:

```text
http://localhost:8081
```
