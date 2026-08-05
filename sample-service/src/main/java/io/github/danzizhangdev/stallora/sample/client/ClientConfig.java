package io.github.danzizhangdev.stallora.sample.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.support.RestClientAdapter;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;

@Configuration
public class ClientConfig {

    /** The base URL is a plain service name in both deployments: DNS is the discovery mechanism. */
    @Bean
    AuthKeysClient authKeysClient(@Value("${stallora.sample.auth-base-url}") String baseUrl) {
        RestClient rc = RestClient.builder().baseUrl(baseUrl).build();
        return HttpServiceProxyFactory.builderFor(RestClientAdapter.create(rc)).build()
                .createClient(AuthKeysClient.class);
    }
}
