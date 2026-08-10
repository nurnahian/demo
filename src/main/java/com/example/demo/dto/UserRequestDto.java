package com.example.demo.dto;

import lombok.*;

/**
 * DTO (Data Transfer Object) representing the request model for creating or updateing a user
 * UserRequestDto
 */

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserRequestDto {
    private String name;             // Name of the user.
    private String designation;      // Designation of the user.
    private String contactNumber;    // Contact number of the user.
    private String email;            // Email address of the user.
    private String password;         // Password for the user.
    private String reseatPassword;   // New password for resetting the user's password.
    private String role;             // Role of the user (e.g., ADMIN, STAFF).

}
