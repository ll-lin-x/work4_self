package org.example.controller;

import org.example.model.dto.UserLoginDTO;
import org.example.model.pojo.User;
import org.example.model.normal.Result;
import org.example.service.Impl.UserServiceImpl;
import org.example.service.LoginService;
import org.example.model.vo.UserVO;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

@RestController
public class LoginController {

    @Autowired
    private LoginService loginService;

    @Autowired
    private UserServiceImpl userServiceImpl;

    @PostMapping("/user/login")
    public Result login(UserLoginDTO user) {
        User userAllInfo = loginService.login(user);
        LocalDateTime createdAt = Instant.ofEpochMilli(userAllInfo.getCreatedAt()).atZone(ZoneId.systemDefault()).toLocalDateTime();
        LocalDateTime deletedAt = Instant.ofEpochMilli(userAllInfo.getDeletedAt()).atZone(ZoneId.systemDefault()).toLocalDateTime();
        LocalDateTime updatedAt = Instant.ofEpochMilli(userAllInfo.getUpdatedAt()).atZone(ZoneId.systemDefault()).toLocalDateTime();
        UserVO userVO = new UserVO();
        BeanUtils.copyProperties(userAllInfo, userVO);
        userVO.setCreatedAt(createdAt);
        userVO.setDeletedAt(deletedAt);
        userVO.setUpdatedAt(updatedAt);
        return Result.success(userVO);
    }


}
