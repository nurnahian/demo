package com.example.demo.service.Impl;

import java.util.List;


import com.example.demo.dto.requestDto.CreateUserRequest;
import com.example.demo.dto.responseDto.UserResponse;
import com.example.demo.entity.UsersEntity;
import com.example.demo.repository.UserRepository;
import com.example.demo.utils.Role;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import com.example.demo.service.UserService;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;

    @Override
    @Transactional
    public UserResponse createUser(CreateUserRequest request) {

        UsersEntity user = UsersEntity.builder()
                .name(request.name())
                .email(request.email())
                .password(request.password())
                .phone(request.phone())
                .role(Role.USER)
                .enabled(request.enabled())
                .build();

        UsersEntity savedUser = userRepository.save(user);

        return new UserResponse(
                savedUser.getId(),
                savedUser.getName(),
                savedUser.getEmail(),
                savedUser.getPhone(),
                savedUser.getRole().name(),
                savedUser.isEnabled()
        );
    }
    @Override
    public List<UserResponse> getAllUsers() {

        return userRepository.findAll()
                .stream()
                .map(user -> new UserResponse(
                        user.getId(),
                        user.getName(),
                        user.getEmail(),
                        user.getPhone(),
                        user.getRole().name(),
                        user.isEnabled()
                ))
                .toList();
    }

}
