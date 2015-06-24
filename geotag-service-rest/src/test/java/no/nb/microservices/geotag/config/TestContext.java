package no.nb.microservices.geotag.config;

import no.nb.microservices.geotag.repository.GeoTagRepository;
import org.mockito.Mockito;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

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

}
