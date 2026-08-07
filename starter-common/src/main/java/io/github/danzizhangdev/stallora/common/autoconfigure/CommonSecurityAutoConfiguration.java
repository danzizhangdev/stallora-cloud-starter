package io.github.danzizhangdev.stallora.common.autoconfigure;

import io.github.danzizhangdev.stallora.common.error.AccessDeniedAdvice;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Bean;

/**
 * Separate from {@link CommonWebAutoConfiguration} because spring-security is an optional
 * dependency of this module: the advice only makes sense (and only compiles its imports)
 * when Spring Security is on the host's classpath.
 */
@AutoConfiguration
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@ConditionalOnClass(name = "org.springframework.security.access.AccessDeniedException")
public class CommonSecurityAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public AccessDeniedAdvice stalloraAccessDeniedAdvice() {
        return new AccessDeniedAdvice();
    }
}
