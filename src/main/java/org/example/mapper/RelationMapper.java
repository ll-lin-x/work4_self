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

@Mapper
public interface RelationMapper extends BaseMapper<Relation> {
    IPage<FollowingListVO> getFollowingPage(Page<FollowingListVO> page,Long userId);

    IPage<FollowingListVO> getFollowerPage(Page<FollowingListVO> page, Long id);

    IPage<FollowingListVO> getFriendsPage(Page<FollowingListVO> page,@Param(Constants.WRAPPER) LambdaQueryWrapper<Relation> queryWrapper);
}
