package com.utetra.config;

import jakarta.servlet.DispatcherType;
import org.sitemesh.builder.SiteMeshFilterBuilder;
import org.sitemesh.config.ConfigurableSiteMeshFilter;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SiteMeshConfig {
    public static class LayoutFilter extends ConfigurableSiteMeshFilter {
        @Override protected void applyCustomConfiguration(SiteMeshFilterBuilder builder) {
            // These mappings are full servlet paths; SiteMesh 3.2.2 otherwise
            // prepends /WEB-INF/decorators/ to every decorator path.
            builder.setDecoratorPrefix("");
            builder.addDecoratorPath("/*", "/WEB-INF/views/layouts/app.jsp")
                .addExcludedPath("/api/*").addExcludedPath("/assets/*")
                .addExcludedPath("/ws").addExcludedPath("/ws/*")
                .addExcludedPath("/payments/*").addExcludedPath("/error");
        }
    }
    @Bean public FilterRegistrationBean<LayoutFilter> siteMeshFilter() {
        var bean = new FilterRegistrationBean<>(new LayoutFilter());
        bean.addUrlPatterns("/*");
        bean.setDispatcherTypes(DispatcherType.REQUEST);
        bean.setOrder(1);
        return bean;
    }
}
