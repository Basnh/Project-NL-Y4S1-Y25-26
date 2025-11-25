package com.example.sshcontrol.sshcontrol.controller;

import com.example.sshcontrol.model.User;
import com.example.sshcontrol.model.Server;
import com.example.sshcontrol.service.UserService;
import com.example.sshcontrol.service.SystemStatsService;
import com.example.sshcontrol.service.ActivityLogService;
import com.example.sshcontrol.repository.ServerRepository;
import java.util.List;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.*;
import java.awt.FontMetrics;

@Controller
public class AuthController {
    @Autowired
    private UserService userService;

    @Autowired
    private ActivityLogService activityLogService;

    // Hiển thị form đăng nhập và tạo CAPTCHA mới
    @GetMapping("/login")
    public String showLoginPage(HttpSession session, Model model) {
        if (session.getAttribute("user") != null) {
            return "redirect:/";
        }
        
        Map<String, String> captcha = generateCaptcha();
        System.out.println("Generated CAPTCHA - Text: " + captcha.get("text"));
        System.out.println("Generated CAPTCHA - Image exists: " + (captcha.get("image") != null));
        
        model.addAttribute("captchaImage", captcha.get("image"));
        session.setAttribute("captcha", captcha.get("text"));
        return "login";
    }

    // Đăng xuất
    @PostMapping("/logout")
    public String logout(HttpSession session) {
        User user = (User) session.getAttribute("user");
        if (user != null) {
            activityLogService.logActivity(user, "LOGOUT", "Người dùng đã đăng xuất");
        }
        
        if (session != null) {
            session.invalidate();
        }
        return "redirect:/";
    }

    // Hiển thị trang đăng ký
    @GetMapping("/register")
    public String showRegisterPage(HttpSession session, Model model) {
        Map<String, String> captcha = generateCaptcha();
        model.addAttribute("captchaImage", captcha.get("image"));
        session.setAttribute("captcha", captcha.get("text"));
        return "register";
    }

    // Xử lý đăng ký với validation mở rộng
    @PostMapping("/register")
    public String register(@RequestParam String username, 
                          @RequestParam String password, 
                          @RequestParam String confirmPassword,
                          @RequestParam String fullName,
                          @RequestParam String email,
                          @RequestParam(required = false) String phoneNumber,
                          @RequestParam String captcha,
                          HttpSession session,
                          Model model) {
        
        // Kiểm tra CAPTCHA
        String sessionCaptcha = (String) session.getAttribute("captcha");
        if (sessionCaptcha == null || !sessionCaptcha.equalsIgnoreCase(captcha)) {
            Map<String, String> newCaptcha = generateCaptcha();
            model.addAttribute("captchaImage", newCaptcha.get("image"));
            session.setAttribute("captcha", newCaptcha.get("text"));
            model.addAttribute("error", "Mã CAPTCHA không đúng!");
            return "register";
        }

        // Validation cơ bản
        if (username == null || username.trim().isEmpty()) {
            Map<String, String> newCaptcha = generateCaptcha();
            model.addAttribute("captchaImage", newCaptcha.get("image"));
            session.setAttribute("captcha", newCaptcha.get("text"));
            model.addAttribute("error", "Tên đăng nhập không được để trống!");
            return "register";
        }
        
        if (fullName == null || fullName.trim().isEmpty()) {
            Map<String, String> newCaptcha = generateCaptcha();
            model.addAttribute("captchaImage", newCaptcha.get("image"));
            session.setAttribute("captcha", newCaptcha.get("text"));
            model.addAttribute("error", "Họ tên không được để trống!");
            return "register";
        }
        
        if (email == null || email.trim().isEmpty()) {
            Map<String, String> newCaptcha = generateCaptcha();
            model.addAttribute("captchaImage", newCaptcha.get("image"));
            session.setAttribute("captcha", newCaptcha.get("text"));
            model.addAttribute("error", "Email không được để trống!");
            return "register";
        }
        
        // Validation email format
        if (!email.matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")) {
            Map<String, String> newCaptcha = generateCaptcha();
            model.addAttribute("captchaImage", newCaptcha.get("image"));
            session.setAttribute("captcha", newCaptcha.get("text"));
            model.addAttribute("error", "Email không đúng định dạng!");
            return "register";
        }
        
        if (password == null || password.length() < 6) {
            Map<String, String> newCaptcha = generateCaptcha();
            model.addAttribute("captchaImage", newCaptcha.get("image"));
            session.setAttribute("captcha", newCaptcha.get("text"));
            model.addAttribute("error", "Mật khẩu phải có ít nhất 6 ký tự!");
            return "register";
        }
        
        if (!password.equals(confirmPassword)) {
            Map<String, String> newCaptcha = generateCaptcha();
            model.addAttribute("captchaImage", newCaptcha.get("image"));
            session.setAttribute("captcha", newCaptcha.get("text"));
            model.addAttribute("error", "Mật khẩu xác nhận không khớp!");
            return "register";
        }

        // Kiểm tra username đã tồn tại
        if (userService.existsByUsername(username)) {
            Map<String, String> newCaptcha = generateCaptcha();
            model.addAttribute("captchaImage", newCaptcha.get("image"));
            session.setAttribute("captcha", newCaptcha.get("text"));
            model.addAttribute("error", "Tên đăng nhập đã tồn tại!");
            return "register";
        }

        // Kiểm tra email đã tồn tại
        if (userService.existsByEmail(email)) {
            Map<String, String> newCaptcha = generateCaptcha();
            model.addAttribute("captchaImage", newCaptcha.get("image"));
            session.setAttribute("captcha", newCaptcha.get("text"));
            model.addAttribute("error", "Email đã được sử dụng!");
            return "register";
        }

        // Tạo user mới
        User newUser = new User(username, password, fullName, email, phoneNumber);
        newUser.setServers(new ArrayList<>()); // Khởi tạo danh sách server rỗng
        userService.save(newUser);

        // Xóa CAPTCHA khỏi session
        session.removeAttribute("captcha");
        session.removeAttribute("captchaImage");

        model.addAttribute("success", "Đăng ký thành công! Hãy đăng nhập.");
        return "login";
    }

