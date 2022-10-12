package com.hcy.community.service;

import com.github.benmanes.caffeine.cache.CacheLoader;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import com.hcy.community.entity.DiscussPost;
import com.hcy.community.mapper.DiscussPostMapper;
import com.hcy.community.util.SensitiveFilter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.util.HtmlUtils;

import javax.annotation.PostConstruct;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class DiscussPostService {
    @Autowired
    private DiscussPostMapper discussPostMapper;
    
    @Autowired
    private SensitiveFilter sensitiveFilter;
    
    @Value("${caffeine.posts.max-size}")
    private int maxSize;
    
    @Value("${caffeine.posts.expire-seconds}")
    private int expireSeconds;
    
    //caffeine核心接口:Cache,LoadingCache(同步缓存),AsyncLoadingCache
    //LoadingCache:多个线程线程访问缓存时，若缓存没有对应的数据，只允许一个线程去数据库中取数据，其他线程则排队等候
    
    //帖子列表缓存
    // 只需初始化一次
    
    private LoadingCache<String, List<DiscussPost>> postListCache;
    
    // 帖子总数缓存
    // 只需初始化一次
    // findDiscussPostRows方法调用频繁,且该数据不是很重要,因此可以将总页数缓存起来
    private LoadingCache<Integer, Integer> postRowsCache;
    
    @PostConstruct
    public void init() {
        //初始化热门帖子列表缓存
        postListCache = Caffeine.newBuilder()
                .maximumSize(maxSize)
                .expireAfterWrite(expireSeconds, TimeUnit.SECONDS)
                .build(new CacheLoader<String, List<DiscussPost>>() {
                    @Override
                    public List<DiscussPost> load(String key) throws Exception {
                        //查询数据库得到缓存数据的方法
                        if (StringUtils.isBlank(key)) {
                            throw new IllegalArgumentException("参数错误！");
                        }
                        String[] params = key.split(":");
                        if (params == null || params.length != 2) {
                            throw new IllegalArgumentException("参数错误！");
                        }
                        int offset = Integer.parseInt(params[0]);
                        int limit = Integer.parseInt(params[1]);
                        
                        log.debug("load post list from DB");
                        return discussPostMapper.selectDiscussPosts(0, offset, limit, 1);
                    }
                });
        //初始化帖子总数缓存
        postRowsCache = Caffeine.newBuilder()
                .maximumSize(maxSize)
                .expireAfterWrite(expireSeconds, TimeUnit.SECONDS)
                .build(new CacheLoader<Integer, Integer>() {
                    @Override
                    public Integer load(Integer key) throws Exception {
                        log.debug("load post list from DB");
                        return discussPostMapper.selectDiscussPostRows(key);
                    }
                });
    }
    
    /**
     * 分页查询帖子
     *
     * @return
     */
    public List<DiscussPost> findDiscussPosts(int userId, int offset, int limit, int orderMode) {
        //缓存热门列表
        //此时userId为0且orderMode为1
        //通过offset和limit可以唯一确定帖子列表页
        if (userId == 0 && orderMode == 1) {
            return postListCache.get(offset + ":" + limit);
        }
        log.debug("load post list from DB");
        return discussPostMapper.selectDiscussPosts(userId, offset, limit, orderMode);
    }
    
    /**
     * 查询帖子数量
     *
     * @return 该方法调用频繁, 可以将总页数缓存起来
     */
    public int findDiscussPostRows(int userId) {
        //缓存热门列表帖子页数
        if (userId == 0) {
            return postRowsCache.get(userId);
        }
        log.debug("load post rows from DB");
        return discussPostMapper.selectDiscussPostRows(userId);
    }
    
    /**
     * 发布帖子
     *
     * @param post
     * @return
     */
    public int addDiscussPost(DiscussPost post) {
        if (post == null) {
            throw new IllegalArgumentException("参数不能为空！");
        }
        
        //转义HTML标记
        /*
            <div>hello world</div><p>&nbsp;</p>
            
            &lt;div&gt;hello world&lt;/div&gt;&lt;p&gt;&amp;nbsp;&lt;/p&gt;
         */
        post.setTitle(HtmlUtils.htmlEscape(post.getTitle()));
        post.setContent(HtmlUtils.htmlEscape(post.getContent()));
        //过滤敏感词
        post.setTitle(sensitiveFilter.filter(post.getTitle()));
        post.setContent(sensitiveFilter.filter(post.getContent()));
        
        return discussPostMapper.insertDiscussPost(post);
    }
    
    /**
     * 帖子详情
     *
     * @param id
     * @return
     */
    public DiscussPost findDiscussPostById(int id) {
        return discussPostMapper.selectDiscussPostById(id);
    }
    
    /**
     * 修改帖子评论数
     *
     * @param id
     * @param commentCount
     * @return
     */
    public int updateCommentCount(int id, int commentCount) {
        return discussPostMapper.updateCommentCount(id, commentCount);
    }
    
    /**
     * 修改帖子类型 0-普通; 1-置顶;
     *
     * @return
     */
    public int updateType(int id, int type) {
        return discussPostMapper.updateType(id, type);
    }
    
    /**
     * 修改帖子状态 0-正常; 1-精华; 2-拉黑;
     *
     * @return
     */
    public int updateStatus(int id, int status) {
        return discussPostMapper.updateStatus(id, status);
    }
    
    public int updateScore(int id, double score) {
        return discussPostMapper.updateScore(id, score);
    }
}
