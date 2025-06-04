package com.knowledge.service;

import com.knowledge.annotation.Log;
import com.knowledge.dto.UserRegistrationDto;
import com.knowledge.entity.User;
import com.knowledge.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class UserServiceImpl implements UserService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Override
    @Log(value = "用户注册", module = "用户管理", description = "创建新用户账号", logResult = false)
    public User registerUser(UserRegistrationDto registrationDto) {
        // 创建新用户实体
        User user = new User();
        user.setUsername(registrationDto.getUsername());
        user.setEmail(registrationDto.getEmail());
        user.setPassword(passwordEncoder.encode(registrationDto.getPassword()));

        // 保存用户
        return userRepository.save(user);
    }

    @Override
    public boolean existsByUsername(String username) {
        return userRepository.existsByUsername(username);
    }

    @Override
    public boolean existsByEmail(String email) {
        return userRepository.existsByEmail(email);
    }

    @Override
    @Log(value = "查找用户", module = "用户管理", description = "根据用户名查找用户信息", logParams = false)
    public User findByUsername(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("用户不存在"));
    }

    @Override
    @Log(value = "更新用户", module = "用户管理", description = "更新用户信息")
    public User updateUser(User user) {
        return userRepository.save(user);
    }

    @Override
    public boolean validatePassword(String rawPassword, String encodedPassword) {
        return passwordEncoder.matches(rawPassword, encodedPassword);
    }

    @Override
    @Log(value = "修改密码", module = "用户管理", description = "用户修改登录密码")
    public User changePassword(String username, String oldPassword, String newPassword) {
        User user = findByUsername(username);
        
        // 验证旧密码
        if (!validatePassword(oldPassword, user.getPassword())) {
            throw new RuntimeException("旧密码不正确");
        }
        
        // 设置新密码
        user.setPassword(passwordEncoder.encode(newPassword));
        
        // 保存更新
        return updateUser(user);
    }
} 