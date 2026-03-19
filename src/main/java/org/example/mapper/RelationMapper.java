package org.example.mapper;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.example.model.pojo.Relation;
import org.example.model.vo.FollowingListVO;
import com.baomidou.mybatisplus.core.toolkit.Constants;

import java.util.List;

@Mapper
public interface RelationMapper extends BaseMapper<Relation> {

}
