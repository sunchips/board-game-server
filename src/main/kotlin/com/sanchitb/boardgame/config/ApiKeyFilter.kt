package com.sanchitb.boardgame.config

import com.fasterxml.jackson.databind.ObjectMapper
import com.sanchitb.boardgame.error.ApiError
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter

@Component
class ApiKeyFilter(
    @Value("\${api.key}") private val expectedKey: String,
    private val objectMapper: ObjectMapper,
) : OncePerRequestFilter() {

    override fun shouldNotFilter(request: HttpServletRequest): Boolean {
        val path = request.requestURI
        return !path.startsWith("/api/")
    }

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain,
    ) {
        val provided = request.getHeader(HEADER)
        if (provided.isNullOrEmpty() || provided != expectedKey) {
            response.status = HttpStatus.UNAUTHORIZED.value()
            response.contentType = MediaType.APPLICATION_JSON_VALUE
            val body = ApiError(
                status = HttpStatus.UNAUTHORIZED.value(),
                error = "Unauthorized",
                message = "Missing or invalid $HEADER header",
                violations = emptyList(),
            )
            response.writer.write(objectMapper.writeValueAsString(body))
            return
        }
        filterChain.doFilter(request, response)
    }

    companion object {
        const val HEADER = "X-API-Key"
    }
}
