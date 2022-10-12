package com.hcy.community.mapper;

import com.hcy.community.entity.LoginTicket;
import org.apache.ibatis.annotations.*;

@Deprecated
@Mapper
public interface LoginTicketMapper {
    
    @Options(useGeneratedKeys = true,keyProperty = "id")
    @Insert("insert into login_ticket (user_id,ticket,status,expired) values (#{userId},#{ticket},#{status},#{expired})")
    int insertLoginTicket(LoginTicket loginTicket);
    
    @Select("select id,user_id,ticket,status,expired from login_ticket where ticket = #{ticket}")
    LoginTicket selectByTicket(@Param("ticket")String ticket);
    
    @Update("update login_ticket set status = #{status} where ticket = #{ticket}")
    int updateStatus(@Param("ticket")String ticket,@Param("status")int status);
    
    @Select("select id,user_id,ticket,status,expired from login_ticket where user_id = #{userId} and status = 0")
    LoginTicket selectByUserId(@Param("userId")int userId);
}
