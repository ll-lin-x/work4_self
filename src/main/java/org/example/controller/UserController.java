package org.example.controller;

import org.example.model.domin.LoginUserCache;
import org.example.model.normal.Result;
import org.example.model.pojo.User;
import org.example.service.Impl.UserServiceImpl;

import org.example.model.vo.UserVO;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;


@RestController
public class UserController {
    @Autowired
    private UserServiceImpl userServiceImpl;



    @PostMapping("/user/register")
    public Result register(User user) {
        return userServiceImpl.register(user);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/user/info")
    public Result info(@AuthenticationPrincipal LoginUserCache loginUserCache) {
        Long id = loginUserCache.getUser().getId();
//        Long id = Long.parseLong(idString);
        User user = userServiceImpl.userInfo(id);
        LocalDateTime createdAt = Instant.ofEpochMilli(user.getCreatedAt()).atZone(ZoneId.systemDefault()).toLocalDateTime();
        LocalDateTime deletedAt = Instant.ofEpochMilli(user.getDeletedAt()).atZone(ZoneId.systemDefault()).toLocalDateTime();
        LocalDateTime updatedAt = Instant.ofEpochMilli(user.getUpdatedAt()).atZone(ZoneId.systemDefault()).toLocalDateTime();
        UserVO userVO = new UserVO();
        BeanUtils.copyProperties(user, userVO);
        userVO.setCreatedAt(createdAt);
        userVO.setDeletedAt(deletedAt);
        userVO.setUpdatedAt(updatedAt);
        return Result.success(userVO);
    }
    @PutMapping("/user/avatar/upload")
    public Result uploadImage(MultipartFile file, @AuthenticationPrincipal LoginUserCache loginUserCache) {
        User user = userServiceImpl.uploadImage(file,loginUserCache.getUser().getId());
        LocalDateTime createdAt = Instant.ofEpochMilli(user.getCreatedAt()).atZone(ZoneId.systemDefault()).toLocalDateTime();
        LocalDateTime deletedAt = Instant.ofEpochMilli(user.getDeletedAt()).atZone(ZoneId.systemDefault()).toLocalDateTime();
        LocalDateTime updatedAt = Instant.ofEpochMilli(user.getUpdatedAt()).atZone(ZoneId.systemDefault()).toLocalDateTime();
        UserVO userVO = new UserVO();
        BeanUtils.copyProperties(user, userVO);
        userVO.setCreatedAt(createdAt);
        userVO.setDeletedAt(deletedAt);
        userVO.setUpdatedAt(updatedAt);
        return Result.success(userVO);
    }
}
