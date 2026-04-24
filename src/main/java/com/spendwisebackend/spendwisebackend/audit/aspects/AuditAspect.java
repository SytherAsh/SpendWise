package com.spendwisebackend.spendwisebackend.audit.aspects;

import com.spendwisebackend.spendwisebackend.audit.annotations.Auditable;
import com.spendwisebackend.spendwisebackend.audit.models.AuditLog;
import com.spendwisebackend.spendwisebackend.audit.repositories.AuditLogRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.LocalDateTime;

@Aspect
@Component
@RequiredArgsConstructor
public class AuditAspect {

    private final AuditLogRepository auditLogRepository;

    @AfterReturning("@annotation(auditable)")
    public void logAuditActivity(JoinPoint joinPoint, Auditable auditable) {
        String userEmail = "Anonymous";
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated()
                && !authentication.getPrincipal().equals("anonymousUser")) {
            userEmail = authentication.getName();
        }

        HttpServletRequest request = null;
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder
                .currentRequestAttributes();
        request = attributes.getRequest();

        String ipAddress = "Unknown";
        String details = "No Request Info";

        if (request != null) {
            ipAddress = request.getHeader("X-Forwarded-For");
            if (ipAddress == null) {
                ipAddress = request.getRemoteAddr();
            }
            details = request.getMethod() + " " + request.getRequestURI();
        }

        String methodName = joinPoint.getSignature().getName();
        String className = joinPoint.getTarget().getClass().getSimpleName();

        AuditLog log = AuditLog.builder()
                .userEmail(userEmail)
                .action(auditable.action())
                .resource(className + "." + methodName)
                .ipAddress(ipAddress)
                .details(details)
                .timestamp(LocalDateTime.now())
                .build();

        auditLogRepository.save(log);
    }
}
