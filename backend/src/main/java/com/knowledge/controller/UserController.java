package com.knowledge.controller;

import com.knowledge.annotation.Log;
import com.knowledge.entity.User;
import com.knowledge.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/users")
@CrossOrigin(origins = "*", maxAge = 3600)
public class UserController {

    @Autowired
    private UserService userService;

    @GetMapping("/profile")
    @Log(value = "获取用户信息", module = "用户管理", description = "获取当前用户的详细信息")
    public ResponseEntity<?> getCurrentUser() {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication == null || !authentication.isAuthenticated()) {
                return ResponseEntity.status(401)
                        .body(Map.of("message", "用户未登录"));
            }
            
            String username = authentication.getName();
            User user = userService.findByUsername(username);
            
            if (user == null) {
                return ResponseEntity.status(404)
                        .body(Map.of("message", "用户不存在"));
            }
            
            // 返回用户信息（不包含密码）
            Map<String, Object> userInfo = new HashMap<>();
            userInfo.put("id", user.getId());
            userInfo.put("username", user.getUsername());
            userInfo.put("email", user.getEmail());
            userInfo.put("createdAt", user.getCreatedAt());
            
            return ResponseEntity.ok(userInfo);
        } catch (Exception e) {
            return ResponseEntity.status(500)
                    .body(Map.of("message", "获取用户信息失败：" + e.getMessage()));
        }
    }

    @PutMapping("/profile")
    @Log(value = "更新用户信息", module = "用户管理", description = "更新当前用户的个人信息")
    public ResponseEntity<?> updateProfile(@RequestBody Map<String, String> updateData) {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication == null || !authentication.isAuthenticated()) {
                return ResponseEntity.status(401)
                        .body(Map.of("message", "用户未登录"));
            }
            
            String username = authentication.getName();
            User user = userService.findByUsername(username);
            
            if (user == null) {
                return ResponseEntity.status(404)
                        .body(Map.of("message", "用户不存在"));
            }
            
            // 更新邮箱（如果提供）
            String newEmail = updateData.get("email");
            if (newEmail != null && !newEmail.equals(user.getEmail())) {
                // 检查邮箱是否已被使用
                if (userService.existsByEmail(newEmail)) {
                    return ResponseEntity.badRequest()
                            .body(Map.of("message", "邮箱已被使用"));
                }
                user.setEmail(newEmail);
            }
            
            // 这里可以添加更多字段的更新逻辑
            // 例如：昵称、头像等
            
            // 保存更新后的用户信息
            userService.updateUser(user);
            
            return ResponseEntity.ok(Map.of("message", "用户信息更新成功"));
        } catch (Exception e) {
            return ResponseEntity.status(500)
                    .body(Map.of("message", "更新用户信息失败：" + e.getMessage()));
        }
    }

    @PostMapping("/change-password")
    @Log(value = "修改密码", module = "用户管理", description = "用户修改登录密码")
    public ResponseEntity<?> changePassword(@RequestBody Map<String, String> passwordData) {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication == null || !authentication.isAuthenticated()) {
                return ResponseEntity.status(401)
                        .body(Map.of("message", "用户未登录"));
            }
            
            String username = authentication.getName();
            String oldPassword = passwordData.get("oldPassword");
            String newPassword = passwordData.get("newPassword");
            
            if (oldPassword == null || newPassword == null) {
                return ResponseEntity.badRequest()
                        .body(Map.of("message", "旧密码和新密码不能为空"));
            }
            
            if (newPassword.length() < 6) {
                return ResponseEntity.badRequest()
                        .body(Map.of("message", "新密码长度不能少于6个字符"));
            }
            
            // 修改密码
            userService.changePassword(username, oldPassword, newPassword);
            
            return ResponseEntity.ok(Map.of("message", "密码修改成功"));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(500)
                    .body(Map.of("message", "修改密码失败：" + e.getMessage()));
        }
    }
} 