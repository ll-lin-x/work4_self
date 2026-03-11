package org.example.mapper;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Constants;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.example.model.dto.BatchUpdateDTO;
import org.example.model.pojo.Video;

import java.util.List;

@Mapper
public interface VideoMapper extends BaseMapper<Video> {
    void batchIncrementCount(@Param("columName") String columName,@Param("videos") List<BatchUpdateDTO> videos);

    IPage<Video> selectSearchVideoList(Page<Video> videoPage,@Param(Constants.WRAPPER) Wrapper<Video> wrapper);
}
