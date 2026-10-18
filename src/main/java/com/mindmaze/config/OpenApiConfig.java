package com.mindmaze.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Title and description shown at the top of the Swagger UI page. */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI mindMazeOpenApi() {
        return new OpenAPI().info(new Info()
                .title("Mind Maze API")
                .version("1.0.0")
                .description("Adaptive brain-challenge API: logic, patterns, memory, codes and maths."));
    }
}
