package org.example.service.Impl;

import org.example.model.domin.LoginUserCache;
import org.example.model.dto.UserLoginDTO;
import org.example.mapper.UserMapper;
import org.example.model.normal.RedisKey;
import org.example.model.domin.LoginUserDetails;
import org.example.model.pojo.User;
import org.example.service.LoginService;
import org.example.utils.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

@Service
public class LoginServiceImpl implements LoginService {

    @Autowired
    private AuthenticationManager authenticationManager;
    @Autowired
    private RedisTemplate  redisTemplate;
    @Autowired
    private UserMapper userMapper;

    @Override
    public User login(UserLoginDTO user) {
        // AuthenticationManager authenticate 进行用户认证
        UsernamePasswordAuthenticationToken authenticationToken =  new UsernamePasswordAuthenticationToken(user.getUsername(), user.getPassword());
        Authentication authenticate = authenticationManager.authenticate(authenticationToken);
        // 如果认证没有通过，给出对应提示
        if(Objects.isNull(authenticate)){
            throw new RuntimeException("登录失败");
        }

        LoginUserDetails loginUserDetails = (LoginUserDetails)authenticate.getPrincipal();
        String userId = loginUserDetails.getUser().getId().toString();
        LoginUserCache  loginUserCache = new LoginUserCache(loginUserDetails.getUser(),loginUserDetails.getPermissions());
        String jwt = JwtUtil.createJWT(userId);
        System.out.println("********jwt********:"+jwt);
        // 如果认证通过了，使用user_id生成jwt  jwt存入Result返回
        // 存入用户信息，并设置 30 分钟过期
        redisTemplate.opsForValue().set(RedisKey.USER_LOGIN + userId,loginUserCache,60, TimeUnit.MINUTES);
//        redisCache.setCacheObject("login:" + userId, loginUserDetails, 30, TimeUnit.MINUTES);
        // 把token返回给前端
//        HashMap<Object, Object> hashMap = new HashMap<>();
//        hashMap.put("token", jwt);
//        return Result.success(hashMap);
        return userMapper.selectById(userId);
    }

}