    // Danh sách máy chủ
    @GetMapping("/server-list")
    public String serverList(Model model, HttpSession session) {
        User user = (User) session.getAttribute("user");
        if (user == null) {
            return "redirect:/login";
        }
    
    model.addAttribute("servers", user.getServers());
    return "server-list";
    }

    // Form đăng nhập SSH vào máy chủ
    @GetMapping("/server-login")
    public String showServerLogin(@RequestParam String ip, Model model, HttpSession session) {
        User user = (User) session.getAttribute("user");
        if (user == null) return "redirect:/login";
        Server server = user.getServers().stream()
            .filter(s -> s.getIp() != null && s.getIp().equals(ip))
            .findFirst().orElse(null);
        if (server == null) return "redirect:/server-list";
        model.addAttribute("server", server);
        return "server-login";
    }

    // Xử lý đăng nhập SSH vào máy chủ
    @PostMapping("/server-login")
    public String serverLogin(@RequestParam String ip,
                              @RequestParam String sshUsername,
                              @RequestParam String sshPassword,
                              HttpSession session) {
        session.setAttribute("host", ip);
        session.setAttribute("username", sshUsername);
        session.setAttribute("password", sshPassword);
        return "redirect:/dashboard";
    }

    // Hiển thị form thêm máy chủ
    @GetMapping("/add-server")
    public String showAddServer(Model model) {
        model.addAttribute("server", new Server());
        return "add-server";
    }

    // Xử lý thêm máy chủ
    @Autowired
    private ServerRepository serverRepository;

