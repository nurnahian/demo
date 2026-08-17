package com.example.demo.controllers;

import com.example.demo.dto.commonDto.ApiResponse;
import com.example.demo.dto.requestDto.CreateUserRequest;
import com.example.demo.dto.responseDto.UserResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.example.demo.service.UserService;

import lombok.RequiredArgsConstructor;

import java.util.List;

@RestController
@RequestMapping("/api/v1/user")
@RequiredArgsConstructor
public class UserController{

    private final UserService userService;

    @PostMapping("/createUser")
    public ResponseEntity<ApiResponse<UserResponse>> createUser(@Valid @RequestBody CreateUserRequest request) {

        UserResponse response = userService.createUser(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(
                        ApiResponse.success(
                                "User created successfully",
                                response
                        )
                );
    }

    @GetMapping("getAllUser")
    public ResponseEntity<ApiResponse<List<UserResponse>>> getAllUsers() {

        List<UserResponse> users = userService.getAllUsers();

        return ResponseEntity.ok(ApiResponse.success("Users fetched successfully",users));
    }
}