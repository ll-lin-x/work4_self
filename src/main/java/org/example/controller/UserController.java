package org.example.controller;

import org.example.model.normal.Result;
import org.example.model.pojo.User;
import org.example.service.Impl.UserServiceImpl;

import org.example.model.vo.UserVO;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;


@RestController
public class UserController {
    @Autowired
    private UserServiceImpl userServiceImpl;



    @PostMapping("/user/register")
    public Result register(User user) {
        return userServiceImpl.register(user);
    }

    @GetMapping("/user/info")
    public Result info(@AuthenticationPrincipal User userDetails) {
        Long id = userDetails.getId();
//        Long id = Long.parseLong(idString);
        User user = userServiceImpl.userInfo(id);
        UserVO userVO = new UserVO();
        BeanUtils.copyProperties(user, userVO);
        return Result.success(userVO);
    }
    @PutMapping("/user/avatar/upload")
    public Result uploadImage(MultipartFile file, @AuthenticationPrincipal User userDetails) {
        User user = userServiceImpl.uploadImage(file,userDetails.getId());
        UserVO userVO = new UserVO();
        BeanUtils.copyProperties(user, userVO);
        return Result.success(userVO);
    }
}
