package com.example.investcalc.client.retry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.function.Supplier;

/**
 * Timeouts for RestClient and a simple retry helper used by MOEX / Sector clients.
 */
@Configuration
public class RetryConfig {

    private static final Logger log = LoggerFactory.getLogger(RetryConfig.class);

    @Value("${marketdata.connect-timeout-ms:5000}")
    private int connectTimeoutMs;

    @Value("${marketdata.read-timeout-ms:30000}")
    private int readTimeoutMs;

    @Value("${marketdata.retry.max-attempts:3}")
    private int maxAttempts;

    @Value("${marketdata.retry.backoff-ms:500}")
    private long backoffMs;

    @Bean
    public ClientHttpRequestFactory moexRequestFactory() {
        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofMillis(connectTimeoutMs))
                .build();
        JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(httpClient);
        factory.setReadTimeout(Duration.ofMillis(readTimeoutMs));
        return factory;
    }

    @Bean
    public RestClient.Builder restClientBuilder(ClientHttpRequestFactory moexRequestFactory) {
        return RestClient.builder().requestFactory(moexRequestFactory);
    }

    /**
     * Execute supplier with retries on RestClientException / IO-related failures.
     */
    public <T> T executeWithRetry(Supplier<T> action, String context) {
        int attempt = 0;
        RuntimeException last = null;
        while (attempt < maxAttempts) {
            attempt++;
            try {
                return action.get();
            } catch (RestClientException | IllegalStateException e) {
                last = e instanceof RuntimeException re ? re : new RuntimeException(e);
                log.warn("Attempt {}/{} failed for {}: {}", attempt, maxAttempts, context, e.getMessage());
                if (attempt < maxAttempts) {
                    try {
                        Thread.sleep(backoffMs * attempt);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw new RuntimeException("Retry interrupted: " + context, ie);
                    }
                }
            }
        }
        throw last != null ? last : new RuntimeException("Retry failed: " + context);
    }

    public int getMaxAttempts() {
        return maxAttempts;
    }

    public long getBackoffMs() {
        return backoffMs;
    }
}
