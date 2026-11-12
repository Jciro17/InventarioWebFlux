package co.edu.uco.inventario.adaptador;

import org.apache.pulsar.client.api.PulsarClient;
import org.apache.pulsar.client.api.PulsarClientException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class PulsarConfig {

    // destroyMethod = close: Spring cierra el cliente al apagar la aplicación.
    @Bean(destroyMethod = "close")
    public PulsarClient pulsarClient(@Value("${pulsar.service-url}") String serviceUrl)
            throws PulsarClientException {
        return PulsarClient.builder()
                .serviceUrl(serviceUrl)
                .build();
    }
}
