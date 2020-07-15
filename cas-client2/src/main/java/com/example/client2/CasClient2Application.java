package com.example.client2;

import java.util.Collections;

import org.jasig.cas.client.boot.configuration.EnableCasClient;
import org.jasig.cas.client.session.SingleSignOutFilter;
import org.jasig.cas.client.session.SingleSignOutHttpSessionListener;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.boot.web.servlet.ServletListenerRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.core.Ordered;

@EnableCasClient
@SpringBootApplication
public class CasClient2Application {

    public static void main(String[] args) {
        SpringApplication.run(CasClient2Application.class, args);
    }

//    @Bean
    public FilterRegistrationBean logoutFilter() {
        FilterRegistrationBean bean = new FilterRegistrationBean();
        bean.setFilter(new SingleSignOutFilter());
        bean.setOrder(Ordered.HIGHEST_PRECEDENCE);
        bean.setUrlPatterns(Collections.singleton("/*"));
        return bean;
    }

//    @Bean
    public ServletListenerRegistrationBean logoutListener() {
        return new ServletListenerRegistrationBean(new SingleSignOutHttpSessionListener());
    }
}
