package com.sanchitb.boardgame.auth

import jakarta.servlet.http.HttpServletRequest
import java.util.UUID

/**
 * Request-scoped helpers for getting the authenticated user id. The [JwtAuthFilter]
 * stashes the UUID on the request under [USER_ID_ATTR]; controllers read it back
 * via these extension functions to stay out of Spring Security's way.
 */
object CurrentUser {
    const val USER_ID_ATTR = "boardgame.user-id"
}

fun HttpServletRequest.userId(): UUID =
    getAttribute(CurrentUser.USER_ID_ATTR) as? UUID
        ?: error("No authenticated user on request — auth filter misconfigured")
