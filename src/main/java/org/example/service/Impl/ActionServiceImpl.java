package org.example.service.Impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.example.mapper.CommentMapper;
import org.example.mapper.LikeMapper;
import org.example.mapper.VideoMapper;
import org.example.model.dto.CommentListDTO;
import org.example.model.dto.CommentPublishDTO;
import org.example.model.dto.LikeActionDTO;
import org.example.model.dto.LikeListDTO;
import org.example.model.normal.RedisKey;
import org.example.model.pojo.Comment;
import org.example.model.pojo.Like;
import org.example.model.pojo.Video;
import org.example.repository.VideoRepository;
import org.example.service.ActionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;
import org.springframework.data.elasticsearch.core.query.ScriptType;
import org.springframework.data.elasticsearch.core.query.UpdateQuery;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
@Transactional(rollbackFor=Exception.class)
public class ActionServiceImpl implements ActionService {
    @Autowired
    private CommentMapper commentMapper;
    @Autowired
    private VideoMapper videoMapper;

    @Autowired
    private LikeMapper likeMapper;
    @Autowired
    private RedisTemplate<Object, Object> redisTemplate;
    @Autowired
    private ElasticsearchOperations elasticsearchOperations;

    @Override
    public void likeAction(LikeActionDTO likeActionDTO, Long id){
        Long commentId;
        Long videoId;
        if(likeActionDTO.getAction_type().equals("1")){
            if(StringUtils.hasText(likeActionDTO.getComment_id())){
                Like getLike = likeMapper.selectOne(new QueryWrapper<Like>()
                                .eq("comment_id", likeActionDTO.getComment_id()));
                if(getLike != null){
                    return;
                }
                // 给评论点赞
                commentId = Long.parseLong(likeActionDTO.getComment_id());
                Comment comment = commentMapper.selectById(commentId);
                if(comment == null) throw new RuntimeException("the comment is not exist");
                if(StringUtils.hasText(likeActionDTO.getVideo_id())){
                    videoId = Long.parseLong(likeActionDTO.getVideo_id());
                }else{
                    videoId = null;
                }
                commentMapper.update(null,new LambdaUpdateWrapper<Comment>()
                        .eq(Comment::getId,commentId)
                        .setSql("like_count = like_count + 1")
                );
            }else{
                // 给视频点赞
                commentId = null;
                videoId = Long.parseLong(likeActionDTO.getVideo_id());
                Double score = redisTemplate.opsForZSet().score(RedisKey.USER_LIKE + id, videoId);
                if(score==null){
                    // 未点赞
                    int update = videoMapper.update(null, new LambdaUpdateWrapper<Video>()
                            .eq(Video::getId, videoId)
                            .setSql("like_count = like_count + 1")
                    );
                    if(update > 0){
                        redisTemplate.opsForZSet().add(RedisKey.USER_LIKE+id,videoId,System.currentTimeMillis());
                    }
                    String script = "ctx._source.likeCount += params.likeCount;" + "ctx._source.updatedAt = params.updatedAt;";
                    UpdateQuery updateQuery = UpdateQuery.builder(videoId.toString())
                            .withScript(script)
                            .withScriptType(ScriptType.INLINE)
                            .withParams(Map.of(
                                    "likeCount",1,
                                    "updatedAt", System.currentTimeMillis()
                            )).build();
                    elasticsearchOperations.update(updateQuery, IndexCoordinates.of("video"));
                }
            }
            Like like = new Like(null,id,videoId,commentId,System.currentTimeMillis());

            likeMapper.insert(like);

        } else if (likeActionDTO.getAction_type().equals("2")) {
            if(StringUtils.hasText(likeActionDTO.getComment_id())){
                Like getLike = likeMapper.selectOne(new QueryWrapper<Like>()
                        .eq("comment_id", likeActionDTO.getComment_id()));
                if(getLike == null){
                    return;
                }
                // 给评论取消点赞  直接删除
                commentId = Long.parseLong(likeActionDTO.getComment_id());
                likeMapper.delete(new LambdaQueryWrapper<Like>()
                        .eq(Like::getCommentId,commentId)
                        .eq(Like::getUserId,id)
                );
                commentMapper.update(null,new LambdaUpdateWrapper<Comment>()
                        .eq(Comment::getUserId,id)
                        .setSql("like_count = like_count - 1")
                );

            }else{
                videoId = Long.parseLong(likeActionDTO.getVideo_id());
                likeMapper.delete(new LambdaQueryWrapper<Like>()
                        .eq(Like::getVideoId,videoId)
                        .eq(Like::getUserId,id)
                );
                videoMapper.update(null,new LambdaUpdateWrapper<Video>()
                        .eq(Video::getId,videoId)
                        .setSql("like_count = like_count - 1")
                );
                redisTemplate.opsForZSet().remove(RedisKey.USER_LIKE+id,videoId);
                String script = "ctx._source.likeCount += params.likeCount;" + "ctx._source.updatedAt = params.updatedAt;";
                UpdateQuery updateQuery = UpdateQuery.builder(videoId.toString())
                        .withScript(script)
                        .withScriptType(ScriptType.INLINE)
                        .withParams(Map.of(
                                "likeCount",-1,
                                "updatedAt", System.currentTimeMillis()
                        )).build();
                elasticsearchOperations.update(updateQuery, IndexCoordinates.of("video"));
            }
        }else{
            throw new IllegalArgumentException("action type error");
        }


    }

