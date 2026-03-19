package org.example.controller;

import org.example.model.domin.LoginUserCache;
import org.example.model.dto.FollowingListDTO;
import org.example.model.normal.Result;
import org.example.model.pojo.User;
import org.example.model.vo.FollowingListVO;
import org.example.service.RelationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;

@RestController
public class RelationController {
    @Autowired
    private RelationService relationService;

    @PostMapping("/relation/action")
    public Result relationAction(@RequestParam("to_user_id") String toUserId, @RequestParam("action_type") Integer actionType,
                                 @AuthenticationPrincipal LoginUserCache loginUserCache) {
        Long userId = Long.parseLong(toUserId);
        if(userId.equals(loginUserCache.getUser().getId())) throw new IllegalArgumentException("the to_user_id is the same as your user_id");
        relationService.relationAction(toUserId,actionType,loginUserCache.getUser().getId());
        return Result.success();
    }
    
    @GetMapping("/following/list")
    public Result followingList(FollowingListDTO followingListDTO){
        if(!StringUtils.hasText(followingListDTO.getUser_id())) throw new IllegalArgumentException("the user_id is null");
        List<FollowingListVO> followingList = relationService.followingList(followingListDTO);
        HashMap<String,Object> map  = new HashMap<>();
        map.put("items",followingList);
        map.put("total",followingList.size());
        return Result.success(map);
    }

    @GetMapping("/follower/list")
    public Result followerList(FollowingListDTO followingListDTO){
        if(!StringUtils.hasText(followingListDTO.getUser_id())) throw new IllegalArgumentException("the user_id is null");
        List<FollowingListVO> followerList = relationService.followerList(followingListDTO);
        HashMap<String,Object> map  = new HashMap<>();
        map.put("items",followerList);
        map.put("total",followerList.size());
        return Result.success(map);
    }
    @GetMapping("/friends/list")
    public Result friendsList(FollowingListDTO followingListDTO,@AuthenticationPrincipal LoginUserCache loginUserCache) {
        List<FollowingListVO> friendsList = relationService.friendsList(followingListDTO,loginUserCache.getUser().getId());
        HashMap<String,Object> map  = new HashMap<>();
        map.put("items",friendsList);
        map.put("total",friendsList.size());
        return Result.success(map);
    }

}
