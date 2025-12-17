package io.github.ramils.autoconfigure;

import io.github.ramils.core.TestData;
import io.github.ramils.metadata.MetadataProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingClass;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Auto-configuration for Test Data Builder.
 * <p>
 * Automatically registers the appropriate MetadataProvider based on classpath:
 * <ul>
 *   <li>If JPA (@Entity) is present, uses JpaMetadataProvider</li>
 *   <li>If Spring Data JDBC (@Table) is present (without JPA), uses JdbcMetadataProvider</li>
 * </ul>
 */
@AutoConfiguration
public class TestDataAutoConfiguration {

    /**
     * Configuration for JPA environments.
     */
    @Configuration
    @ConditionalOnClass(name = "jakarta.persistence.Entity")
    public static class JpaConfiguration {

        @Bean
        public TestDataProviderInitializer jpaProviderInitializer() {
            return new TestDataProviderInitializer("io.github.ramils.jpa.JpaMetadataProvider");
        }
    }

    /**
     * Configuration for Spring Data JDBC environments (when JPA is not present).
     */
    @Configuration
    @ConditionalOnClass(name = "org.springframework.data.relational.core.mapping.Table")
    @ConditionalOnMissingClass("jakarta.persistence.Entity")
    public static class JdbcConfiguration {

        @Bean
        public TestDataProviderInitializer jdbcProviderInitializer() {
            return new TestDataProviderInitializer("io.github.ramils.jdbc.JdbcMetadataProvider");
        }
    }

    /**
     * Initializer that registers a MetadataProvider on construction.
     */
    public static class TestDataProviderInitializer {

        public TestDataProviderInitializer(String providerClassName) {
            try {
                Class<?> providerClass = Class.forName(providerClassName);
                MetadataProvider provider = (MetadataProvider) providerClass
                        .getDeclaredConstructor()
                        .newInstance();
                TestData.registerProvider(provider);
            } catch (Exception e) {
                // Provider module not on classpath, ignore
            }
        }
    }
}
