package com.sanchitb.boardgame.auth

import java.util.UUID

/**
 * Per-request principal resolved by [JwtAuthFilter] from the `Authorization: Bearer`
 * header and stashed as a request attribute. Absent for anonymous calls — record
 * creation still works without a session token, the record is just unowned.
 */
data class CurrentUser(
    val userId: UUID,
    val appleSub: String,
) {
    companion object {
        const val REQUEST_ATTRIBUTE = "com.sanchitb.boardgame.currentUser"
    }
}