    @PostMapping("/add-server")
    public String addServer(@ModelAttribute Server server, HttpSession session, Model model) {
        User user = (User) session.getAttribute("user");
        if (user == null) return "redirect:/login";

        // Validation cơ bản
        if (server.getIp() == null || server.getIp().trim().isEmpty()) {
            model.addAttribute("server", server);
            model.addAttribute("error", "IP không được để trống!");
            return "add-server";
        }
        
        if (server.getName() == null || server.getName().trim().isEmpty()) {
            model.addAttribute("server", server);
            model.addAttribute("error", "Tên máy chủ không được để trống!");
            return "add-server";
        }
        
        if (server.getSshUsername() == null || server.getSshUsername().trim().isEmpty()) {
            model.addAttribute("server", server);
            model.addAttribute("error", "SSH Username không được để trống!");
            return "add-server";
        }
        
        if (server.getSshPassword() == null || server.getSshPassword().trim().isEmpty()) {
            model.addAttribute("server", server);
            model.addAttribute("error", "SSH Password không được để trống!");
            return "add-server";
        }

        // Refresh user from database
        user = userService.findByUsername(user.getUsername());
        
        // Kiểm tra trùng IP + SSH Username + Password trong database
        boolean exists = serverRepository.findAll().stream()
            .anyMatch(s -> s.getIp() != null && 
                         s.getIp().equalsIgnoreCase(server.getIp()) &&
                         s.getSshUsername() != null &&
                         s.getSshUsername().equalsIgnoreCase(server.getSshUsername()) &&
                         s.getSshPassword() != null &&
                         s.getSshPassword().equals(server.getSshPassword()));
        
        if (exists) {
            model.addAttribute("server", server);
            model.addAttribute("error", "Máy chủ với IP, SSH Username và Password này đã tồn tại!");
            return "add-server";
        }

        // Set user cho server và lưu vào database
        server.setUser(user);
        serverRepository.save(server);
        
        // Cập nhật session với user mới
        session.setAttribute("user", user);
        
        return "redirect:/dashboard";
    }

    // Xóa máy chủ
    @PostMapping("/delete-server")
    public String deleteServer(@RequestParam String ip, HttpSession session, RedirectAttributes redirectAttributes) {
        return handleServerDeletion(ip, session, redirectAttributes);
    }

    // Thêm method GET để xóa server
    @GetMapping("/delete-server")
    public String deleteServerGet(@RequestParam String ip, HttpSession session, RedirectAttributes redirectAttributes) {
        return handleServerDeletion(ip, session, redirectAttributes);
    }

    // Helper method để xử lý xóa server
    private String handleServerDeletion(String ip, HttpSession session, RedirectAttributes redirectAttributes) {
        try {
            User sessionUser = (User) session.getAttribute("user");
            if (sessionUser == null) {
                redirectAttributes.addFlashAttribute("error", "Phiên làm việc hết hạn!");
                return "redirect:/login";
            }

            // Nạp lại user từ database để có servers mới nhất
            User currentUser = userService.findByUsername(sessionUser.getUsername());
            if (currentUser == null) {
                redirectAttributes.addFlashAttribute("error", "Không tìm thấy người dùng!");
                return "redirect:/login";
            }

            // Tìm server trong danh sách của user
            Server serverToDelete = null;
            if (currentUser.getServers() != null) {
                serverToDelete = currentUser.getServers().stream()
                    .filter(s -> s.getIp() != null && s.getIp().equals(ip))
                    .findFirst()
                    .orElse(null);
            }

            if (serverToDelete != null) {
                try {
                    // **QUAN TRỌNG**: Xóa server từ user.servers list trước
                    currentUser.getServers().remove(serverToDelete);
                    
                    // Sau đó xóa từ database
                    serverRepository.delete(serverToDelete);
                    serverRepository.flush(); // Đảm bảo lưu vào DB
                    
                    // Lưu user (để cập nhật relationship)
                    userService.save(currentUser);
                    
                    // Ghi nhận hoạt động
                    activityLogService.logActivity(currentUser, "SERVER_DELETE", 
                        "Xóa máy chủ: " + serverToDelete.getName() + " (" + ip + ")");
                    
                    // Refresh user data in session
                    currentUser = userService.findByUsername(currentUser.getUsername());
                    session.setAttribute("user", currentUser);
                    
                    redirectAttributes.addFlashAttribute("message", "Xóa máy chủ thành công!");
                } catch (Exception e) {
                    e.printStackTrace();
                    
                    // Ghi nhận lỗi
                    activityLogService.logActivity(currentUser, "SERVER_DELETE_FAILED", 
                        "Lỗi khi xóa máy chủ: " + ip, null, "FAILED");
                    
                    redirectAttributes.addFlashAttribute("error", "Lỗi khi xóa máy chủ: " + e.getMessage());
                }
            } else {
                redirectAttributes.addFlashAttribute("error", "Không tìm thấy máy chủ với IP: " + ip);
            }
        } catch (Exception e) {
            e.printStackTrace();
            redirectAttributes.addFlashAttribute("error", "Lỗi hệ thống: " + e.getMessage());
        }

        return "redirect:/server-list";
    }

