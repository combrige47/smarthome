package com.smarthome.user.entity;

import lombok.Setter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import java.util.Collection;
import java.util.Collections;

@Entity
public class UserEntity implements UserDetails { // 实现 UserDetails 接口
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Setter
    private String username;
    @Setter
    private String password;
    // 可扩展字段：如账号是否启用、是否过期等（根据需求添加）
    private boolean enabled = true;

    // 实现 UserDetails 接口方法，提供用户权限、状态等信息
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        // 这里简单返回一个默认权限，实际可从数据库查询用户角色
        return Collections.singleton(new SimpleGrantedAuthority("ROLE_USER"));
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String getUsername() {
        return username;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true; // 根据需求实现，如检查账号有效期
    }

    @Override
    public boolean isAccountNonLocked() {
        return true; // 根据需求实现，如检查账号是否锁定
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true; // 根据需求实现，如检查密码有效期
    }

    @Override
    public boolean isEnabled() {
        return enabled; // 关联实体字段，控制账号是否启用
    }

}