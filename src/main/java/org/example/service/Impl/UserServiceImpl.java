package org.example.service.Impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import org.example.mapper.UserMapper;
import org.example.model.domin.LoginUserDetails;
import org.example.model.normal.Result;
import org.example.model.pojo.User;
import org.example.utils.FileUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Service
public class UserServiceImpl implements UserDetailsService {

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private FileUtil fileUtil;





    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        // 查询用户信息
        LambdaQueryWrapper<User> queryWrapper = new LambdaQueryWrapper<User>();
        queryWrapper.eq(User::getUsername, username);
        User user = userMapper.selectOne(queryWrapper);
        if(Objects.isNull(user)){
            throw new RuntimeException("用户不存在");
        }
        // TODO 查询对应的权限信息
        List<String> list = new ArrayList<>();
        String rolePermission = user.getPermission();
        System.out.println("当前用户权限"+rolePermission);
        list.add(rolePermission);
        // 把数据封装成UserDetails返回
        return new LoginUserDetails(user,list);
    }

    @Transactional
    public Result register(User user) {
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());
        if(Objects.isNull(user.getAvatarUrl())){
            user.setAvatarUrl("");
        }
        if(Objects.isNull(user.getDeletedAt())){
            user.setDeletedAt(LocalDateTime.now().minusYears(100));
        }
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        user.setPermission("ROLE_USER");
        int insertRow = userMapper.insert(user);
        if(insertRow > 0){
            return Result.success("register success");
        }else{
            throw new RuntimeException("register failed");
        }
    }

    public User userInfo(Long id) {
        return userMapper.selectById(id);
    }

    @Transactional
    public User uploadImage(MultipartFile file,Long id) {
        if(!fileUtil.isImage(file)){
            throw new RuntimeException("the upload is not image");
        }
        String path = fileUtil.uploadImageOSS(file);
        userMapper.update(new LambdaUpdateWrapper<User>()
                .eq(User::getId, id) // 匹配 ID
                .set(User::getAvatarUrl, path) // 更新目标字段
        );
        return userMapper.selectById(id);
    }
}
