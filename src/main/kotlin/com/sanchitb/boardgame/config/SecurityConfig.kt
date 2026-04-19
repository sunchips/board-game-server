package com.sanchitb.boardgame.config

import com.sanchitb.boardgame.auth.JwtAuthFilter
import org.springframework.boot.web.servlet.FilterRegistrationBean
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class SecurityConfig {

    @Bean
    fun jwtAuthFilterRegistration(filter: JwtAuthFilter): FilterRegistrationBean<JwtAuthFilter> {
        val registration = FilterRegistrationBean(filter)
        registration.addUrlPatterns("/api/*")
        registration.order = 10
        return registration
    }
}
