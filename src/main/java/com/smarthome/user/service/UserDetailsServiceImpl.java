package com.smarthome.user.service;

import com.smarthome.user.repository.UserRepository;
import com.smarthome.user.entity.UserEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class UserDetailsServiceImpl implements UserDetailsService {

    private final UserRepository userRepository;

    @Autowired
    public UserDetailsServiceImpl(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /**
     * 根据用户名加载用户信息，供 Spring Security 使用
     *
     * @param username 用户名
     * @return UserDetails 用户详细信息对象
     * @throws UsernameNotFoundException 如果用户不存在
     */
    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        // 根据用户名从数据库中查找用户实体
        UserEntity user = userRepository.findByUsername(username);

        // 如果找不到用户，抛出 UsernameNotFoundException
        if (user == null) {
            throw new UsernameNotFoundException("用户不存在: " + username);
        }
        return User.withUsername(user.getUsername())
                .password(user.getPassword())
                .authorities("ROLE_USER") // 设置用户权限
                .accountExpired(false)
                .accountLocked(false)
                .credentialsExpired(false)
                .disabled(false)
                .build();
    }
}