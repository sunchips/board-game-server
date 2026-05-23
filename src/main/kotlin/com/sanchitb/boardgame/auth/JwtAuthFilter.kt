package com.sanchitb.boardgame.auth

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter

/**
 * Optional auth: if an `Authorization: Bearer <jwt>` header is present and valid,
 * stash a [CurrentUser] on the request. Invalid tokens are ignored (treated as
 * anonymous) — the ApiKeyFilter is the gate for whether the request reaches the
 * controller at all. Endpoints that want to require sign-in can read the request
 * attribute and 401 themselves.
 */
@Component
class JwtAuthFilter(private val jwtService: JwtService) : OncePerRequestFilter() {

    override fun shouldNotFilter(request: HttpServletRequest): Boolean =
        !request.requestURI.startsWith("/api/")

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain,
    ) {
        val header = request.getHeader("Authorization")
        if (header != null && header.startsWith(BEARER_PREFIX)) {
            val token = header.removePrefix(BEARER_PREFIX).trim()
            if (token.isNotEmpty()) {
                runCatching { jwtService.parse(token) }
                    .onSuccess { p ->
                        request.setAttribute(
                            CurrentUser.REQUEST_ATTRIBUTE,
                            CurrentUser(userId = p.userId, appleSub = p.appleSub),
                        )
                    }
            }
        }
        filterChain.doFilter(request, response)
    }

    private companion object {
        const val BEARER_PREFIX = "Bearer "
    }
}
