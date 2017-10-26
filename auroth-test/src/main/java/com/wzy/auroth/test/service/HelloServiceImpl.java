package com.wzy.auroth.test.service;

import com.wzy.auroth.test.consul.register.po.User;
import com.wzy.auroth.thrift.annotation.TService;
import org.apache.thrift.TException;

import java.util.Random;

@TService
public class HelloServiceImpl implements HelloService.Iface {

    @Override
    public User getUser(long id) throws TException {
        if (new Random().nextInt(10) > 5) {
            throw new NullPointerException("测试");
        }
        User user = new User();
        user.setId(id);
        user.setAge(10);
        user.setName("hahahahahahah");
        user.setAddress("哈哈来时代峻峰大开杀戒方式登录卡机发快递送积分");
        return user;
    }
}
