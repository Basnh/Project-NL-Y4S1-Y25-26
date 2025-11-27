package com.example.sshcontrol.aspect;

import com.example.sshcontrol.annotation.RequireAdmin;
import com.example.sshcontrol.model.User;
import jakarta.servlet.http.HttpSession;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Aspect
@Component
public class AdminCheckAspect {
    
    @Around("@annotation(requireAdmin)")
    public Object checkAdmin(ProceedingJoinPoint joinPoint, RequireAdmin requireAdmin) throws Throwable {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes == null) {
            throw new IllegalAccessException(requireAdmin.value());
        }
        
        HttpSession session = attributes.getRequest().getSession();
        User user = (User) session.getAttribute("user");
        
        if (user == null || !user.isAdmin()) {
            throw new IllegalAccessException(requireAdmin.value());
        }
        
        return joinPoint.proceed();
    }
}
