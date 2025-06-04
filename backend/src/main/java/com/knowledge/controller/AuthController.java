package com.knowledge.controller;

import com.knowledge.annotation.Log;
import com.knowledge.dto.AuthResponseDto;
import com.knowledge.dto.UserLoginDto;
import com.knowledge.dto.UserRegistrationDto;
import com.knowledge.entity.User;
import com.knowledge.service.UserService;
import com.knowledge.util.JwtUtil;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "*", maxAge = 3600)
public class AuthController {

    private static final Logger logger = LoggerFactory.getLogger(AuthController.class);

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private UserService userService;

    @Autowired
    private JwtUtil jwtUtil;

    @PostMapping("/register")
    @Log(value = "用户注册", module = "用户认证", description = "新用户注册账号", logResult = false)
    public ResponseEntity<?> registerUser(@Valid @RequestBody UserRegistrationDto registrationDto) {
        // 检查用户名是否已存在
        if (userService.existsByUsername(registrationDto.getUsername())) {
            return ResponseEntity
                    .badRequest()
                    .body(Map.of("message", "用户名已被使用"));
        }

        // 检查邮箱是否已存在
        if (userService.existsByEmail(registrationDto.getEmail())) {
            return ResponseEntity
                    .badRequest()
                    .body(Map.of("message", "邮箱已被使用"));
        }

        // 创建新用户
        User user = userService.registerUser(registrationDto);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(Map.of("message", "用户注册成功"));
    }

    @PostMapping("/login")
    @Log(value = "用户登录", module = "用户认证", description = "用户登录系统", logResult = false)
    public ResponseEntity<?> authenticateUser(@Valid @RequestBody UserLoginDto loginDto) {
        try {
            logger.debug("尝试登录用户: {}", loginDto.getUsername());
            
            // 检查用户是否存在
            if (!userService.existsByUsername(loginDto.getUsername())) {
                logger.warn("用户不存在: {}", loginDto.getUsername());
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("message", "用户名或密码错误"));
            }
            
            logger.debug("用户存在，开始认证...");
            
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(loginDto.getUsername(), loginDto.getPassword()));

            SecurityContextHolder.getContext().setAuthentication(authentication);
            
            User user = userService.findByUsername(loginDto.getUsername());
            String jwt = jwtUtil.generateJwtToken(authentication, user.getId());
            
            AuthResponseDto responseDto = new AuthResponseDto(
                    jwt,
                    user.getId(),
                    user.getUsername(),
                    user.getEmail()
            );
            
            logger.info("用户登录成功: {}", loginDto.getUsername());
            return ResponseEntity.ok(responseDto);
            
        } catch (AuthenticationException e) {
            logger.error("认证失败 - 用户: {}, 错误: {}", loginDto.getUsername(), e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "用户名或密码错误"));
        } catch (Exception e) {
            logger.error("登录过程中发生异常 - 用户: {}, 异常: {}", loginDto.getUsername(), e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "登录失败，请稍后重试"));
        }
    }

    @PostMapping("/logout")
    @Log(value = "用户登出", module = "用户认证", description = "用户退出登录")
    public ResponseEntity<?> logout() {
        // 清除安全上下文
        SecurityContextHolder.clearContext();
        return ResponseEntity.ok(Map.of("message", "登出成功"));
    }

    @PostMapping("/refresh")
    @Log(value = "刷新令牌", module = "用户认证", description = "刷新JWT令牌")
    public ResponseEntity<?> refreshToken() {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication == null || !authentication.isAuthenticated()) {
                return ResponseEntity.status(401)
                        .body(Map.of("message", "未登录"));
            }
            
            String username = authentication.getName();
            User user = userService.findByUsername(username);
            String jwt = jwtUtil.generateJwtToken(authentication, user.getId());
            return ResponseEntity.ok(Map.of("token", jwt));
        } catch (Exception e) {
            return ResponseEntity.status(500)
                    .body(Map.of("message", "刷新令牌失败"));
        }
    }
} 