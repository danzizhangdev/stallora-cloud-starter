package io.github.danzizhangdev.stallora.common.autoconfigure;

import io.github.danzizhangdev.stallora.common.error.GlobalExceptionHandler;
import io.github.danzizhangdev.stallora.common.trace.TraceIdResponseFilter;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.core.Ordered;

/**
 * Wires the shared web conventions into any servlet application that puts this
 * module on its classpath — no annotation or import needed on the host side.
 */
@AutoConfiguration
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
public class CommonWebAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public GlobalExceptionHandler stalloraGlobalExceptionHandler() {
        return new GlobalExceptionHandler();
    }

    @Bean
    public FilterRegistrationBean<TraceIdResponseFilter> stalloraTraceIdResponseFilter() {
        FilterRegistrationBean<TraceIdResponseFilter> reg = new FilterRegistrationBean<>(new TraceIdResponseFilter());
        reg.setOrder(Ordered.LOWEST_PRECEDENCE); // run innermost, after the tracing filter has populated MDC
        return reg;
    }
}
