package org.example.mapper;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.toolkit.Constants;
import io.lettuce.core.dynamic.annotation.Param;
import org.apache.ibatis.annotations.Mapper;
import org.example.model.pojo.User;

@Mapper
public interface UserMapper extends BaseMapper<User> {

    // 雪花算法使用mybatis-plus  @TableId(value="id",type=ASSIGN_ID)  AUTO是数据库自增长  INPUT是通过set方法自行输入

    // @TableField场景：成员变量名与数据库字段名不一致    成员变量名以is开头，且是布尔值
    //                  成员变量名与数据库关键字冲突例如order   成员变量不是数据库字段使用@TableField(exist=false)
}
