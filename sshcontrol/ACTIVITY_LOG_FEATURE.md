# Tính Năng Lịch Sử Hoạt Động (Activity Log)

## Giới Thiệu
Tính năng lịch sử hoạt động ghi nhận lại tất cả các thao tác của người dùng trong hệ thống SSH Control, bao gồm:
- Đăng nhập / Đăng xuất
- Kết nối / Ngắt kết nối server
- Tải lên / Tải xuống / Xóa file
- Thực thi lệnh
- Thêm / Xóa / Chỉnh sửa server
- Khởi động / Dừng / Khởi động lại dịch vụ

## Cấu Trúc Dữ Liệu

### Model: ActivityLog
```java
@Entity
@Table(name = "activity_logs")
public class ActivityLog {
    Long id;                           // ID duy nhất
    User user;                         // Người dùng thực hiện hành động
    String action;                     // Loại hành động (LOGIN, SERVER_DELETE, FILE_UPLOAD, v.v.)
    String description;                // Mô tả chi tiết hành động
    String serverId;                   // ID server liên quan (nếu có)
    LocalDateTime createdAt;           // Thời gian thực hiện
    String status;                     // Trạng thái (SUCCESS, FAILED, PENDING)
    String details;                    // Chi tiết bổ sung hoặc lỗi
}
```

### Bảng Cơ Sở Dữ Liệu
```sql
CREATE TABLE activity_logs (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id),
    action VARCHAR(50) NOT NULL,
    description TEXT,
    server_id VARCHAR(255),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    status VARCHAR(20) DEFAULT 'PENDING',
    details TEXT
);
```

## Dịch Vụ: ActivityLogService

### Các Phương Thức Chính

#### 1. Ghi Nhận Hoạt Động
```java
// Ghi nhận cơ bản
activityLogService.logActivity(user, "LOGIN", "Người dùng đã đăng nhập");

// Ghi nhận với server
activityLogService.logActivity(user, "SERVER_DELETE", "Xóa server 192.168.1.1", "1");

// Ghi nhận với trạng thái
activityLogService.logActivity(user, "LOGIN_FAILED", "Mật khẩu sai", null, "FAILED");

// Ghi nhận chi tiết đầy đủ
activityLogService.logActivityWithDetails(
    user, "FILE_UPLOAD", "Tải lên tệp", "1", "SUCCESS", 
    "File: config.conf, Size: 2048 bytes"
);
```

#### 2. Truy Vấn Hoạt Động
```java
// Lấy tất cả hoạt động
Page<ActivityLog> activities = activityLogService.getUserActivities(user, pageRequest);

// Lọc theo hành động
Page<ActivityLog> logins = activityLogService.getActivitiesByAction(user, "LOGIN", pageRequest);

// Lọc theo trạng thái
Page<ActivityLog> failures = activityLogService.getActivitiesByStatus(user, "FAILED", pageRequest);

// Lọc theo server
Page<ActivityLog> serverOps = activityLogService.getActivitiesByServer(user, "1", pageRequest);

// Theo thời gian
List<ActivityLog> recent = activityLogService.getActivitiesByDateRange(user, start, end);

// 10 hoạt động gần nhất
List<ActivityLog> top10 = activityLogService.getRecentActivities(user);
```

#### 3. Thống Kê
```java
ActivityLogService.ActivityStatistics stats = activityLogService.getStatistics(user);
// stats.getTotalActivities()     - Tổng hoạt động
// stats.getLoginCount()          - Lần đăng nhập
// stats.getServerConnectCount()  - Kết nối server
// stats.getFileOperationCount()  - Thao tác file
// stats.getFailedActivities()    - Hoạt động thất bại
```

## Controller: ActivityLogController

### Endpoints
- `GET /log` - Trang chính hiển thị lịch sử hoạt động
- `GET /log?page=0` - Phân trang
- `GET /log?action=LOGIN` - Lọc theo hành động
- `GET /log?status=FAILED` - Lọc theo trạng thái
- `POST /api/activities/date-range` - Lọc theo khoảng thời gian
- `GET /api/statistics` - Lấy thống kê
- `GET /api/recent` - Lấy 10 hoạt động gần nhất

