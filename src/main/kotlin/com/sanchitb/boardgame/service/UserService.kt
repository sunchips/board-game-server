package com.sanchitb.boardgame.service

import com.sanchitb.boardgame.auth.AppleIdentity
import com.sanchitb.boardgame.domain.UserEntity
import com.sanchitb.boardgame.error.RecordNotFoundException
import com.sanchitb.boardgame.repo.UserRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class UserService(private val users: UserRepository) {

    /**
     * Upsert by `apple_sub`. Apple only sends `email` / `displayName` on the
     * very first sign-in, so we merge non-null values into whatever we have.
     *
     * The returned [UpsertResult.isNew] lets the client tell a first-time signup
     * apart from a returning login (useful for welcome UX, analytics).
     */
    @Transactional
    fun upsertFromApple(identity: AppleIdentity, displayName: String?): UpsertResult {
        val existing = users.findByAppleSub(identity.sub).orElse(null)
        if (existing != null) {
            if (identity.email != null && existing.email != identity.email) {
                existing.email = identity.email
            }
            if (!displayName.isNullOrBlank() && existing.displayName != displayName) {
                existing.displayName = displayName
            }
            return UpsertResult(users.save(existing), isNew = false)
        }
        val created = users.save(
            UserEntity(
                appleSub = identity.sub,
                email = identity.email,
                displayName = displayName?.takeIf { it.isNotBlank() },
            ),
        )
        return UpsertResult(created, isNew = true)
    }

    @Transactional(readOnly = true)
    fun requireById(id: UUID): UserEntity =
        users.findById(id).orElseThrow { RecordNotFoundException("user: $id") }

    data class UpsertResult(val user: UserEntity, val isNew: Boolean)
}
