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
import java.util.HashMap;
import java.util.Map;

/**
 * Redis caching configuration
 */
@Configuration
@EnableCaching
public class CacheConfig {

    @Bean
    public RedisCacheManager cacheManager(RedisConnectionFactory connectionFactory) {
        RedisCacheConfiguration defaultConfig = RedisCacheConfiguration.defaultCacheConfig()
            .entryTtl(Duration.ofHours(1))
            .serializeKeysWith(
                RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer())
            )
            .serializeValuesWith(
                RedisSerializationContext.SerializationPair.fromSerializer(new GenericJackson2JsonRedisSerializer())
            );

        // Configure cache-specific TTLs
        Map<String, RedisCacheConfiguration> cacheConfigurations = new HashMap<>();

        // Cache for candidate searches - 30 minutes
        cacheConfigurations.put("candidates", defaultConfig.entryTtl(Duration.ofMinutes(30)));

        // Cache for job analyses - 1 hour
        cacheConfigurations.put("jobAnalyses", defaultConfig.entryTtl(Duration.ofHours(1)));

        // Cache for vector searches - 15 minutes (more dynamic)
        cacheConfigurations.put("vectorSearch", defaultConfig.entryTtl(Duration.ofMinutes(15)));

        // Cache for Q&A responses - 2 hours
        cacheConfigurations.put("qaResponses", defaultConfig.entryTtl(Duration.ofHours(2)));

        return RedisCacheManager.builder(connectionFactory)
            .cacheDefaults(defaultConfig)
            .withInitialCacheConfigurations(cacheConfigurations)
            .build();
    }
}

