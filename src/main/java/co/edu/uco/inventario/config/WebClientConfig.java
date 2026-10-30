package co.edu.uco.inventario.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class WebClientConfig {
    @Bean
    public WebClient messageUcoLabWebClient(MessageUcoLabProperties props) {
        return WebClient.builder()
                .baseUrl(props.getBackendUrl())
                .defaultHeader("Accept", "application/json")
                .build();
    }
}
