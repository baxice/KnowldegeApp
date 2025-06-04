package com.knowledge.service;

import com.knowledge.dto.UserRegistrationDto;
import com.knowledge.entity.User;

public interface UserService {
    
    User registerUser(UserRegistrationDto registrationDto);
    
    boolean existsByUsername(String username);
    
    boolean existsByEmail(String email);
    
    User findByUsername(String username);
    
    User updateUser(User user);
    
    boolean validatePassword(String rawPassword, String encodedPassword);
    
    User changePassword(String username, String oldPassword, String newPassword);
} 