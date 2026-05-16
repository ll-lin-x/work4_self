package org.example.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import io.lettuce.core.dynamic.annotation.Param;
import org.apache.ibatis.annotations.Mapper;
import org.example.model.pojo.Message;

import java.util.List;

@Mapper
public interface MessageMapper extends BaseMapper<Message> {
    void insertList(@Param("messages") List<Message> messages);
}
