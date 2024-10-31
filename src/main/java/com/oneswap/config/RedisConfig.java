package com.oneswap.config;

import io.lettuce.core.ClientOptions;
import io.lettuce.core.SslOptions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceClientConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;

@Configuration
public class RedisConfig {

    @Value("${spring.data.redis.port}")
    private int port;

    @Value("${spring.data.redis.host}")
    private String host;

    @Value("${spring.data.redis.password}")
    private String password;

    @Value("${spring.data.redis.timeout}")
    private Integer timeout;

    @Value("${redis.ssl.enable}")
    private boolean sslEnable;

    @Bean
    public RedisConnectionFactory redisConnectionFactory() {
        RedisStandaloneConfiguration redisStandaloneConfiguration = new RedisStandaloneConfiguration(host, port);
        redisStandaloneConfiguration.setPassword(password);
        LettuceClientConfiguration clientConfig;
        if (sslEnable) {
            clientConfig = LettuceClientConfiguration.builder()
                    .commandTimeout(Duration.ofMillis(timeout))
                    .shutdownTimeout(Duration.ofMillis(100))
                    .clientOptions(ClientOptions.builder()
                            .sslOptions(SslOptions.builder().build())
                            .build())
                    .useSsl()
                    .build();
        } else {
            clientConfig = LettuceClientConfiguration.builder()
                    .commandTimeout(Duration.ofMillis(timeout))
                    .shutdownTimeout(Duration.ofMillis(100))
                    .build();
        }
        return new LettuceConnectionFactory(redisStandaloneConfiguration, clientConfig);
    }

    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);
        template.setDefaultSerializer(new StringRedisSerializer());
        template.afterPropertiesSet();
        return template;
    }
}
