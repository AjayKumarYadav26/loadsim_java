package com.ktlo.simulator.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web MVC configuration for static resources and CORS.
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    /**
     * Configure static resource handlers for CSS, JS, images, and WebJars.
     */
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // Static resources (CSS, JS, images)
        registry.addResourceHandler("/css/**")
                .addResourceLocations("classpath:/static/css/");

        registry.addResourceHandler("/js/**")
                .addResourceLocations("classpath:/static/js/");

        registry.addResourceHandler("/images/**")
                .addResourceLocations("classpath:/static/images/");

        // WebJars (Bootstrap, jQuery, Font Awesome)
        registry.addResourceHandler("/webjars/**")
                .addResourceLocations("classpath:/META-INF/resources/webjars/");
    }

    /**
     * Configure CORS to allow frontend access from different origins (if needed).
     */
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOrigins("*")
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .maxAge(3600);

        registry.addMapping("/portal/**")
                .allowedOrigins("*")
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .maxAge(3600);
    }

    /**
     * Configure simple view controllers for direct page access.
     */
    @Override
    public void addViewControllers(ViewControllerRegistry registry) {
        // Redirect root to portal home
        registry.addRedirectViewController("/", "/portal");
    }
}
