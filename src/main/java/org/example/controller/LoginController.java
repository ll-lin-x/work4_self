package org.example.controller;

import org.example.model.dto.UserLoginDTO;
import org.example.model.pojo.User;
import org.example.model.pojo.Result;
import org.example.service.Impl.UserServiceImpl;
import org.example.service.LoginService;
import org.example.model.vo.UserVO;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class LoginController {

    @Autowired
    private LoginService loginService;

    @Autowired
    private UserServiceImpl userServiceImpl;

    @PostMapping("/user/login")
    public Result login(UserLoginDTO user) {
        User userAllInfo = loginService.login(user);
        UserVO userVO = new UserVO();
        BeanUtils.copyProperties(userAllInfo, userVO);
        return Result.success(userVO);
    }


}
