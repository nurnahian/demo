package com.example.demo.service.Impl;

import java.util.HashMap;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.example.demo.service.UserService;

@Service
public class UserServiceImpl implements UserService {
     public Map<String, Object> register() {
        Map<String, Object> response = new HashMap<>();
        response.put("message", "Successful");
        response.put("name", "John Doe");
        response.put("email", "john@example.com");

    return response;
    }
    
}