### Giao Diện: log.html
- Bảng thống kê hoạt động (4 cards)
- Bộ lọc (hành động, trạng thái)
- Danh sách hoạt động dạng timeline
- Phân trang

## Các Loại Hành Động (Action Types)

| Hành Động | Biểu Tượng | Mô Tả |
|-----------|-----------|-------|
| LOGIN | sign-in-alt | Đăng nhập |
| LOGOUT | sign-out-alt | Đăng xuất |
| LOGIN_FAILED | exclamation | Đăng nhập thất bại |
| SERVER_CONNECT | link | Kết nối server |
| SERVER_DISCONNECT | unlink | Ngắt kết nối |
| SERVER_ADD | plus-circle | Thêm server |
| SERVER_DELETE | minus-circle | Xóa server |
| SERVER_EDIT | edit | Chỉnh sửa server |
| FILE_UPLOAD | cloud-upload-alt | Tải lên tệp |
| FILE_DOWNLOAD | cloud-download-alt | Tải xuống tệp |
| FILE_DELETE | trash-alt | Xóa tệp |
| COMMAND_EXECUTE | terminal | Thực thi lệnh |
| SERVICE_START | play | Khởi động dịch vụ |
| SERVICE_STOP | stop | Dừng dịch vụ |
| SERVICE_RESTART | sync-alt | Khởi động lại dịch vụ |

## Trạng Thái Hoạt Động

| Trạng Thái | Màu | Ý Nghĩa |
|-----------|-----|--------|
| SUCCESS | Xanh lá | Thành công |
| FAILED | Đỏ | Thất bại |
| PENDING | Vàng | Đang xử lý |

## Cách Sử Dụng

### 1. Ghi Nhận Hoạt Động Trong Controller
```java
@Autowired
private ActivityLogService activityLogService;

@PostMapping("/upload")
public String upload(@RequestParam MultipartFile file, HttpSession session) {
    User user = (User) session.getAttribute("user");
    
    try {
        // ... logic upload file
        activityLogService.logActivity(user, "FILE_UPLOAD", 
            "Tải lên: " + file.getOriginalFilename());
    } catch (Exception e) {
        activityLogService.logActivity(user, "FILE_UPLOAD", 
            "Lỗi upload: " + file.getOriginalFilename(), null, "FAILED");
    }
}
```

### 2. Truy Vấn Hoạt Động
```java
@GetMapping("/api/my-logs")
public Page<ActivityLog> getMyLogs(
        @RequestParam(defaultValue = "0") int page,
        HttpSession session) {
    User user = (User) session.getAttribute("user");
    Pageable pageable = PageRequest.of(page, 20);
    return activityLogService.getUserActivities(user, pageable);
}
```

### 3. Hiển Thị Trong Giao Diện
```html
<a href="/log" class="btn btn-primary">
    <i class="fas fa-history"></i> Lịch sử hoạt động
</a>
```

## Maintenance

### Xóa Hoạt Động Cũ
```java
// Xóa hoạt động cũ hơn 90 ngày (gọi từ scheduled task)
@Scheduled(cron = "0 0 2 * * *")  // 2:00 AM mỗi ngày
public void cleanupOldLogs() {
    activityLogService.deleteOldActivities();
}
```

## Hiệu Năng
- Sử dụng index để tăng tốc độ truy vấn
- Phân trang 20 bản ghi mỗi trang
- Auto-cleanup logs cũ hơn 90 ngày
- Hỗ trợ lọc và tìm kiếm nhanh

## Bảo Mật
- Chỉ xem được lịch sử của người dùng hiện tại
- Tất cả hoạt động đều ghi lại thời gian chính xác
- Lỗi đăng nhập được ghi nhận
- Chi tiết lỗi được lưu để debug

## Tương Lai
- [ ] Export lịch sử ra file (CSV, PDF)
- [ ] Thêm chart thống kê hoạt động theo thời gian
- [ ] Cảnh báo hoạt động bất thường (brute-force login, etc.)
- [ ] Audit log cho admin
- [ ] Real-time notifications cho một số hoạt động