    @Override
    public List<Video> likeList(LikeListDTO likeListDTO) {
        int current = likeListDTO.getPage_num() > 0 ? likeListDTO.getPage_num()-1 : 0;
        int size = likeListDTO.getPage_size() >0 ? likeListDTO.getPage_size() : 10;
        Set<Object> set = redisTemplate.opsForZSet()
                .reverseRange(RedisKey.USER_LIKE + likeListDTO.getUser_id(), current*size ,current*size+size-1);
        if (set == null || set.isEmpty()) {
            return Collections.emptyList();
        }

        // 3. 转换为 Long 列表
        List<Long> videoIds = set.stream()
                .map(obj -> Long.valueOf(obj.toString()))
                .toList();

        // 4. 构造 ORDER BY FIELD 字符串，确保 ID 顺序与 Redis 中一致
        String idsSql = videoIds.stream()
                .map(String::valueOf)
                .collect(Collectors.joining(","));

        // 5. 数据库查询：使用 in 查询并强制按传入 ID 顺序排序
        return videoMapper.selectList(new LambdaQueryWrapper<Video>()
                .in(Video::getId, videoIds)
                .last("ORDER BY FIELD(id, " + idsSql + ")"));


//        Page<Video> page = new Page<>(current, size);
//        Long userId = Long.parseLong(likeListDTO.getUser_id());
//        IPage<Video> likeVideos = likeMapper.getLikeVideos(page, new QueryWrapper<Like>()
//                .eq("l.user_id", userId)
//                .isNull("l.comment_id")
//                .orderByDesc("l.created_at")
//        );
//        return likeVideos.getRecords();
    }

    @Override
    public void publishComment(CommentPublishDTO comment, Long id) {
        Comment insertComment = new Comment();
        Video video;
        insertComment.setUserId(id);
        insertComment.setContent(comment.getContent());
        insertComment.setLikeCount(0);
        insertComment.setCreatedAt(System.currentTimeMillis());
        insertComment.setUpdatedAt(System.currentTimeMillis());
        insertComment.setDeletedAt(LocalDateTime.now().plusYears(10).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli());
        Long targetVideoId;
        // 只要comment_id存在那么都走这一段代码
        if(StringUtils.hasText(comment.getComment_id())){
            // videoId 是空，说明回复的是评论，但是也需要把视频id取到
            Comment parentComment = commentMapper.selectById(comment.getComment_id());
            if(parentComment == null || parentComment.getDeletedAt()<=System.currentTimeMillis()) throw new RuntimeException("the comment does not exist");
            // 被回复的那条评论属于哪个视频那么该评论也是属于哪个视频，所以这个video_id可以设置成一样的
            targetVideoId = parentComment.getVideoId();
            insertComment.setVideoId(targetVideoId);
            insertComment.setReplyUserId(parentComment.getUserId());
            insertComment.setParentId(parentComment.getId());
            // 需要判断一下被回复的哪个root_id是不是为0，若是，那么那条评论就是新开始的一个评论，那么root_id就要修改成它的id，若不是那么就是相同的root_id
            Long rootId = parentComment.getRootId()==0? parentComment.getId() :  parentComment.getRootId();
            insertComment.setRootId(rootId);
            commentMapper.update(null,new LambdaUpdateWrapper<Comment>()
                    .eq(Comment::getId,parentComment.getId())
                    .setSql("child_count = child_count + 1")
            );
            if(!rootId.equals(parentComment.getId())){
                commentMapper.update(null,new LambdaUpdateWrapper<Comment>()
                        .eq(Comment::getId,rootId)
                        .setSql("child_count = child_count + 1"));
            }
        }else{
            // comment_id是空 只有video_id
            // 说明回复的是视频  也就是新起的一条root评论
            targetVideoId = Long.parseLong(comment.getVideo_id());
            insertComment.setVideoId(targetVideoId);
            insertComment.setRootId(0L);
            insertComment.setParentId(0L);
            video = videoMapper.selectOne(new LambdaQueryWrapper<Video>().eq(Video::getId, comment.getVideo_id()));
            if(Objects.isNull(video) || video.getDeletedAt() <= System.currentTimeMillis()) throw new RuntimeException("the video does no exist");
            insertComment.setReplyUserId(video.getUserId());
        }
        commentMapper.insert(insertComment);

        videoMapper.update(null, new LambdaUpdateWrapper<Video>()
                .eq(Video::getId, targetVideoId)
                .setSql("comment_count = comment_count + 1"));
        String script = "ctx._source.commentCount += params.commentCount;" + "ctx._source.updatedAt = params.updatedAt;";
        UpdateQuery updateQuery = UpdateQuery.builder(targetVideoId.toString())
                .withScript(script)
                .withScriptType(ScriptType.INLINE)
                .withParams(Map.of(
                        "commentCount",1,
                        "updatedAt", System.currentTimeMillis()
                )).build();
        elasticsearchOperations.update(updateQuery, IndexCoordinates.of("video"));

    }


