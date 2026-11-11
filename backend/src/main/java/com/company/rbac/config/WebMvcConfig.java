package com.company.rbac.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.method.HandlerTypePredicate;
import org.springframework.web.servlet.config.annotation.PathMatchConfigurer;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Override
    public void configurePathMatch(PathMatchConfigurer configurer) {
        // Keep the existing /api contract while serving the Vue app from /.
        configurer.addPathPrefix("/api", HandlerTypePredicate.forAnnotation(RestController.class));
    }

    @Override
    public void addViewControllers(ViewControllerRegistry registry) {
        // Vue Router uses HTML5 history. These page URLs must return index.html on refresh.
        String[] pagePaths = {
                "/login", "/register", "/settings", "/profile",
                "/users", "/roles", "/menus", "/departments", "/positions", "/data",
                "/logs", "/logs/login", "/logs/operation",
                "/monitor", "/monitor/server", "/monitor/online",
                "/business", "/business/products", "/business/orders",
                "/files", "/images", "/attachments", "/videos"
        };
        for (String path : pagePaths) {
            registry.addViewController(path).setViewName("forward:/index.html");
        }
    }
}
