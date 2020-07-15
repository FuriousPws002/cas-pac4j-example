package com.example.client1;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import lombok.Data;

/**
 *
 */
@Data
@Configuration
@ConfigurationProperties("cas")
public class CasProperties {

    /**
     * CAS server URL E.g. https://example.com/cas or https://cas.example. Required.
     */
    private String serverUrlPrefix;

    /**
     * CAS server login URL E.g. https://example.com/cas/login or https://cas.example/login. Required.
     */
    private String serverLoginUrl;

    /**
     * CAS-protected client application host URL E.g. https://myclient.example.com Required.
     */
    private String clientHostUrl;

    private String clientName;
}
