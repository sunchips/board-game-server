package com.sanchitb.boardgame.auth

import com.fasterxml.jackson.databind.ObjectMapper
import com.sanchitb.boardgame.error.ApiError
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter

/**
 * Authenticates /api/ requests via `Authorization: Bearer <jwt>` (the JWT we
 * minted in [com.sanchitb.boardgame.api.AuthController]). Leaves the
 * auth-exchange endpoint open so clients can trade an Apple ID token for a
 * session.
 */
@Component
class JwtAuthFilter(
    private val jwtService: JwtService,
    private val objectMapper: ObjectMapper,
) : OncePerRequestFilter() {

    override fun shouldNotFilter(request: HttpServletRequest): Boolean {
        val path = request.requestURI
        return !path.startsWith("/api/") || OPEN_PATHS.any { path.startsWith(it) }
    }

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain,
    ) {
        val header = request.getHeader("Authorization")
        if (header.isNullOrBlank() || !header.startsWith(BEARER_PREFIX, ignoreCase = true)) {
            writeUnauthorized(response, "Missing `Authorization: Bearer <token>` header")
            return
        }
        val token = header.substring(BEARER_PREFIX.length).trim()
        val userId = try {
            jwtService.parse(token)
        } catch (e: InvalidSessionTokenException) {
            writeUnauthorized(response, e.message ?: "Invalid session token")
            return
        }
        request.setAttribute(CurrentUser.USER_ID_ATTR, userId)
        filterChain.doFilter(request, response)
    }

    private fun writeUnauthorized(response: HttpServletResponse, message: String) {
        response.status = HttpStatus.UNAUTHORIZED.value()
        response.contentType = MediaType.APPLICATION_JSON_VALUE
        val body = ApiError(
            status = HttpStatus.UNAUTHORIZED.value(),
            error = "Unauthorized",
            message = message,
            violations = emptyList(),
        )
        response.writer.write(objectMapper.writeValueAsString(body))
    }

    companion object {
        private const val BEARER_PREFIX = "Bearer "
        private val OPEN_PATHS = listOf("/api/auth/")
    }
}
