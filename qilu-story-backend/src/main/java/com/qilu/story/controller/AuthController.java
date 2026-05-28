package com.qilu.story.controller;

import com.qilu.story.dto.LoginRequest;
import com.qilu.story.dto.LoginResponse;
import com.qilu.story.dto.RegisterRequest;
import com.qilu.story.entity.User;
import com.qilu.story.service.UserService;
import com.qilu.story.utils.ApiResponse;
import com.qilu.story.utils.JwtUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 认证 Controller
 */
@Slf4j
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private UserService userService;

    @Autowired
    private JwtUtil jwtUtil;

    /**
     * 用户注册
     */
    @PostMapping("/register")
    public ApiResponse<Long> register(@RequestBody RegisterRequest request) {
        log.info("用户注册: username={}", request.getUsername());
        Long userId = userService.register(request.getUsername(), request.getPassword(), request.getNickname());
        return ApiResponse.success("注册成功", userId);
    }

    /**
     * 用户登录
     */
    @PostMapping("/login")
    public ApiResponse<LoginResponse> login(@RequestBody LoginRequest request) {
        log.info("用户登录: username={}", request.getUsername());
        User user = userService.login(request.getUsername(), request.getPassword());
        
        // 生成 JWT token
        String token = jwtUtil.generateToken(user.getId(), user.getUsername());
        
        LoginResponse loginResponse = LoginResponse.builder()
                .token(token)
                .userInfo(LoginResponse.UserInfo.builder()
                        .id(user.getId())
                        .username(user.getUsername())
                        .nickname(user.getNickname())
                        .build())
                .build();
        
        return ApiResponse.success("登录成功", loginResponse);
    }
}
