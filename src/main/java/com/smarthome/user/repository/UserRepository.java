package com.smarthome.user.repository;

import com.smarthome.user.entity.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<UserEntity, Long> {
    // 根据用户名查询用户（Spring Data JPA 约定命名自动实现）
    UserEntity findByUsername(String username);
}