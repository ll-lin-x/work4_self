package org.example.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.example.model.domin.LoginUserCache;
import org.example.model.normal.RedisKey;
import org.example.model.domin.LoginUserDetails;
import org.example.utils.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collection;
import java.util.Objects;
import java.util.stream.Collectors;

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
        String redisKey = RedisKey.USER_LOGIN + userId;
        // 找到报错的那一行，不要直接强转
        Object cacheObject = redisTemplate.opsForValue().get(redisKey);
        if (Objects.isNull(cacheObject)) {
            // 说明 Redis 里的缓存过期了，或者用户已在别处下线
            throw new RuntimeException("用户未登录");
        }
        // 使用 ObjectMapper 手动转换
        ObjectMapper mapper = new ObjectMapper();
        //  注册 JavaTimeModule，否则 LocalDateTime 又会报错
        mapper.registerModule(new JavaTimeModule());

        LoginUserCache loginUser = mapper.convertValue(cacheObject, LoginUserCache.class);

        Collection<? extends GrantedAuthority> authorities = loginUser.getPermissions()
                .stream()
                .map(SimpleGrantedAuthority::new)
                .collect(Collectors.toList());
//        User user = mapper.convertValue(cacheObject,User.class);

//        redisCache.expire(redisKey, 30, java.util.concurrent.TimeUnit.MINUTES);
        // 存入SecurityContextHolder
        // TODO 权限信息是第三个参数，现在没用上所以设置为null  第一个参数不应该是token，应该是根据redis查找到的LoginUser
        // 这里需要三个参数，这样才能将认证设置为true
        UsernamePasswordAuthenticationToken authenticationToken = new UsernamePasswordAuthenticationToken(loginUser,null,authorities);
        SecurityContextHolder.getContext().setAuthentication(authenticationToken);
        redisTemplate.expire(redisKey,60,java.util.concurrent.TimeUnit.MINUTES);
        // 放行
        filterChain.doFilter(request,response);
    }
}
