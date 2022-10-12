package com.hcy.community.config;

import com.hcy.community.quartz.PostScoreRefreshJob;
import org.quartz.JobDataMap;
import org.quartz.JobDetail;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.quartz.JobDetailFactoryBean;
import org.springframework.scheduling.quartz.SimpleTriggerFactoryBean;

// 配置 -> 数据库 -> 调用
// 此配置仅在第一次的时候被读取到,初始化到数据库,
// 以后quartz是访问数据库去调用
@Configuration
public class QuartzConfig {

    // FactoryBean可简化Bean的实例化过程:
    // 1.通过FactoryBean封装Bean的实例化过程
    // 2.将FactoryBean装配到Spring容器里
    // 3.将FactoryBean注入给其他的Bean
    // 4.该Bean得到的是FactoryBean所管理的对象实例

    /*
    帖子的分数: log(精华分+评论数*10+点赞数*2+收藏数*2)+(发布时间-牛客纪元)
    可以看出分数和高频的操作有关,评论、点赞、收藏,每次操作都会改变帖子分数,
    因此每次操作后都进行一次帖子的评分是不现实的,故采用定时任务的方法
    
    定时算分的时候也不是将全部贴子都算一次。
    先将发生分数变化的帖子存在redis缓存中,当定时到了的时候,再从缓存中取出,
    进行计算
     */
    
    // 配置JobDetail
    // 刷新帖子分数任务
    @Bean
    public JobDetailFactoryBean postScoreRefreshJobDetail() {
        JobDetailFactoryBean factoryBean = new JobDetailFactoryBean();
        factoryBean.setJobClass(PostScoreRefreshJob.class);
        factoryBean.setName("postScoreRefreshJob");
        factoryBean.setGroup("communityJobGroup");
        factoryBean.setDurability(true);//任务是否持久保存
        factoryBean.setRequestsRecovery(true);//任务是否可恢复
        return factoryBean;
    }

    @Bean
    public SimpleTriggerFactoryBean postScoreRefreshTrigger(JobDetail postScoreRefreshJobDetail) {
        SimpleTriggerFactoryBean factoryBean = new SimpleTriggerFactoryBean();
        factoryBean.setJobDetail(postScoreRefreshJobDetail);
        factoryBean.setName("postScoreRefreshTrigger");
        factoryBean.setGroup("communityTriggerGroup");
        factoryBean.setRepeatInterval(1000 * 60 * 5);//执行频率
        factoryBean.setJobDataMap(new JobDataMap());
        return factoryBean;
    }

}
