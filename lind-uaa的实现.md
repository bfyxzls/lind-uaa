## lind-uaa实现总结
1. maven构建多模块系统
2. 网关
3. 授权服务
4. 资源服务
### maven构建多模块系统
```
<project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <groupId>com.xingfly</groupId>
    <artifactId>cloud</artifactId>
    <version>1.0-SNAPSHOT</version>
    <packaging>jar</packaging>

    <name>cloud</name>

    <properties>
        <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
    </properties>
    <modules>
        <module>account</module>
        <module>gateway</module>
        <module>xfauth</module>
    </modules>

</project>

```
### 网关
所有请求的统一入口，请求的分发，限流，路由处理等，框架使用zuul实现
> 核心配置主要体现了eureka,zuul网关分发和oauth2授权
```
eureka:
  client:
    service-url:
      defaultZone: http://${eureka.host:localhost}:${eureka.port:6761}/eureka/ #没有配置就使用默认值

zuul:
  routes:
    uaa:
      path: /uaa/**
      sensitiveHeaders: "*"
      serviceId: auth-service
    order:
      path: /accounts/**
      sensitiveHeaders: "*"
      serviceId: account-service
  add-proxy-headers: true

security:
  oauth2:
    client:
      access-token-uri: http://${spring.application.name}:${server.port}/uaa/oauth/token
      user-authorization-uri: http://${spring.application.name}:${server.port}/uaa/oauth/authorize
      client-id: webapp
    resource:
      user-info-uri: http://${spring.application.name}:${server.port}/uaa/user
      prefer-token-info: false
```
### 授权服务
请求从网关过来，由授权服务进行对用户建权，一般会提供统一的登陆，授权，注销等功能，
它会存储用户，角色，权限之间的关系，一般地他们之前都是多对多关系。
> 核心是对用户授权方法的实现,我们的授权服务要`UserDetailsService`接口，`loadUserByUsername`方法是具体授权服务需要根据自己的业务进行实现的。

#### 三大授权接口
授权服务需要实现`AuthorizationServerConfigurerAdapter`,`ResourceServerConfigurerAdapter`,`WebSecurityConfigurerAdapter`等接口，下面解决这两大接口的作用。
* AuthorizationServerConfigurerAdapter
* ResourceServerConfigurerAdapter
* WebSecurityConfigurerAdapter

##### AuthorizationServerConfigurerAdapter
配置授权服务一个比较重要的方面就是提供一个授权码给一个OAuth客户端（通过 authorization_code 授权类型），一个授权码的获取是OAuth客户端跳转到一个授权页面，然后通过验证授权之后服务器重定向到OAuth客户端，并且在重定向连接中附带返回一个授权码。

##### ResourceServerConfigurerAdapter
一个资源服务（可以和授权服务在同一个应用中，当然也可以分离开成为两个不同的应用程序）提供一些受token令牌保护的资源，Spring OAuth提供者是通过Spring Security authentication filter 即验证过滤器来实现的保护，你可以通过 @EnableResourceServer 注解到一个 @Configuration 配置类上，并且必须使用 ResourceServerConfigurer 这个配置对象来进行配置（可以选择继承自 ResourceServerConfigurerAdapter 然后覆写其中的方法，参数就是这个对象的实例），下面是一些可以配置的属性：
tokenServices：ResourceServerTokenServices 类的实例，用来实现令牌服务。
resourceId：这个资源服务的ID，这个属性是可选的，但是推荐设置并在授权服务中进行验证。
其他的拓展属性例如 tokenExtractor 令牌提取器用来提取请求中的令牌。
请求匹配器，用来设置需要进行保护的资源路径，默认的情况下是受保护资源服务的全部路径。
受保护资源的访问规则，默认的规则是简单的身份验证（plain authenticated）。
其他的自定义权限保护规则通过 HttpSecurity 来进行配置。
@EnableResourceServer 注解自动增加了一个类型为 OAuth2AuthenticationProcessingFilter 的过滤器链，

##### WebSecurityConfigurerAdapter
注意：如果你的应用程序中既包含授权服务又包含资源服务的话，那么这里实际上是另一个的低优先级的过滤器来控制资源接口的，这些接口是被保护在了一个访问令牌（access token）中，所以请挑选一个URL链接来确保你的资源接口中有一个不需要被保护的链接用来取得授权，就如上面示例中的 /login 链接，你需要在 WebSecurityConfigurer 配置对象中进行设置。
令牌端点默认也是受保护的，不过这里使用的是基于 HTTP Basic Authentication 标准的验证方式来验证客户端的，这在XML配置中是无法进行设置的（所以它应该被明确的保护）。

#### ConsumerTokenServices接口
下面代码展示了用户注销的功能，其中`ConsumerTokenServices`接口是系统提供了，它会按着事前约定的token进行相应的注销处理
```
@FrameworkEndpoint
public class RevokeTokenEndpoint {
    @Autowired
    @Qualifier("consumerTokenServices")
    ConsumerTokenServices consumerTokenServices;
    @RequestMapping(method = RequestMethod.DELETE, value = "/oauth/token")
    @ResponseBody
    public String revokeToken(String access_token) {
        if (consumerTokenServices.revokeToken(access_token)) {
            return "注销成功";
        } else {
            return "注销失败";
        }
    }
}

```
### 资源服务
请求从网关过来，它的用户登陆会统一走`授权服务`，服务从网关过来后需要带有token作为标识，token对应一个用户的身份，
它会有操作权限相关的配置，如果没有授权或者权限不足，会出现401或者403的异常。