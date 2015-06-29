package no.nb.microservices.geotag.config;

import no.nb.microservices.geotag.repository.GeoTagRepository;
import no.nb.microservices.geotag.rest.controller.GlobalControllerExceptionHandler;
import org.mockito.Mockito;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.method.annotation.ExceptionHandlerMethodResolver;
import org.springframework.web.servlet.mvc.method.annotation.ExceptionHandlerExceptionResolver;
import org.springframework.web.servlet.mvc.method.annotation.ServletInvocableHandlerMethod;

import java.lang.reflect.Method;

/**
 * Created by Andreas Bjørnådal (andreasb) on 25.08.14.
 */
@Configuration
public class TestContext {

    @Bean
    public GeoTagRepository geoTagRepository() {
        return Mockito.mock(GeoTagRepository.class);
    }

    @Bean
    public ApplicationSettings applicationSettings() {
        ApplicationSettings settings = new ApplicationSettings();
        settings.setNbsokContentUrl("http://www.nb.no/nbsok/nb/{sesamid}");
        settings.setFotoContentUrl("http://www.nb.no/foto/nb/{sesamid}");
        return settings;
    }

    public static ExceptionHandlerExceptionResolver createExceptionResolver() {
        ExceptionHandlerExceptionResolver exceptionResolver = new ExceptionHandlerExceptionResolver() {
            protected ServletInvocableHandlerMethod getExceptionHandlerMethod(HandlerMethod handlerMethod, Exception exception) {
                Method method = new ExceptionHandlerMethodResolver(GlobalControllerExceptionHandler.class).resolveMethod(exception);
                return new ServletInvocableHandlerMethod(new GlobalControllerExceptionHandler(), method);
            }
        };
        exceptionResolver.afterPropertiesSet();
        return exceptionResolver;
    }

}
