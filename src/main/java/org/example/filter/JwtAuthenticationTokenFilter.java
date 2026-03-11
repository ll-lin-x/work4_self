package org.example.filter;

import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.example.model.pojo.LoginUserDetails;
import org.example.model.pojo.User;
import org.example.utils.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Objects;

@Component
public class JwtAuthenticationTokenFilter extends OncePerRequestFilter {



    @Autowired
    private RedisTemplate redisTemplate;


    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        // 获取token
        String token = request.getHeader("Access-Token");
        if(!StringUtils.hasText(token)){
            // 没有token直接放行   后面的过滤器可以判断是不是认证过的
            filterChain.doFilter(request,response);
            return;
        }
        // 解析token中携带的userid
        String userId;
        try{
            Claims claims = JwtUtil.parseJWT(token);
            userId = claims.getSubject();
        }catch (Exception e){
            throw new RuntimeException("Invalid token");
        }
        // 根据userid查询redis
        String redisKey = "login:" + userId;
        User user =  (User)redisTemplate.opsForValue().get(redisKey);
//        LoginUserDetails loginUser = redisCache.getCacheObject(redisKey);

        if (Objects.isNull(user)) {
            // 说明 Redis 里的缓存过期了，或者用户已在别处下线
            throw new RuntimeException("用户未登录");
        }
        redisTemplate.expire(redisKey,60,java.util.concurrent.TimeUnit.MINUTES);
//        redisCache.expire(redisKey, 30, java.util.concurrent.TimeUnit.MINUTES);
        // 存入SecurityContextHolder
        // TODO 权限信息是第三个参数，现在没用上所以设置为null  第一个参数不应该是token，应该是根据redis查找到的LoginUser
        // 这里需要三个参数，这样才能将认证设置为true
        UsernamePasswordAuthenticationToken authenticationToken = new UsernamePasswordAuthenticationToken(user,null,null);
        SecurityContextHolder.getContext().setAuthentication(authenticationToken);
        // 放行
        filterChain.doFilter(request,response);
    }
}
