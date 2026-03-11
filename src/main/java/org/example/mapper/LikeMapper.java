package org.example.mapper;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Constants;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.example.model.pojo.Like;
import org.example.model.pojo.Video;

import java.util.List;

@Mapper
public interface LikeMapper extends BaseMapper<Like> {
    IPage<Video> getLikeVideos(Page<Video> page, @Param(Constants.WRAPPER) Wrapper<Like> wrapper);

    List<Long> selectUserIdsByVideoId(Long videoId);
}
