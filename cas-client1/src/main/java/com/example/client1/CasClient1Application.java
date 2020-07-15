package com.example.client1;

import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

import org.apache.commons.collections.map.HashedMap;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.authc.AuthenticationException;
import org.apache.shiro.authc.AuthenticationInfo;
import org.apache.shiro.authc.AuthenticationToken;
import org.apache.shiro.authc.SimpleAuthenticationInfo;
import org.apache.shiro.authc.UsernamePasswordToken;
import org.apache.shiro.authz.AuthorizationInfo;
import org.apache.shiro.authz.SimpleAuthorizationInfo;
import org.apache.shiro.mgt.SecurityManager;
import org.apache.shiro.realm.AuthenticatingRealm;
import org.apache.shiro.realm.Realm;
import org.apache.shiro.spring.web.ShiroFilterFactoryBean;
import org.apache.shiro.spring.web.config.DefaultShiroFilterChainDefinition;
import org.apache.shiro.spring.web.config.ShiroFilterChainDefinition;
import org.apache.shiro.subject.PrincipalCollection;
import org.pac4j.cas.client.CasClient;
import org.pac4j.cas.config.CasConfiguration;
import org.pac4j.core.config.Config;
import org.pac4j.core.profile.CommonProfile;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

import io.buji.pac4j.filter.CallbackFilter;
import io.buji.pac4j.filter.SecurityFilter;
import io.buji.pac4j.realm.Pac4jRealm;
import io.buji.pac4j.subject.Pac4jPrincipal;
import lombok.RequiredArgsConstructor;

//@EnableCasClient
@SpringBootApplication
@RequiredArgsConstructor
public class CasClient1Application {

    private final CasProperties casProperties;

    public static void main(String[] args) {
        SpringApplication.run(CasClient1Application.class, args);
    }

    @Bean
    public Realm realm() {
        return new CustomRealm();
    }

    @Bean
    public Realm pac4jRealm() {
//        return new Pac4jRealm();
        String clientName = casProperties.getClientName();
        Objects.requireNonNull(clientName);
        return new CustomPac4jRealm(clientName);
    }

    @Bean
    @Primary
    public ShiroFilterFactoryBean shiroFilterFactoryBean(SecurityManager securityManager, Config config) {
        ShiroFilterFactoryBean filterFactoryBean = new ShiroFilterFactoryBean();

        filterFactoryBean.setLoginUrl("/login.html");
        filterFactoryBean.setSuccessUrl("/index.html");

        filterFactoryBean.setSecurityManager(securityManager);
        Map<String, String> map = new HashMap<>();
        map.put("/cas.html", "securityFilter");
        map.put("/callback", "callbackFilter");
        map.put("/**", "anon");
        filterFactoryBean.setFilterChainDefinitionMap(map);
        Map<String, Filter> filters = new HashedMap();
        filters.put("securityFilter", securityFilter(config));
        filters.put("callbackFilter", callbackFilter(config));
        filterFactoryBean.setFilters(filters);

        return filterFactoryBean;
    }

    //    @Bean
    public ShiroFilterChainDefinition shiroFilterChainDefinition() {
        DefaultShiroFilterChainDefinition chainDefinition = new DefaultShiroFilterChainDefinition();
        // all other paths require a logged in user
//        chainDefinition.addPathDefinition("/sign", "anon"); // all paths are managed via annotations
//        chainDefinition.addPathDefinition("/login.html", "anon"); // all paths are managed via annotations
//        chainDefinition.addPathDefinition("/**", "authc");
//        chainDefinition.addPathDefinition("/index.html", "anon");
        chainDefinition.addPathDefinition("/cas.html", "securityFilter");
        chainDefinition.addPathDefinition("/callback", "callbackFilter");
        chainDefinition.addPathDefinition("/**", "anona");
//        chainDefinition.addPathDefinition("/**", "anon");
        return chainDefinition;
    }

