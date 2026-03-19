package org.example.model.normal;


public class RedisKey {
    // 用户登录
    public static final String USER_LOGIN = "user:login:";
    // 视频详情
    public static final String VIDEO_DETAIL = "video:detail:";
    // 热门排行
    public static final String VIDEO_RANK_TOTAL = "video:rank:total:";
    // 播放量增量
    public static final String VIDEO_RANK_INCREMENT = "video:rank:increment:";
    // 视频点赞
    public static final String VIDEO_LIKE = "video:like:";
    // 用户收件箱
    public static final String USER_FEED = "user:feed:";
    // 搜索历史
    public static final String VIDEO_SEARCH_HISTORY = "video:searchHistory:";
    // 关注列表
    public static final String USER_FOLLOWING =  "user:following:";
    // 粉丝列表
    public static final String USER_FOLLOWER = "user:follower:";
    // 朋友列表
    public static final String USER_FRIEND = "user:friend:";
    // 分布式锁
    public static final String LOCK_RANK_INIT = "lock:video:rank:init:";
    public static final String LOCK_VIDEO_RANK = "lock:video:rank:";
}