    @Override
    public List<Comment> listComments(CommentListDTO comment){
        int current = comment.getPage_num() > 0 ? comment.getPage_num() : 1;
        int size = comment.getPage_size() >0 ? comment.getPage_size() : 10;
        Page<Comment> page = new Page<>(current, size);
        Page<Comment> commentPage;
        if(StringUtils.hasText(comment.getComment_id())){
            // 若存在comment_id  无论video_id是否为空  那么统一都是查找该comment_id下的评论
            commentPage = commentMapper.selectPage(page, new LambdaQueryWrapper<Comment>()
                    .eq(Comment::getParentId, Long.parseLong(comment.getComment_id()))
                    .gt(Comment::getDeletedAt,LocalDateTime.now())
                    .orderByDesc(Comment::getUpdatedAt)
            );
        }else{
            commentPage = commentMapper.selectPage(page, new LambdaQueryWrapper<Comment>()
                    .eq(Comment::getVideoId, Long.parseLong(comment.getVideo_id()))
                    .gt(Comment::getDeletedAt,LocalDateTime.now())
                    .orderByDesc(Comment::getUpdatedAt)
            );

        }
        return commentPage.getRecords();
    }

    @Override
    public void deleteComment(String videoId, String commentId, Long id){
        Long deleteVideoId;
        int rows;
        if(StringUtils.hasText(commentId)){
            // 若存在commentId  无论videoId是否为空  那么统一都是删除该commentId相关的评论
            // 删除评论前需要判断一下身份  判断要删除的评论是否是该id回复的
            Long deleteCommentId = Long.parseLong(commentId);
            Comment comment = commentMapper.selectById(deleteCommentId);
            if(comment == null || comment.getDeletedAt()<= System.currentTimeMillis()) throw new RuntimeException("the comment does not exist");
            if(!Objects.equals(comment.getUserId(), id)) throw new RuntimeException("这条评论不是你发表的，没有权限删除");
            deleteVideoId = comment.getVideoId();
            Long deleteRootId = comment.getRootId();
            Long deleteParentId = comment.getParentId();
            rows = commentMapper.update(null, new LambdaUpdateWrapper<Comment>()
                    .eq(Comment::getId, deleteCommentId)
                    .or()
                    .eq(Comment::getParentId, deleteCommentId)
                    .or()
                    .eq(Comment::getRootId, deleteCommentId)
                    .set(Comment::getDeletedAt,LocalDateTime.now())
            );
            videoMapper.update(null, new LambdaUpdateWrapper<Video>()
                    .eq(Video::getId, deleteVideoId)
                    .setSql("comment_count = comment_count - "+rows)
            );
            if(deleteRootId != 0){
                // 说明不是根回复  若是根回复则没有必要修改了
                commentMapper.update(null, new LambdaUpdateWrapper<Comment>()
                        .eq(Comment::getId,deleteRootId)
                        .setSql("child_count = child_count - "+rows)
                );
                if(!Objects.equals(deleteParentId, deleteRootId) && deleteParentId != 0){
                    commentMapper.update(null, new LambdaUpdateWrapper<Comment>()
                            .eq(Comment::getId,deleteParentId)
                            .setSql("child_count = child_count - 1")
                    );
                }

            }

        }else{
            deleteVideoId = Long.parseLong(videoId);
            Video video = videoMapper.selectById(deleteVideoId);
            if(video == null || video.getDeletedAt()<=System.currentTimeMillis()) throw new RuntimeException("the video does not exist");
            if(!Objects.equals(video.getUserId(), id)) throw new RuntimeException("这条评论不是你发表的，没有权限删除");
            rows = commentMapper.update(null, new LambdaUpdateWrapper<Comment>()
                    .eq(Comment::getVideoId, deleteVideoId)
                            .gt(Comment::getDeletedAt,LocalDateTime.now())
                    .set(Comment::getDeletedAt, LocalDateTime.now())
            );

            videoMapper.update(null,new LambdaUpdateWrapper<Video>()
                    .eq(Video::getId, deleteVideoId)
                    .setSql("comment_count = comment_count - "+rows)
            );
        }
        String script = "ctx._source.commentCount += params.commentCount;" + "ctx._source.updatedAt = params.updatedAt;";
        UpdateQuery updateQuery = UpdateQuery.builder(deleteVideoId.toString())
                .withScript(script)
                .withScriptType(ScriptType.INLINE)
                .withParams(Map.of(
                        "commentCount",-rows,
                        "updatedAt", System.currentTimeMillis()
                )).build();
        elasticsearchOperations.update(updateQuery, IndexCoordinates.of("video"));
    }
}
