package com.sanchitb.boardgame.config

import com.sanchitb.boardgame.auth.JwtAuthFilter
import org.springframework.boot.web.servlet.FilterRegistrationBean
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class SecurityConfig {

    @Bean
    fun apiKeyFilterRegistration(filter: ApiKeyFilter): FilterRegistrationBean<ApiKeyFilter> {
        val registration = FilterRegistrationBean(filter)
        registration.addUrlPatterns("/api/*")
        registration.order = 10
        return registration
    }

    @Bean
    fun jwtAuthFilterRegistration(filter: JwtAuthFilter): FilterRegistrationBean<JwtAuthFilter> {
        val registration = FilterRegistrationBean(filter)
        registration.addUrlPatterns("/api/*")
        // After the API-key gate so we don't bother parsing JWTs for unauthorised requests.
        registration.order = 20
        return registration
    }
}
