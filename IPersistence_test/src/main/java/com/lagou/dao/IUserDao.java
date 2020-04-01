package com.lagou.dao;

import com.lagou.pojo.User;

import java.util.List;

public interface IUserDao {

    // 查询所有用户
    public List<User> findAll() throws Exception;


    // 根据条件进行用户查询
    public User findByCondition(User user) throws Exception;

    // 插入
    public int insertUser(User user) throws Exception;

    // 修改
    public int updateUser(User user) throws Exception;

    // 删除
    public int deleteUser(User user) throws Exception;
}
