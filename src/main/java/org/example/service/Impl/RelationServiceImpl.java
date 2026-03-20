package org.example.service.Impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.annotations.Mapper;
import org.example.mapper.RelationMapper;
import org.example.mapper.UserMapper;
import org.example.model.dto.FollowingListDTO;
import org.example.model.normal.RedisKey;
import org.example.model.pojo.Relation;
import org.example.model.pojo.User;
import org.example.model.pojo.Video;
import org.example.model.vo.FollowingListVO;
import org.example.service.RelationService;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
public class RelationServiceImpl implements RelationService {
    @Autowired
    private RelationMapper relationMapper;
    @Autowired
    private UserMapper userMapper;
    @Autowired
    private RedisTemplate redisTemplate;

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void relationAction(String toUserId, Integer actionType,Long userId) {
        Long focusId ;
        try{
           focusId = Long.parseLong(toUserId);
        }catch(Exception e){
            throw new IllegalArgumentException("to_user_id illegal value");
        }
        // 自己不能关注/取消关注自己
        if(focusId.equals(userId)) return;
        Double score = redisTemplate.opsForZSet().score(RedisKey.USER_FOLLOWING + userId, focusId);
        if(actionType==1){
            // 关注操作
            // 若已关注则不能再次点击关注
            if(score!=null) return;
            relationMapper.insert(new Relation(null,userId,focusId,System.currentTimeMillis()));
            redisTemplate.opsForZSet().add(RedisKey.USER_FOLLOWING + userId, focusId, System.currentTimeMillis());
            redisTemplate.opsForZSet().add(RedisKey.USER_FOLLOWER + focusId, userId, System.currentTimeMillis());


        }else if(actionType==2){
            // 取消关注
            // 若未关注则不能取消关注
            if(score==null) return;
            // 查看对方是否关注
            relationMapper.delete(new LambdaQueryWrapper<Relation>()
                    .eq(Relation::getUserId,userId)
                    .eq(Relation::getFocusUserId,focusId)
            );
            redisTemplate.opsForZSet().remove(RedisKey.USER_FOLLOWING + userId, focusId);
            redisTemplate.opsForZSet().remove(RedisKey.USER_FOLLOWING + focusId, userId);
        }else{
            throw new IllegalArgumentException("action_type illegal");
        }

    }

    @Override
    public List<FollowingListVO> followingList(FollowingListDTO followingListDTO){
        Long id = Long.parseLong(followingListDTO.getUser_id());
        Long pageNum = followingListDTO.getPage_num()>0? followingListDTO.getPage_num()-1:0L;
        Long pageSize = followingListDTO.getPage_size()>0? followingListDTO.getPage_size():10L;
        Set<ZSetOperations.TypedTuple<Object>> set = redisTemplate.opsForZSet().reverseRangeWithScores(RedisKey.USER_FOLLOWING + id, pageNum*pageSize, pageNum*pageSize + pageSize-1);
        List<Long> userIds = set.stream().map(tuple -> Long.valueOf(tuple.getValue().toString())).toList();
        String ids = userIds.stream().map(Object::toString).collect(Collectors.joining(","));
        List<User> users = userMapper.selectList(new LambdaQueryWrapper<User>()
                .in(User::getId, userIds)
                .last("order by field (id," + ids + ")")
        );
        return users.stream().map(user->{
            FollowingListVO followingListVO = new FollowingListVO();
            BeanUtils.copyProperties(user,followingListVO);
            return followingListVO;
                }

        ).toList();
    }

    @Override
    public List<FollowingListVO> followerList(FollowingListDTO followingListDTO){
        Long id = Long.parseLong(followingListDTO.getUser_id());
        Long pageNum = followingListDTO.getPage_num()>0? followingListDTO.getPage_num()-1:0L;
        Long pageSize = followingListDTO.getPage_size()>0? followingListDTO.getPage_size():10L;
        Set<ZSetOperations.TypedTuple<Object>> set = redisTemplate.opsForZSet().reverseRangeWithScores(RedisKey.USER_FOLLOWER + id, pageNum*pageSize, pageNum*pageSize + pageSize-1);
        List<Long> userIds = set.stream().map(tuple -> Long.valueOf(tuple.getValue().toString())).toList();
        String ids = userIds.stream().map(Object::toString).collect(Collectors.joining(","));
        List<User> users = userMapper.selectList(new LambdaQueryWrapper<User>()
                .in(User::getId, userIds)
                .last("order by field (id," + ids + ")")
        );
        return users.stream().map(user->{
                    FollowingListVO followingListVO = new FollowingListVO();
                    BeanUtils.copyProperties(user,followingListVO);
                    return followingListVO;
                }

        ).toList();
    }
    @Override
    public List<FollowingListVO> friendsList(FollowingListDTO followingListDTO,Long userId) {
        Long pageNum = followingListDTO.getPage_num()>0? followingListDTO.getPage_num()-1:0L;
        Long pageSize = followingListDTO.getPage_size()>0? followingListDTO.getPage_size():10L;
        String key = RedisKey.USER_FRIEND + userId;
        redisTemplate.opsForZSet().intersectAndStore(RedisKey.USER_FOLLOWING + userId, RedisKey.USER_FOLLOWER + userId, key);
        redisTemplate.expire(key,30, TimeUnit.SECONDS);
        Set<ZSetOperations.TypedTuple<Object>> set = redisTemplate.opsForZSet().reverseRangeWithScores(key, pageNum*pageSize, pageNum*pageSize + pageSize-1);
        List<Long> userIds = set.stream().map(tuple -> Long.valueOf(tuple.getValue().toString())).toList();
        String ids = userIds.stream().map(Object::toString).collect(Collectors.joining(","));
        List<User> users = userMapper.selectList(new LambdaQueryWrapper<User>()
                .in(User::getId, userIds)
                .last("order by field (id," + ids + ")")
        );
        return users.stream().map(user->{
                    FollowingListVO followingListVO = new FollowingListVO();
                    BeanUtils.copyProperties(user,followingListVO);
                    return followingListVO;
                }

        ).toList();
    }


}
