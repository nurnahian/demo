package com.example.demo.dto.responseDto;

public record UserResponse(
        Long id,
        String name,
        String email,
        String phone,
        String role,
        boolean enabled
) {
}