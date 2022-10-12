package com.hcy.community.util;

import com.hcy.community.entity.User;
import org.springframework.stereotype.Component;

/**
 * 持有用户信息,用于代替session对象
 *
 * ThreadLocal在每个线程中维护一个ThreadLocalMap,
 * map中以ThreadLocal对象为key，value是存入的值
 */
@Component
public class HostHolder {

    private ThreadLocal<User> users = new ThreadLocal<>();

    public void setUser(User user) {
        users.set(user);
    }

    public User getUser() {
        return users.get();
    }

    public void clear() {
        users.remove();
    }

}
