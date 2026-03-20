package org.example;

import org.example.mapper.UserMapper;
import org.example.model.pojo.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;


import java.time.LocalDateTime;
import java.util.List;

@SpringBootTest
class Work4SelfApplicationTests {

    @Autowired
    private UserMapper userMapper;

    @Test
    void contextLoads() {
    }

    @Test
    public void insertUser() {
        User user = new User(123L,"admin","1234", null, System.currentTimeMillis(),System.currentTimeMillis(),null,"ADMIN");
        userMapper.insert(user);
    }

    @Test
    public void selectUser(){
        List<User> userList = userMapper.selectList(null);
        System.out.println(userList);
    }


    @Test
    public void passwordEncoderTest(){
        // $2a$10$CDejmtHMTr3H43rFwalRYuUO.37aNrAsrkvt/0QWuAo8oHm2zVOYi
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        String password = encoder.encode("123456");
        System.out.println(password);
        System.out.println(encoder.matches("123456", password));
    }
}
