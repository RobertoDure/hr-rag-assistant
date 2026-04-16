package ie.com.rag.config;

import org.springframework.boot.web.client.RestClientCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;

@Configuration(proxyBeanMethods = false)
public class HttpClientConfig {

    @Bean
    public RestClientCustomizer jdkClientRestClientCustomizer() {
        return builder -> builder.requestFactory(new JdkClientHttpRequestFactory());
    }
}
