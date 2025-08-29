package com.smarthome;

import com.smarthome.user.repository.UserRepository;
import org.testng.annotations.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
public class UserTest {

    @Autowired
    private UserRepository userRepository;

    @Test
    public void testConnection() {
        // 测试查询所有用户（若表为空则返回空列表，无异常即连接成功）
        System.out.println("用户数量：" + userRepository.count());
    }
}