    @Bean
    public Config config() {
        CasConfiguration casConfiguration = new CasConfiguration(casProperties.getServerLoginUrl(), casProperties.getServerUrlPrefix());
        CasClient casClient = new CasClient(casConfiguration);
        casClient.setName(casProperties.getClientName());
        Config casConfig = new Config(casProperties.getClientHostUrl() + "/callback", casClient);
        return casConfig;
    }

    public SecurityFilter securityFilter(Config config) {
//        SecurityFilter securityFilter = new SecurityFilter();
        SecurityFilter securityFilter = new RestSecurityFilter();
        securityFilter.setConfig(config);
        securityFilter.setClients(casProperties.getClientName());
        return securityFilter;
    }

    public CallbackFilter callbackFilter(Config config) {
        CallbackFilter callbackFilter = new CallbackFilter();
        callbackFilter.setConfig(config);
        callbackFilter.setDefaultUrl("/index.html");
        return callbackFilter;
    }

    private static class CustomRealm extends AuthenticatingRealm {

        @Override
        protected AuthenticationInfo doGetAuthenticationInfo(AuthenticationToken authenticationToken) throws AuthenticationException {
            if (authenticationToken instanceof UsernamePasswordToken) {
                UsernamePasswordToken token = (UsernamePasswordToken) authenticationToken;
                String username = token.getUsername();
                //模拟数据操作
                if (Objects.equals(username, "u")) {
                    return new SimpleAuthenticationInfo(token.getPrincipal(), token.getCredentials(), getName());
                }
            }
            throw new AuthenticationException("login failure");
        }
    }

    private static class RestSecurityFilter extends SecurityFilter {
        @Override
        public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {
            if (SecurityUtils.getSubject().isAuthenticated()) {
                filterChain.doFilter(servletRequest, servletResponse);
                return;
            }
            super.doFilter(servletRequest, servletResponse, filterChain);
        }
    }

    @RequiredArgsConstructor
    private static class CustomPac4jRealm extends Pac4jRealm {

        private static final String ROLES_KEY = "roles";
        private static final String AUTH_KEY = "authorities";
        private final String appName;

        @Override
        protected AuthorizationInfo doGetAuthorizationInfo(final PrincipalCollection principals) {
            final Set<String> roles = new HashSet<>();
            final Set<String> permissions = new HashSet<>();
            final Pac4jPrincipal principal = principals.oneByType(Pac4jPrincipal.class);
            if (principal != null) {
                final List<CommonProfile> profiles = principal.getProfiles();
                for (CommonProfile profile : profiles) {
                    if (profile != null) {
                        roles.addAll(profile.getRoles());
                        permissions.addAll(profile.getPermissions());
                        //add roles and permissions from attributes
                        Object rolesAttr = profile.getAttribute(ROLES_KEY);
                        if (!Objects.isNull(rolesAttr)) {
                            if (rolesAttr instanceof String)
                                roles.add(Objects.toString(rolesAttr));
                            if (rolesAttr instanceof Collection<?>) {
                                roles.addAll((Collection<? extends String>) rolesAttr);
                            }
                        }
                        Object authsAttr = profile.getAttribute(AUTH_KEY);
                        if (!Objects.isNull(authsAttr)) {
                            if (authsAttr instanceof String && ((String) authsAttr).startsWith(appName)) {
                                permissions.add(((String) rolesAttr).substring(appName.length() + 1));
                            }
                            if (authsAttr instanceof Collection<?>) {
                                Collection<String> collect = (Collection<String>) authsAttr;
                                Set<String> auths = collect.stream().filter(auth -> auth.startsWith(appName))
                                        .map(auth -> auth.substring(appName.length() + 1))
                                        .collect(Collectors.toSet());
                                permissions.addAll(auths);
                            }
                        }
                    }
                }
            }

            final SimpleAuthorizationInfo simpleAuthorizationInfo = new SimpleAuthorizationInfo();
            simpleAuthorizationInfo.addRoles(roles);
            simpleAuthorizationInfo.addStringPermissions(permissions);
            return simpleAuthorizationInfo;
        }
    }
}
