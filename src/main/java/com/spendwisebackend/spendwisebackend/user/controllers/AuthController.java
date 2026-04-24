package com.spendwisebackend.spendwisebackend.user.controllers;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.Map;

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
import com.spendwisebackend.spendwisebackend.user.services.CaptchaService;
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
    private final CaptchaService captchaService;

    @Value("${SAME_SITE:Lax}")
    private String sameSite;

    @PostMapping("/login")
    public ResponseEntity<CustomUserDetails> login(@Valid @RequestBody LoginRequset data,
            HttpServletResponse response, HttpServletRequest request) {
        String userInput = data.getCaptcha();
        String sessionCaptcha = (String) request.getSession().getAttribute("captcha");

        if (sessionCaptcha == null || !sessionCaptcha.equalsIgnoreCase(userInput)) {
            return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).build();
        }

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
    }

    @PostMapping("/register")
    public ResponseEntity<UserDTO> register(@Valid @RequestBody UserDTO data, HttpServletResponse response) {
        data = userServices.saveUser(data);
        if (data.getId() != null) {
            String token = jwtService.generateToken(new CustomUserDetails(data.getId(),
                    data.getEmail(), "", data.getActive(), data.getRole(), data.getName(), data.getEmail()));

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
        String captchaText = captchaService.generateRandomText(5);
        request.getSession().setAttribute("captcha", captchaText);

        BufferedImage img = captchaService.createCaptchaImage(captchaText);

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
