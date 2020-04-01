package com.lagou.sqlSession;

import com.lagou.enums.SqlCommandType;
import com.lagou.pojo.Configuration;
import com.lagou.pojo.MappedStatement;

import java.lang.reflect.*;
import java.util.List;

public class DefaultSqlSession implements SqlSession {

    private Configuration configuration;

    public DefaultSqlSession(Configuration configuration) {
        this.configuration = configuration;
    }

    @Override
    public <E> List<E> selectList(String statementid, Object... params) throws Exception {

        //将要去完成对simpleExecutor里的query方法的调用
        simpleExecutor simpleExecutor = new simpleExecutor();
        MappedStatement mappedStatement = configuration.getMappedStatementMap().get(statementid);
        List<Object> list = simpleExecutor.query(configuration, mappedStatement, params);

        return (List<E>) list;
    }

    @Override
    public <T> T selectOne(String statementid, Object... params) throws Exception {
        List<Object> objects = selectList(statementid, params);
        if(objects.size()==1){
            return (T) objects.get(0);
        }else {
            throw new RuntimeException("查询结果为空或者返回结果过多");
        }
    }

    @Override
    public <T> T getMapper(Class<?> mapperClass) {
        // 使用JDK动态代理来为Dao接口生成代理对象，并返回
        Object proxyInstance = Proxy.newProxyInstance(DefaultSqlSession.class.getClassLoader(), new Class[]{mapperClass}, new InvocationHandler() {
            @Override
            public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                // 1.获取statementId
                String methodName = method.getName();
                String className = method.getDeclaringClass().getName();
                String statementId = className+"."+methodName;

                // 2.返回值类型
                Type genericReturnType = method.getGenericReturnType();

                // 3.执行相应的sql
                return executeSql(statementId, genericReturnType, args);

            }
        });
        return (T) proxyInstance;
    }

    @Override
    public Object executeSql(String statementId, Type genericReturnType, Object[] args) throws Exception{
        //完成对simpleExecutor里的query | update方法的调用
        simpleExecutor simpleExecutor = new simpleExecutor();
        MappedStatement mappedStatement = configuration.getMappedStatementMap().get(statementId);

        // 获取sql类型
        String sqlCommandType = mappedStatement.getSqlCommandType();
        Object result = null;
        // 查询
        if(SqlCommandType.SELECT.name().equalsIgnoreCase(sqlCommandType)){
            List<Object> objects = simpleExecutor.query(configuration, mappedStatement, args);

            // 判断返回值类型是否进行了 泛型类型参数化
            if(genericReturnType instanceof ParameterizedType){
                result = objects;
            } else {
                if(objects.size()==1){
                    result = objects.get(0);
                }else {
                    throw new RuntimeException("查询结果为空或者返回结果过多");
                }
            }
            // 插入、修改、删除
        } else if(SqlCommandType.INSERT.name().equalsIgnoreCase(sqlCommandType) ||
                SqlCommandType.UPDATE.name().equalsIgnoreCase(sqlCommandType) ||
                SqlCommandType.DELETE.name().equalsIgnoreCase(sqlCommandType)){

            result = simpleExecutor.update(configuration, mappedStatement, args);
        }
        return result;
    }
}
