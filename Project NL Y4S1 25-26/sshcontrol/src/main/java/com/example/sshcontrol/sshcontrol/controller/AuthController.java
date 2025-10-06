package com.example.sshcontrol.sshcontrol.controller;

import com.example.sshcontrol.model.User;
import com.example.sshcontrol.model.Server;
import com.example.sshcontrol.sshcontrol.service.UserService;
import com.example.sshcontrol.sshcontrol.service.ServerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpSession;
import java.util.List;
import java.net.InetAddress;
import java.net.Socket;
import java.net.InetSocketAddress;
import java.io.IOException;
import java.util.Random;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import javax.imageio.ImageIO;
import java.util.Base64;

@Controller
public class AuthController {

    @Autowired
    private UserService userService;
    
    @Autowired
    private ServerService serverService;

    // Trang chủ sử dụng index.html
    @GetMapping("/")
    public String home(HttpSession session, Model model) {
        User user = (User) session.getAttribute("user");
        if (user != null) {
            // Refresh user data và servers
            user = userService.findByUsername(user.getUsername());
            session.setAttribute("user", user);
            session.setAttribute("servers", user.getServers());
        }
        return "index"; // Trả về index.html template
    }

    // Hiển thị trang đăng nhập
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
            
            return "redirect:/"; // Redirect về trang chủ thay vì dashboard
        } else {
            model.addAttribute("error", "Tên đăng nhập hoặc mật khẩu không đúng!");
            return "login";
        }
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
        userService.save(newUser);

        // Xóa CAPTCHA khỏi session
        session.removeAttribute("captcha");
        session.removeAttribute("captchaImage");

        model.addAttribute("success", "Đăng ký thành công! Hãy đăng nhập.");
        return "login";
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

    // Hiển thị dashboard
    @GetMapping("/dashboard")
    public String showDashboard(HttpSession session, Model model) {
        User user = (User) session.getAttribute("user");
        if (user == null) {
            return "redirect:/login";
        }
        // Refresh user data
        user = userService.findByUsername(user.getUsername());
        session.setAttribute("user", user);
        session.setAttribute("servers", user.getServers());
        return "dashboard";
    }

    // Thêm server
    @PostMapping("/add-server")
    public String addServer(
            @RequestParam String name,
            @RequestParam String ip,
            @RequestParam String username,
            @RequestParam String password,
            HttpSession session,
            Model model) {

        User user = (User) session.getAttribute("user");
        if (user == null) {
            return "redirect:/login";
        }

        // Refresh user data from database
        user = userService.findByUsername(user.getUsername());

        // Create new server
        Server newServer = new Server(name, ip, username, password, user);
        serverService.save(newServer);

        // Update session with fresh data
        user = userService.findByUsername(user.getUsername());
        session.setAttribute("user", user);
        session.setAttribute("servers", user.getServers());

        return "redirect:/dashboard";
    }

    // Xóa server
    @PostMapping("/remove-server")
    public String removeServer(@RequestParam String ip, HttpSession session) {
        User user = (User) session.getAttribute("user");
        if (user == null) {
            return "redirect:/login";
        }

        // Refresh user data from database
        user = userService.findByUsername(user.getUsername());

        // Find and remove server
        user.getServers().removeIf(server -> server.getIp().equals(ip));
        userService.save(user);

        // Update session
        user = userService.findByUsername(user.getUsername());
        session.setAttribute("user", user);
        session.setAttribute("servers", user.getServers());

        return "redirect:/dashboard";
    }

    // Logout
    @PostMapping("/logout")
    public String logout(HttpSession session) {
        session.invalidate();
        return "redirect:/";
    }

    // API get server
    @GetMapping("/get-server")
    @ResponseBody
    public Server getServer(@RequestParam String ip, HttpSession session) {
        User user = (User) session.getAttribute("user");
        if (user == null) {
            return null;
        }

        user = userService.findByUsername(user.getUsername());

        return user.getServers().stream()
                .filter(server -> server.getIp().equals(ip))
                .findFirst()
                .orElse(null);
    }

    // Check server status
    @PostMapping("/check-server-status")
    @ResponseBody
    public boolean checkServerStatus(@RequestParam String ip) {
        try {
            InetAddress address = InetAddress.getByName(ip);
            boolean reachable = address.isReachable(3000);
            
            if (!reachable) {
                try (Socket socket = new Socket()) {
                    socket.connect(new InetSocketAddress(ip, 22), 3000);
                    return true;
                } catch (IOException e) {
                    return false;
                }
            }
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}