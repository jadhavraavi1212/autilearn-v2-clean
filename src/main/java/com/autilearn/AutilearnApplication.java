package com.autilearn;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.*;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;

@SpringBootApplication
public class AutilearnApplication {

    public static void main(String[] args) {
        SpringApplication.run(AutilearnApplication.class, args);
        System.out.println("\n=============================================");
        System.out.println("  AutiLearn Server Started Successfully!");
        System.out.println("  Home          : http://localhost:8080");
        System.out.println("  Child Login   : http://localhost:8080/pages/login-child.html");
        System.out.println("  Parent Login  : http://localhost:8080/pages/login-parent.html");
        System.out.println("  Register      : http://localhost:8080/pages/register.html");
        System.out.println("  Pricing       : http://localhost:8080/pages/pricing.html");
        System.out.println("  Health Check  : http://localhost:8080/api/health");
        System.out.println("=============================================\n");
    }

    @Configuration
    public static class WebConfig implements WebMvcConfigurer {

        @Override
        public void addCorsMappings(CorsRegistry registry) {
            registry.addMapping("/**")
                    .allowedOriginPatterns("*")
                    .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                    .allowedHeaders("*")
                    .allowCredentials(false);
        }

        @Override
        public void addResourceHandlers(ResourceHandlerRegistry registry) {
            // Serve everything under static/ at the root URL
            registry.addResourceHandler("/**")
                    .addResourceLocations("classpath:/static/")
                    .setCachePeriod(0);
            // Explicitly serve /pages/** subfolder
            registry.addResourceHandler("/pages/**")
                    .addResourceLocations("classpath:/static/pages/")
                    .setCachePeriod(0);
            registry.addResourceHandler("/css/**")
                    .addResourceLocations("classpath:/static/css/");
            registry.addResourceHandler("/js/**")
                    .addResourceLocations("classpath:/static/js/");
        }

        @Override
        public void addViewControllers(ViewControllerRegistry registry) {
            // Root → home page
            registry.addViewController("/").setViewName("forward:/index.html");
            registry.addViewController("/index").setViewName("forward:/index.html");
            // Shortcut URLs for convenience
            registry.addViewController("/login").setViewName("forward:/pages/login-parent.html");
            registry.addViewController("/register").setViewName("forward:/pages/register.html");
            registry.addViewController("/pricing").setViewName("forward:/pages/pricing.html");
        }
    }

    @Controller
    public static class HealthController {
        @GetMapping("/api/health")
        @ResponseBody
        public String health() {
            return "{\"status\":\"UP\",\"message\":\"AutiLearn server is running!\"}";
        }
    }
}
