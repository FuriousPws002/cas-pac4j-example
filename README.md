# 简述
该项目集成了CAS Server CAS Client 以及客户端采用Pac4j的方式来认证
# 什么是单点登录
单点登录（Single Sign On），简称 SSO，常用于多个系统中，用户只需登录一次，就可以访问其他信任的系统。比如，员工登录过OA系统后，可以直接访问邮件系统，而不再需要登录邮件系统。这里的OA系统和邮件系统，可以认为是相互信任的子系统，他们共用一套用户数据，这套用户数据的权限认证由统一的认证服务器来认证。

# CAS协议
现阶段比较流行的单点登录解决方案是[CAS(Central Authentication Service)](https://www.apereo.org/projects/cas) 官网中有如下说明
>Enterprise Single Sign-On - CAS provides a friendly open source community that actively supports and contributes to the project. While the project is rooted in higher-ed open source, it has grown to an international audience spanning Fortune 500 companies and small special-purpose installations.

`CAS`提供了[CAS协议](https://apereo.github.io/cas/5.3.x/protocol/CAS-Protocol.html) 来完成单点登录，主要流程如下
1. 用户第一次向浏览器访问应用1(https://app.example.com) ，应用1判断用户没有登录，重定向到CAS server(https://cas.example.com/cas/login?service=https://app.example.com)

2. CAS server中判断当前用户的sso的session不存在，展示用户名密码输入框

3. 用户输入用户名密码向CAS server登录，验证通过后，CAS server 生成sso的session，向浏览器设置cookie，CASTGC=TGT-...
其中cookie的值就是sso中session的key。同时CAS server会对应用1生成一个ticket，再去重定向到应用1中，应用1拿到这个ticket再次请求CAS server获取授权数据。成功后，设置cookie，jessionid=...。到此，应用1第一次认证结束。

4. 当应用1第二次认证的时候直接校验jessionid是否合法即可。

5. 当应用2第一次访问的时候，同样会重定向到CAS server中去，只不过，此时，由于，应用1已经认证通过了，同时sso的session已经保存了，所以，CAS server不会再去要求用户登录，而是直接对应用2生成一个ticket，后面的逻辑就和前面一样了。

# 代码说明
本机环境版本
- jdk 1.8
- maven 3.6.3
- mysql 5.7.29
- cas server 5.3.14
- cas client 3.6.1
- shiro 1.5.3
- pac4j 4.0.3

## CAS服务端
直接clone官方提供的overlay`git clone https://github.com/apereo/cas-overlay-template.git`
由于最新版6.3.x需要jdk11的支持，所以切换了5.3的分支
添加了src/main/resources/目录，在该目录下创建了application.properties文件和services目录，用于cas server的自定义认证方式。
由于认证数据依赖数据库，请先运行sql文件，更改application.properties的数据库连接信息。
修改pom文件，添加如下依赖
```
        <dependency>
            <groupId>org.apereo.cas</groupId>
            <artifactId>cas-server-support-rest</artifactId>
            <version>${cas.version}</version>
        </dependency>
        <dependency>
            <groupId>mysql</groupId>
            <artifactId>mysql-connector-java</artifactId>
            <version>${mysql.version}</version>
        </dependency>
        <dependency>
            <groupId>org.apereo.cas</groupId>
            <artifactId>cas-server-support-jdbc</artifactId>
            <version>${cas.version}</version>
        </dependency>
        <dependency>
            <groupId>org.apereo.cas</groupId>
            <artifactId>cas-server-support-jdbc-drivers</artifactId>
            <version>${cas.version}</version>
        </dependency>
```
命令行运行`mvn clean package & java -jar target/cas.war` 等待服务启动后，访问http://locahost:8443/cas ， 会跳转登录，输入u/p登录成功。到此，cas服务端算是ok了。

# CAS客户端
新建spring boot项目`cas-client2`和`cas-client3`，其中cas-client2的端口为8082，cas-client3的端口为8083，pom中添加如下依赖
```
        <dependency>
            <groupId>org.jasig.cas.client</groupId>
            <artifactId>cas-client-support-springboot</artifactId>
            <version>${cas.client.version}</version>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>

        <dependency>
            <groupId>org.projectlombok</groupId>
            <artifactId>lombok</artifactId>
            <optional>true</optional>
        </dependency>
```
添加注解`@EnableCasClient`启用casClient，application中添加如下cas服务配置
```
cas.server-url-prefix=http://localhost:8443/cas
cas.server-login-url=${cas.server-url-prefix}/login
cas.client-host-url=http://localhost:${server.port}
```
启动客户端cas-client2和cas-client3，访问http://localhost:8082 ，会跳转cas登录，然后在访问http://localhost:8083 ，会发现已经认证通过，不会跳转cas登录，达到了单点登录的效果。

# 客户端shiro集成cas服务认证
现实的系统中可能不是像上面那样简单的配置，可能客户端先前已经集成了shiro等权限框架，后面再接入cas认证，这种情况怎么处理呢？
新建spring boot项目`cas-client1`
pom中添加入下依赖
```
        <dependency>
            <groupId>io.buji</groupId>
            <artifactId>buji-pac4j</artifactId>
            <version>${buji.version}</version>
        </dependency>
        <dependency>
            <groupId>org.pac4j</groupId>
            <artifactId>pac4j-cas</artifactId>
            <version>${pac4j.version}</version>
        </dependency>
        <dependency>
            <groupId>org.apache.shiro</groupId>
            <artifactId>shiro-spring-boot-web-starter</artifactId>
            <version>${shiro.version}</version>
        </dependency>
```
application.properties的配置如下
```
server.port=8081

cas.server-url-prefix=http://localhost:8443/cas
cas.server-login-url=${cas.server-url-prefix}/login
cas.client-host-url=http://localhost:${server.port}
cas.client-name=app-client1

shiro.loginUrl=/login.html

```

主类中`CasClient1Application`添加了cas认证的相关配置，而`TestController`则包含了单纯的shiro登录和cas的rest方式登录。
启动后，访问http://localhost:8081/cas.html ，会跳转到CAS Server去认证，认证完成后，访问http://localhost:8081/test 可以测试权限数据是否正确。
CAS Server除了采用CAS本身的login登录界面外，还支持rest的方式，获取认证，访问http://localhost:8081/login.html ，输入u/p后会通过rest的方式认证。
