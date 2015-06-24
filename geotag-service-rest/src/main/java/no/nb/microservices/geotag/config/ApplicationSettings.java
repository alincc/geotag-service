package no.nb.microservices.geotag.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "microservice")
public class ApplicationSettings {
    private String nbsokContentUrl;
    private String fotoContentUrl;

    public String getNbsokContentUrl() {
        return nbsokContentUrl;
    }

    public void setNbsokContentUrl(String nbsokContentUrl) {
        this.nbsokContentUrl = nbsokContentUrl;
    }

    public String getFotoContentUrl() {
        return fotoContentUrl;
    }

    public void setFotoContentUrl(String fotoContentUrl) {
        this.fotoContentUrl = fotoContentUrl;
    }
}
