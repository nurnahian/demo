package com.example.demo.service;

import com.example.demo.dto.requestDto.CreateUserRequest;
import com.example.demo.dto.responseDto.UserResponse;

import java.util.List;


public interface UserService {
    UserResponse createUser(CreateUserRequest request);

    List<UserResponse> getAllUsers();
}
