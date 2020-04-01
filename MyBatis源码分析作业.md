# 一、简单题

## 1.Mybatis动态Sql是做什么的？都有哪些动态sql？简述一下动态sql的执行原理？

**答：**

①`Mybatis`动态`SQL`可以通过`XML`标签的形式编写动态`SQL`，完成**逻辑判断**和**动态拼接SQL**的功能

②`Mybatis` 动态 `SQL`标签主要有：

```
<if/>、<choose/>、<when/>、<otherwise/>、<trim/>、<when/>、<set/>、<foreach/>、<bind/>
```

> bind之前没用过特此记录，bind:可以将OGNL表达式的值绑定到变量中，方便后来引用变量值

```xml
<!-- 伪代码 -->
<select id="findByCondition" paramterType="com.lagou.pojo.User" resultType="com.lagou.pojo.User">
    <!-- bind:可以将OGNL表达式的值绑定到一个变量中，方便后来引用这个变量的值 -->
    <!-- username是User中一个属性值 -->
    <bind name="bindeName" value="'%'+username+'%'"/>
    SELECT * FROM user
    <if test="username != null">
        where username like #{bindeName}
    </if>
</select>
```

③动态`SQL`执行原理（核心方法在`SqlSource.getBoundSql()`）

+ 1.`Executor`预处理`MappedStatement`的`sql`，会调用`MappedStatement`的`getBoundSql`方法。
+ 2.`MappedStatement`的`getBoundSql`中会调用`SqlSource.getBoundSql()`，此方法中会调用`SqlNode.apply()`方法处理不同的ONGL表达式，并进行sql拼接。
+ 3.最后通过`GenericTokenParser`将`#0`占位符替换成？，并设置相应参数。



## 2.Mybatis是否支持延迟加载？如果支持，它的实现原理是什么？

答：

①`MyBatis`仅支持`association`关联对象（一对一查询）和`collection`关联集合对象（一对多查询）的延迟加载

②**实现原理：**

+ 使用CGLIB创建目标对象的代理对象，当调用目标方法时，进入拦截器方法
+ 比如调用a.getB().getName()，拦截器invoke()方法发现a.getB()是null值
+ 就会单独发送事先保存好的查询关联B对象的sql，把B查询出来，然后调用a.setB(b)，于是a的对象b属性就有值了，接着完成a.getB().getName()方法的调用。这就是延迟加载的基本原理。

## 3.Mybatis都有哪些Executor执行器？它们之间的区别是什么？

答：三种基本的`Executor`执行器：`SimpleExecutor、ReuseExecutor、BatchExecutor`。

**SimpleExecutor**：每执行一次update或select，就开启一个Statement对象，用完立刻关闭Statement对象

**ReuseExecutor**：执行update或select，以sql作为key查找Statement对象，存在就使用，不存在就创建，用完后不关闭Statement对象，而是放置于Map内，供下一次使用。重复使用Statement对象。

**BatchExecutor**：执行update（没有select，JDBC批处理不支持select），将所有sql都添加到批处理中（addBatch()），等待统一执行（executeBatch()），它缓存了多个Statement对象，每个Statement对象都是addBatch()完毕后，等待逐一执行executeBatch()批处理。与JDBC批处理相同。



## 4.简述下Mybatis的一级、二级缓存（分别从存储结构、范围、失效场景三方面来作答）？

答：

**①一级缓存**（默认打开）

```properties
1.存储结构：基于 PerpetualCache 的 HashMap 本地缓存
2.范围：	Session
3.失效场景：
	1）当Session flush 或 close 之后，该 Session 中的所有 Cache 就将清空；
	2）在Session中进行了insert、update、delete后，缓存会被清空。
```

**②二级缓存**

```properties
1.存储结构：基于 PerpetualCache 的 HashMap 本地缓存
2.范围：	Mapper（Namespace）
3.失效场景：同一个namespace中，如果有insert、update、delete操作，缓存将清空
```



## 5.简述Mybatis的插件运行原理，以及如何编写一个插件？

答：

**1）插件编写范围**

Mybatis仅可以编写ParameterHandler、ResultSetHandler、StatementHandler、Executor这4种接口的插件

**2）运行原理**

使用JDK的动态代理，为需要拦截的接口生成代理对象以实现接口方法拦截功能，每当执行这4种接口对象的方法时，就会进入拦截方法，具体就是InvocationHandler的invoke()方法，只会拦截那些你指定需要拦截的方法。

**3）编写插件**

+ 实现Mybatis的Interceptor接口并复写intercept()方法
+ 给插件编写注解，指定要拦截哪一个接口的哪些方法
+ 配置文件中配置编写的插件



# 二、编程题

## 1.完善自定义持久层框架IPersistence，添加修改、删除功能（采用getMapper方式）

**①思路：**

1）XMLMapperBuilder中解析Mapper时，增加一个字段SqlCommandType表示sql类型（select、insert、update、delete）。

2）DefaultSqlSession动态代理调用方法时，根据sql类型执行相应操作。

**②核心代码：**

1）DefaultSqlSession的getMapper方法

```java
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
```

2）DefaultSqlSession的executeSql方法（自己新加的）

```java
private Object executeSql(String statementId, Type genericReturnType, Object[] args) throws Exception{
	//将要去完成对simpleExecutor里的query方法的调用
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
```

**③测试方法**

```
IPersistenceTest的test1()、test2()、test3()、test4()方法
对应于增删改查
```

