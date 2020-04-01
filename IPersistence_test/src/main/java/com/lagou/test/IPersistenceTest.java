package com.lagou.test;

import com.lagou.dao.IUserDao;
import com.lagou.io.Resources;
import com.lagou.pojo.User;
import com.lagou.sqlSession.SqlSession;
import com.lagou.sqlSession.SqlSessionFactory;
import com.lagou.sqlSession.SqlSessionFactoryBuilder;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

public class IPersistenceTest {

    // 1.查找用户
    @Test
    public void test1() throws Exception {
        InputStream resourceAsSteam = Resources.getResourceAsStream("sqlMapConfig.xml");
        SqlSessionFactory sqlSessionFactory = new SqlSessionFactoryBuilder().build(resourceAsSteam);
        SqlSession sqlSession = sqlSessionFactory.openSession();

        //调用
        User user = new User();
        user.setId(1);

        User user2 = sqlSession.selectOne("com.lagou.dao.IUserDao.findByCondition", user);
        System.out.println(user2);

        List<User> users = sqlSession.selectList("com.lagou.dao.IUserDao.findAll");
        for (User user1 : users) {
            System.out.println(user1);
        }

        IUserDao userDao = sqlSession.getMapper(IUserDao.class);
        User user3 = userDao.findByCondition(user);
        System.out.println(user3);

        List<User> all = userDao.findAll();
        for (User user1 : all) {
            System.out.println(user1);
        }
    }

    private IUserDao userDao;

    @Before
    public void befor() throws Exception {
        InputStream resourceAsStream = Resources.getResourceAsStream("sqlMapConfig.xml");
        SqlSessionFactory sqlSessionFactory = new SqlSessionFactoryBuilder().build(resourceAsStream);
        SqlSession sqlSession = sqlSessionFactory.openSession();
        userDao = sqlSession.getMapper(IUserDao.class);
    }

    // 2.插入
    @Test
    public void test2() throws Exception{
        User user = new User();
        user.setId(5);
        user.setUsername("插入测试");

        userDao.insertUser(user);

        System.out.println(userDao.findByCondition(user));
    }

    // 3.修改
    @Test
    public void test3() throws Exception{
        User user = new User();
        user.setId(5);
        user.setUsername("修改测试");

        userDao.updateUser(user);

        System.out.println(userDao.findByCondition(user));
    }

    // 4.删除
    @Test
    public void test4() throws Exception{
        User user = new User();
        user.setId(5);

        userDao.deleteUser(user);

        System.out.println(userDao.findByCondition(user));
    }

}
