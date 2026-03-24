package com.dronetrack.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI droneTrackOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("DroneTrack API")
                        .description("Sistema de monitorización de aeronaves y detección de intrusiones en zonas restringidas")
                        .version("1.0.0"));
    }
}
