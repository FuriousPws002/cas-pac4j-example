package com.example.client1;

import java.util.Collections;
import java.util.Map;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.shiro.SecurityUtils;
import org.apache.shiro.authc.UsernamePasswordToken;
import org.apache.shiro.authz.annotation.RequiresRoles;
import org.pac4j.cas.client.rest.CasRestFormClient;
import org.pac4j.cas.config.CasConfiguration;
import org.pac4j.cas.credentials.authenticator.CasRestAuthenticator;
import org.pac4j.cas.profile.CasProfile;
import org.pac4j.cas.profile.CasRestProfile;
import org.pac4j.core.context.JEEContext;
import org.pac4j.core.context.WebContext;
import org.pac4j.core.credentials.TokenCredentials;
import org.pac4j.core.credentials.UsernamePasswordCredentials;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import io.buji.pac4j.token.Pac4jToken;
import lombok.RequiredArgsConstructor;

/**
 *
 */
@RestController
@RequiredArgsConstructor
public class TestController {

    private final CasProperties casProperties;

    @PostMapping("sign")
    public void login(String username, String password) {
        System.err.println("login method ");
        SecurityUtils.getSubject().login(new UsernamePasswordToken(username, password));
    }

    @PostMapping("login-cas")
    public void loginCas(HttpServletRequest request,
                         HttpServletResponse response,
                         String username, String password) {
        final String serviceUrl = casProperties.getClientHostUrl();
        CasConfiguration casConfiguration = new CasConfiguration(casProperties.getServerLoginUrl());
        final CasRestAuthenticator authenticator = new CasRestAuthenticator(casConfiguration);
        final CasRestFormClient client = new CasRestFormClient();
        client.setConfiguration(casConfiguration);

        final WebContext webContext = new JEEContext(request, response);
//        casConfiguration.init(webContext);
        UsernamePasswordCredentials credentials = new UsernamePasswordCredentials(username, password);
        CasRestAuthenticator restAuthenticator = new CasRestAuthenticator(casConfiguration);
        // authenticate with credentials (validate credentials)
        restAuthenticator.validate(credentials, webContext);
        final CasRestProfile profile = (CasRestProfile) credentials.getUserProfile();
        // get service ticket
        final TokenCredentials casCredentials = client.requestServiceTicket(serviceUrl, profile, webContext);
        // validate service ticket
        final CasProfile casProfile = client.validateServiceTicket(serviceUrl, casCredentials, webContext);
        Map<String, Object> attributes = casProfile.getAttributes();
        Set<Map.Entry<String, Object>> mapEntries = attributes.entrySet();
        for (Map.Entry entry : mapEntries) {
            System.out.println(entry.getKey() + ":" + entry.getValue());
        }
        System.err.println(casProfile);
        Pac4jToken token = new Pac4jToken(Collections.singletonList(casProfile), false);
        SecurityUtils.getSubject().login(token);
//        client.destroyTicketGrantingTicket(profile,webContext);
        System.err.println("login cas success");
    }

    @GetMapping("user")
    public Object user() {
        Object principal = SecurityUtils.getSubject().getPrincipal();
        System.err.println(principal);
        return principal;
    }

    @GetMapping("test")
    @RequiresRoles("test")
    public Object test() {
        return "TEST";
    }
}
