package com.millsystem.config;

import com.millsystem.feature.FeatureAccessInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final FeatureAccessInterceptor featureAccessInterceptor;

    public WebConfig(FeatureAccessInterceptor featureAccessInterceptor) {
        this.featureAccessInterceptor = featureAccessInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(featureAccessInterceptor).addPathPatterns("/api/**");
    }
}
