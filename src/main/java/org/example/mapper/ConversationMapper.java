package org.example.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.example.model.pojo.Conversation;


@Mapper
public interface ConversationMapper extends BaseMapper<Conversation> {
}