    // Hiển thị trang dashboard
    @Autowired
    private SystemStatsService systemStatsService;

    @GetMapping("/dashboard")
    public String dashboard(Model model, HttpSession session) {
        User sessionUser = (User) session.getAttribute("user");
        if (sessionUser == null) {
            return "redirect:/login";
        }

        User currentUser = userService.findByUsername(sessionUser.getUsername());
        if (currentUser == null) {
            return "redirect:/login";
        }

        List<Server> servers = serverRepository.findByUser(currentUser);
        servers.forEach(server -> {
            boolean isOnline = checkServerStatus(server.getIp());
            server.setOnline(isOnline);
            serverRepository.save(server);
        });

        // Get system stats
        Map<String, Object> stats = systemStatsService.getSystemStats(servers);

        model.addAttribute("user", currentUser);
        model.addAttribute("servers", servers);
        model.addAttribute("totalServers", stats.get("totalServers"));
        model.addAttribute("activeServers", stats.get("activeServers"));
        model.addAttribute("serverUpPercentage", stats.get("serverUpPercentage"));
        model.addAttribute("avgCpuUsage", stats.get("avgCpuUsage")); 
        model.addAttribute("avgRamUsage", stats.get("avgRamUsage"));
        model.addAttribute("minUptime", stats.get("minUptime"));
        model.addAttribute("maxUptime", stats.get("maxUptime")); 
        model.addAttribute("avgUptime", stats.get("avgUptime"));
        
        return "dashboard";
    }


    private boolean checkServerStatus(String ip) {
        try {
            // Ping test
            InetAddress address = InetAddress.getByName(ip);
            boolean reachable = address.isReachable(3000); // 3 seconds timeout
            
            if (!reachable) {
                return false;
            }
            
            // SSH port test (port 22)
            try (Socket socket = new Socket()) {
                socket.connect(new InetSocketAddress(ip, 22), 3000);
                return true;
            } catch (IOException e) {
                return false;
            }
        } catch (Exception e) {
            return false;
        }
    }

    // Generate CAPTCHA
    private Map<String, String> generateCaptcha() {
        Map<String, String> result = new HashMap<>();
        String captchaText = generateRandomString(5);
        result.put("text", captchaText);
        
        try {
            String captchaImage = generateCaptchaImage(captchaText);
            if (captchaImage != null && !captchaImage.isEmpty()) {
                result.put("image", captchaImage);
                System.out.println("CAPTCHA image generated successfully");
            } else {
                System.out.println("Failed to generate CAPTCHA image");
            }
        } catch (IOException e) {
            System.err.println("Error generating CAPTCHA image: " + e.getMessage());
            e.printStackTrace();
        }
        
        return result;
    }

    // Generate random string for CAPTCHA
    private String generateRandomString(int length) {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        Random random = new Random();
        StringBuilder result = new StringBuilder();
        
        for (int i = 0; i < length; i++) {
            result.append(chars.charAt(random.nextInt(chars.length())));
        }
        
        return result.toString();
    }

