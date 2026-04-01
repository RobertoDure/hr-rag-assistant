package ie.com.rag.config;

import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;
import java.util.Map;

@Configuration(proxyBeanMethods = false)
@EnableCaching
public class CacheConfig {

    @Bean
    public RedisCacheManager cacheManager(final RedisConnectionFactory connectionFactory) {
        RedisCacheConfiguration defaultConfig = RedisCacheConfiguration.defaultCacheConfig()
            .entryTtl(Duration.ofHours(1))
            .serializeKeysWith(
                RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer())
            )
            .serializeValuesWith(
                RedisSerializationContext.SerializationPair.fromSerializer(new GenericJackson2JsonRedisSerializer())
            );

        Map<String, RedisCacheConfiguration> cacheConfigurations = Map.of(
            "candidates", defaultConfig.entryTtl(Duration.ofMinutes(30)),
            "jobAnalyses", defaultConfig.entryTtl(Duration.ofHours(1)),
            "vectorSearch", defaultConfig.entryTtl(Duration.ofMinutes(15)),
            "qaResponses", defaultConfig.entryTtl(Duration.ofHours(2))
        );

        return RedisCacheManager.builder(connectionFactory)
            .cacheDefaults(defaultConfig)
            .withInitialCacheConfigurations(cacheConfigurations)
            .build();
    }
}

