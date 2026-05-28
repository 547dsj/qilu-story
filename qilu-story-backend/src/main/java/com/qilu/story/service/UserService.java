package com.qilu.story.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.qilu.story.entity.User;
import com.qilu.story.exception.BusinessException;
import com.qilu.story.mapper.UserMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

/**
 * 用户 Service
 */
@Slf4j
@Service
public class UserService {

    @Autowired
    private UserMapper userMapper;

    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    /**
     * 用户注册
     */
    public Long register(String username, String password, String nickname) {
        // 检查用户名是否已存在
        QueryWrapper<User> wrapper = new QueryWrapper<>();
        wrapper.eq("username", username);
        User existUser = userMapper.selectOne(wrapper);
        
        if (existUser != null) {
            throw new BusinessException("用户名已存在");
        }

        // 创建新用户
        User user = User.builder()
                .username(username)
                .password(passwordEncoder.encode(password))
                .nickname(nickname)
                .build();

        userMapper.insert(user);
        return user.getId();
    }

    /**
     * 用户登录
     */
    public User login(String username, String password) {
        QueryWrapper<User> wrapper = new QueryWrapper<>();
        wrapper.eq("username", username);
        User user = userMapper.selectOne(wrapper);

        if (user == null) {
            throw new BusinessException("用户不存在");
        }

        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw new BusinessException("密码错误");
        }

        return user;
    }

    /**
     * 根据 ID 获取用户
     */
    public User getUserById(Long userId) {
        return userMapper.selectById(userId);
    }
}
