package no.nb.microservices.geotag.config;

import no.nb.nbsecurity.config.SecurityConfig;
import no.nb.pilt.client.SessionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.annotation.web.servlet.configuration.EnableWebMvcSecurity;
import org.springframework.security.web.authentication.preauth.AbstractPreAuthenticatedProcessingFilter;

/**
 * Created by Andreas Bjørnådal (andreasb) on 20.08.14.
 */
@Configuration
@EnableWebMvcSecurity
@EnableWebSecurity
@Order(1)
@Import(SecurityConfig.class)
public class TestWebSecurityConfig extends WebSecurityConfigurerAdapter {
    @Autowired
    private SecurityConfig securityConfig;

    @Autowired
    private SessionService sessionService;

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        http
                .addFilterBefore(securityConfig.preAuthenticatedProcessingFilter(sessionService), AbstractPreAuthenticatedProcessingFilter.class)
                .authorizeRequests()
                .anyRequest().permitAll()
                .and()
                .httpBasic()
                .and()
                .exceptionHandling()
                .accessDeniedHandler(securityConfig.accessDeniedHandler())
                .and()
                .logout().logoutUrl("/j_spring_security_logout")
                .logoutSuccessHandler(securityConfig.simpleUrlLogoutSuccessHandler());
    }

}