# Đề tài 5: Phát triển ứng dụng điều khiển máy chủ Ubuntu từ xa

## Giới thiệu

Ứng dụng này cho phép người dùng điều khiển và quản lý các máy chủ Ubuntu từ xa thông qua giao diện web hiện đại, sử dụng ngôn ngữ lập trình Java (Spring Boot) và chạy trên hệ điều hành Windows. Người dùng có thể kết nối tới nhiều máy chủ Ubuntu qua SSH, thực hiện các thao tác quản trị hệ thống, dịch vụ và firewall một cách dễ dàng.

---

## Tính năng chính

- **Kết nối SSH tới máy chủ Ubuntu**  
  Cho phép thêm, lưu và quản lý thông tin nhiều máy chủ Ubuntu, kết nối bảo mật qua SSH.

- **Liệt kê & điều khiển dịch vụ hệ thống**  
  Xem danh sách các dịch vụ (systemd), kiểm tra trạng thái, khởi động, dừng, khởi động lại dịch vụ từ xa.

- **Cấu hình lại dịch vụ**  
  Thực hiện các thao tác cấu hình, chỉnh sửa file cấu hình dịch vụ trực tiếp trên giao diện web.

- **Quản lý firewall (UFW)**  
  Kiểm tra trạng thái, bật/tắt firewall, thêm/xóa quy tắc firewall cho từng máy chủ.

- **Thực thi lệnh trên nhiều máy chủ**  
  Gửi lệnh shell tới một hoặc nhiều máy chủ cùng lúc, xem kết quả trả về theo từng máy chủ.

- **Giao diện đồ họa thân thiện**  
  Thiết kế hiện đại, dễ sử dụng, hỗ trợ thao tác nhanh, thông báo trạng thái rõ ràng.

---

## Yêu cầu hệ thống

- **Hệ điều hành:** Windows 10/11 (máy chạy ứng dụng)
- **Java:** JDK 17 trở lên (khuyến nghị Java 21)
- **Build tool:** Maven 3.6+
- **Máy chủ Ubuntu:** Đã bật SSH, có quyền sudo

---

## Hướng dẫn cài đặt & chạy

1. **Clone source code**
    ```bash
    git clone <repo-url>
    cd project-nly3s3
    ```

2. **Cấu hình thông tin máy chủ**  
   Thêm thông tin máy chủ qua giao diện web sau khi đăng nhập.

3. **Build & chạy ứng dụng** Xem tệp đính kèm

4. **Truy cập giao diện web**  
   Mở trình duyệt và truy cập: [http://localhost:8080](http://localhost:8080)

---

## Cấu trúc thư mục chính

```
project-nly3s3/
├── src/
│   ├── main/
│   │   ├── java/com/example/sshcontrol/...
│   │   └── resources/templates/      # Giao diện HTML
│   └── test/
├── README.md
├── pom.xml
└── ...
```

---

## Một số hình ảnh giao diện

> _Thêm ảnh minh họa giao diện dashboard, quản lý dịch vụ, firewall, thực thi lệnh..._

---

## Đóng góp & phát triển

- Mọi ý kiến đóng góp, báo lỗi hoặc đề xuất chức năng mới vui lòng gửi qua [Issues](https://github.com/your-repo/issues) hoặc liên hệ trực tiếp với nhóm phát triển.
- Sinh viên tự do mở rộng thêm các chức năng nâng cao như: giám sát tài nguyên, quản lý user, backup, v.v.

---

## Tác giả

- **Sinh viên thực hiện:** Đỗ Nhật Anh - B2204914
- **Giảng viên hướng dẫn:** TS. Lâm Chí Nguyện
- **Năm học:** HKIII 2024-2025

---

## License

Dành cho mục đích học tập, nghiên cứu. 