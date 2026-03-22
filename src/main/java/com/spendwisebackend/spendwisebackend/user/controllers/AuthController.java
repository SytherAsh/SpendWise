package com.spendwisebackend.spendwisebackend.user.controllers;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.Map;
import javax.imageio.ImageIO;
import java.io.IOException;
import java.security.SecureRandom;

import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestMapping;
import com.spendwisebackend.spendwisebackend.config.JWT.JWTService;
import com.spendwisebackend.spendwisebackend.user.CustomUserDetails;
import com.spendwisebackend.spendwisebackend.user.models.dto.LoginRequset;
import com.spendwisebackend.spendwisebackend.user.models.dto.UserDTO;
import com.spendwisebackend.spendwisebackend.user.services.UserServices;

import org.springframework.security.web.csrf.CsrfToken;
import jakarta.servlet.http.Cookie;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {
    private final UserServices userServices;
    private final JWTService jwtService;

    @Value("${SAME_SITE:Lax}")
    private String sameSite;

    @PostMapping("/login")
    public ResponseEntity<CustomUserDetails> login(@Valid @RequestBody LoginRequset data,
            HttpServletResponse response, HttpServletRequest request) {
        String userInput = data.getCaptcha();

        String sessionCaptcha = (String) request.getSession().getAttribute("captcha");

        CsrfToken csrfToken = (CsrfToken) request.getAttribute(CsrfToken.class.getName());
        if (csrfToken != null) {
            response.setHeader(csrfToken.getHeaderName(), csrfToken.getToken());
        }

        if (sessionCaptcha != null && sessionCaptcha.equalsIgnoreCase(userInput)) {
            CustomUserDetails login = userServices.login(data);
            String token = jwtService.generateToken(login);

            ResponseCookie cookie = ResponseCookie.from("token", token)
                    .httpOnly(true)
                    .secure(true)
                    .path("/")
                    .sameSite(sameSite)
                    .maxAge(data.getRememberMe() ? 60 * 60 * 24 * 7 : 60 * 60)
                    .build();
            response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());

            return ResponseEntity.ok(login);
        } else
            return ResponseEntity.status(422).body(null);
    }

    @PostMapping("/register")
    public ResponseEntity<UserDTO> register(@Valid @RequestBody UserDTO data, HttpServletResponse response) {
        data = userServices.saveUser(data);
        if (data.getId() != null) {

            String token = jwtService.generateToken(new CustomUserDetails(data.getId(),
                    data.getEmail(),
                    "",
                    data.getActive(),
                    data.getRole(),
                    data.getName(),
                    data.getEmail()));

            ResponseCookie cookie = ResponseCookie.from("token", token)
                    .httpOnly(true)
                    .secure(true)
                    .path("/")
                    .sameSite(sameSite)
                    .maxAge(60 * 60)
                    .build();
            response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
        }
        data.setPassword(null);
        return ResponseEntity.ok(data);
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletRequest request, HttpServletResponse response) {

        String token = extractTokenFromCookies(request);
        if (token != null) {
            jwtService.addTokenBlacklist(token);
        }
        ResponseCookie cookie = ResponseCookie.from("token", "")
                .httpOnly(true)
                .secure(true)
                .path("/")
                .sameSite(sameSite)
                .maxAge(0)
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());

        return ResponseEntity.ok().build();
    }

    @GetMapping("/api/captcha")
    public void getCaptcha(HttpServletRequest request, HttpServletResponse response) throws IOException {

        CsrfToken csrfToken = (CsrfToken) request.getAttribute(CsrfToken.class.getName());
        if (csrfToken != null) {
            csrfToken.getToken();
        }

        String captchaText = generateRandomText(5);
        request.getSession().setAttribute("captcha", captchaText);

        BufferedImage img = createCaptchaImage(captchaText);

        response.setContentType("image/png");
        ImageIO.write(img, "png", response.getOutputStream());
    }

    @PostMapping("/api/verify-captcha")
    public ResponseEntity<?> verifyCaptcha(@RequestBody Map<String, String> payload, HttpServletRequest request) {
        String userInput = payload.get("captchaInput");
        String sessionCaptcha = (String) request.getSession().getAttribute("captcha");

        if (sessionCaptcha != null && sessionCaptcha.equalsIgnoreCase(userInput)) {
            return ResponseEntity.ok(Map.of("success", true, "message", "success"));
        } else {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("success", false, "message", "not valid"));
        }
    }
    private String generateRandomText(int length) {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
        StringBuilder sb = new StringBuilder();
        SecureRandom random = new SecureRandom();
        for (int i = 0; i < length; i++) {
            sb.append(chars.charAt(random.nextInt(chars.length())));
        }
        return sb.toString();
    }

    private BufferedImage createCaptchaImage(String text) {
        int width = 150;
        int height = 50;
        BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = img.createGraphics();

        g2d.setColor(Color.WHITE);
        g2d.fillRect(0, 0, width, height);

        g2d.setColor(Color.LIGHT_GRAY);
        SecureRandom random = new SecureRandom();
        for (int i = 0; i < 10; i++) {
            int x1 = random.nextInt(width);
            int y1 = random.nextInt(height);
            int x2 = random.nextInt(width);
            int y2 = random.nextInt(height);
            g2d.drawLine(x1, y1, x2, y2);
        }

        g2d.setFont(new Font("Arial", Font.BOLD, 24));
        g2d.setColor(Color.BLUE);
        g2d.drawString(text, 25, 35);

        g2d.dispose();
        return img;
    }

    private String extractTokenFromCookies(HttpServletRequest request) {
        if (request.getCookies() != null) {
            for (Cookie cookie : request.getCookies()) {
                if ("token".equals(cookie.getName())) {
                    return cookie.getValue();
                }
            }
        }
        return null;
    }
}
