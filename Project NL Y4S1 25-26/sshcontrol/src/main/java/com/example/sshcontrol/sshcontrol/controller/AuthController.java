package com.example.sshcontrol.sshcontrol.controller;

import com.example.sshcontrol.model.User;
import com.example.sshcontrol.model.Server;
import com.example.sshcontrol.service.UserService;
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
    
    // Hiển thị form đăng nhập
    @GetMapping("/login")
    public String showLoginPage() {
        return "login";
    }

    // Xử lý đăng nhập
    @PostMapping("/login")
    public String login(@RequestParam String username, @RequestParam String password, 
                       HttpSession session, Model model) {
        User user = userService.findByUsername(username);

        if (user != null && user.getPassword().equals(password)) {
            user = userService.findByUsername(username);
            session.setAttribute("user", user);
            session.setAttribute("servers", user.getServers());
            
            return "redirect:/"; // Redirect về trang chủ
        } else {
            model.addAttribute("error", "Tên đăng nhập hoặc mật khẩu không đúng!");
            return "login";
        }
    }

    // Đăng xuất
    @PostMapping("/logout")
    public String logout(HttpSession session) {
        session.invalidate();
        return "redirect:/";
    }

    // Hiển thị trang đăng ký
    @GetMapping("/register")
    public String showRegisterPage(HttpSession session, Model model) {
        generateCaptcha(session);
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
            generateCaptcha(session); // Tạo CAPTCHA mới
            model.addAttribute("error", "Mã CAPTCHA không đúng!");
            return "register";
        }

        // Validation cơ bản
        if (username == null || username.trim().isEmpty()) {
            generateCaptcha(session);
            model.addAttribute("error", "Tên đăng nhập không được để trống!");
            return "register";
        }
        
        if (fullName == null || fullName.trim().isEmpty()) {
            generateCaptcha(session);
            model.addAttribute("error", "Họ tên không được để trống!");
            return "register";
        }
        
        if (email == null || email.trim().isEmpty()) {
            generateCaptcha(session);
            model.addAttribute("error", "Email không được để trống!");
            return "register";
        }
        
        // Validation email format
        if (!email.matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")) {
            generateCaptcha(session);
            model.addAttribute("error", "Email không đúng định dạng!");
            return "register";
        }
        
        if (password == null || password.length() < 6) {
            generateCaptcha(session);
            model.addAttribute("error", "Mật khẩu phải có ít nhất 6 ký tự!");
            return "register";
        }
        
        if (!password.equals(confirmPassword)) {
            generateCaptcha(session);
            model.addAttribute("error", "Mật khẩu xác nhận không khớp!");
            return "register";
        }

        // Kiểm tra username đã tồn tại
        if (userService.existsByUsername(username)) {
            generateCaptcha(session);
            model.addAttribute("error", "Tên đăng nhập đã tồn tại!");
            return "register";
        }

        // Kiểm tra email đã tồn tại
        if (userService.existsByEmail(email)) {
            generateCaptcha(session);
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
            .filter(s -> s.getIp().equals(ip))
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

        // Refresh user from database
        user = userService.findByUsername(user.getUsername());
        
        // Kiểm tra trùng IP + SSH Username + Password trong database
        boolean exists = serverRepository.findAll().stream()
            .anyMatch(s -> s.getIp().equalsIgnoreCase(server.getIp()) &&
                         s.getSshUsername().equalsIgnoreCase(server.getSshUsername()) &&
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
        User sessionUser = (User) session.getAttribute("user");
        if (sessionUser == null) {
            return "redirect:/login";
        }

        User currentUser = userService.findByUsername(sessionUser.getUsername());
        if (currentUser == null) {
            return "redirect:/login";
        }

        List<Server> userServers = serverRepository.findByUser(currentUser);
        Server serverToDelete = userServers.stream()
            .filter(s -> s.getIp().equals(ip))
            .findFirst()
            .orElse(null);

        if (serverToDelete != null) {
            serverRepository.delete(serverToDelete);
            
            // Refresh user data in session
            currentUser = userService.findByUsername(currentUser.getUsername());
            session.setAttribute("user", currentUser);
            
            redirectAttributes.addFlashAttribute("message", "Xóa máy chủ thành công!");
        } else {
            redirectAttributes.addFlashAttribute("error", "Không tìm thấy máy chủ để xóa!");
        }

        return "redirect:/server-list";
    }

    // Hiển thị trang dashboard
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

        model.addAttribute("user", currentUser);
        model.addAttribute("servers", servers);
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
    private void generateCaptcha(HttpSession session) {
        String captchaText = generateRandomString(5);
        session.setAttribute("captcha", captchaText);
        
        try {
            String captchaImage = generateCaptchaImage(captchaText);
            session.setAttribute("captchaImage", captchaImage);
        } catch (IOException e) {
            e.printStackTrace();
        }
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
    @PostMapping("/refresh-captcha")
    @ResponseBody
    public String refreshCaptcha(HttpSession session) {
        generateCaptcha(session);
        return (String) session.getAttribute("captchaImage");
    }
}
