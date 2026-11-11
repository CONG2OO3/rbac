package com.company.rbac.config;

import cn.dev33.satoken.interceptor.SaInterceptor;
import cn.dev33.satoken.router.SaRouter;
import cn.dev33.satoken.stp.StpUtil;
import com.company.rbac.common.exception.BusinessException;
import com.company.rbac.system.mapper.SysUserRoleMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@RequiredArgsConstructor
public class SecurityConfig implements WebMvcConfigurer {

    private final SysUserRoleMapper userRoleMapper;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new SaInterceptor(handler -> {
                    SaRouter.match("/api/**")
                            .notMatch(
                                    "/api/auth/login",
                                    "/api/auth/register",
                                    "/api/auth/captcha",
                                    "/api/public/config")
                            .check(router -> StpUtil.checkLogin());
                    SaRouter.match("/api/system/**", "/api/business/**")
                            .check(router -> {
                                if (!userRoleMapper.selectRoleKeys(
                                        StpUtil.getLoginIdAsLong()).contains("admin")) {
                                    throw new BusinessException(403, "无权访问管理接口");
                                }
                            });
                }))
                .addPathPatterns("/api/**");
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOriginPatterns("http://localhost:*", "http://127.0.0.1:*")
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true);
    }
}