    // Generate CAPTCHA image
    private String generateCaptchaImage(String text) throws IOException {
        int width = 150;
        int height = 50;
        
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = image.createGraphics();
        
        // Background
        g2d.setColor(Color.WHITE);
        g2d.fillRect(0, 0, width, height);
        
        // Add noise lines
        Random random = new Random();
        g2d.setColor(Color.LIGHT_GRAY);
        for (int i = 0; i < 10; i++) {
            int x1 = random.nextInt(width);
            int y1 = random.nextInt(height);
            int x2 = random.nextInt(width);
            int y2 = random.nextInt(height);
            g2d.drawLine(x1, y1, x2, y2);
        }
        
        // Draw text
        g2d.setColor(Color.BLACK);
        g2d.setFont(new Font("Arial", Font.BOLD, 24));
        FontMetrics fm = g2d.getFontMetrics();
        int textWidth = fm.stringWidth(text);
        int textHeight = fm.getHeight();
        
        int x = (width - textWidth) / 2;
        int y = (height + textHeight) / 2 - 5;
        
        // Add slight rotation and positioning variations
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            int charX = x + (i * textWidth / text.length());
            int charY = y + random.nextInt(10) - 5;
            
            g2d.drawString(String.valueOf(c), charX, charY);
        }
        
        g2d.dispose();
        
        // Convert to base64
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(image, "png", baos);
        byte[] imageBytes = baos.toByteArray();
        
        return Base64.getEncoder().encodeToString(imageBytes);
    }

    // API để refresh CAPTCHA
    @PostMapping(value = "/refresh-captcha", produces = "application/json")
    @ResponseBody
    public Map<String, String> refreshCaptcha(HttpSession session) {
        Map<String, String> captcha = generateCaptcha();
        session.setAttribute("captcha", captcha.get("text"));
        return captcha;
    }

    // Cập nhật method login để kiểm tra CAPTCHA
    @PostMapping("/login")
    public String login(@RequestParam String username, 
                       @RequestParam String password,
                       @RequestParam String captcha,
                       HttpSession session, 
                       Model model) {
        // Kiểm tra CAPTCHA
        String sessionCaptcha = (String) session.getAttribute("captcha");
        if (sessionCaptcha == null || !sessionCaptcha.equalsIgnoreCase(captcha)) {
            Map<String, String> newCaptcha = generateCaptcha();
            model.addAttribute("captchaImage", newCaptcha.get("image"));
            session.setAttribute("captcha", newCaptcha.get("text"));
            model.addAttribute("captchaError", "Mã xác thực không chính xác!");
            return "login";
        }

        User user = userService.findByUsername(username);

        if (user != null && user.getPassword().equals(password)) {
            session.setAttribute("user", user);
            session.setAttribute("servers", user.getServers());
            
            // Ghi nhận hoạt động đăng nhập
            activityLogService.logActivity(user, "LOGIN", "Người dùng đã đăng nhập từ trình duyệt");
            
            // Xóa CAPTCHA cũ sau khi đăng nhập thành công
            session.removeAttribute("captcha");
            session.removeAttribute("captchaImage");
            
            return "redirect:/";
        } else {
            // Ghi nhận đăng nhập thất bại
            if (user != null) {
                activityLogService.logActivity(user, "LOGIN_FAILED", "Cố gắng đăng nhập thất bại - mật khẩu sai", null, "FAILED");
            }
            
            model.addAttribute("error", "Tên đăng nhập hoặc mật khẩu không đúng!");
            return "login";
        }
    }

    // Hiển thị trang chính
        @GetMapping("/")
        public String showHomePage(Model model, HttpSession session) {
            if (session.getAttribute("user") != null) {
                User currentUser = (User) session.getAttribute("user");
                List<Server> userServers = serverRepository.findByUser(currentUser);
                
                // Tính toán dữ liệu thực tế
                int totalServers = userServers.size();
                int activeServers = (int) userServers.stream()
                    .filter(server -> server.isOnline()).count();
                
                Map<String, Object> stats = new HashMap<>();
                stats.put("totalServers", totalServers);
                stats.put("activeServers", activeServers);
                stats.put("uptime", "99.9%");
                stats.put("performance", totalServers > 0 ? (activeServers * 100 / totalServers) : 0);
                stats.put("cpuLoad", 45); // Có thể cập nhật từ hệ thống monitoring thực
                model.addAttribute("statistics", stats);
                model.addAttribute("userServersCount", totalServers);
            }
            return "index";
        }
    }
