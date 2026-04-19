package com.sanchitb.boardgame.api

import com.sanchitb.boardgame.api.dto.AuthUser
import com.sanchitb.boardgame.api.dto.SessionBundle
import com.sanchitb.boardgame.auth.userId
import com.sanchitb.boardgame.service.PlayerService
import com.sanchitb.boardgame.service.RecordService
import com.sanchitb.boardgame.service.UserService
import jakarta.servlet.http.HttpServletRequest
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/**
 * One-shot hydration endpoint hit by the iOS app immediately after it trades an
 * Apple ID token for a session JWT. Returns the authenticated user plus every
 * record and saved player they own, so the app can populate its UI without a
 * cascade of follow-up round-trips.
 *
 * Everything on this route is already scoped to [request.userId] via the
 * underlying services, so there's no chance of leaking another user's data.
 */
@RestController
@RequestMapping("/api/session")
class SessionController(
    private val users: UserService,
    private val players: PlayerService,
    private val records: RecordService,
) {

    @GetMapping
    fun current(request: HttpServletRequest): SessionBundle {
        val userId = request.userId()
        val user = users.requireById(userId)
        return SessionBundle(
            user = AuthUser(id = user.id, email = user.email, displayName = user.displayName),
            players = players.list(userId),
            records = records.list(userId, game = null, limit = 500),
        )
    }
}
