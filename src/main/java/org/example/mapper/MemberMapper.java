package org.example.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.example.model.pojo.Member;

import java.util.List;

@Mapper
public interface MemberMapper extends BaseMapper<Member> {
    void insertList(List<Member> members);
}
