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
     * Upserts by `apple_sub`. Apple only sends `email` / `displayName` on the
     * very first sign-in, so we merge non-null values into whatever we have.
     */
    @Transactional
    fun upsertFromApple(identity: AppleIdentity, displayName: String?): UserEntity {
        val existing = users.findByAppleSub(identity.sub).orElse(null)
        if (existing != null) {
            if (identity.email != null && existing.email != identity.email) {
                existing.email = identity.email
            }
            if (!displayName.isNullOrBlank() && existing.displayName != displayName) {
                existing.displayName = displayName
            }
            return users.save(existing)
        }
        return users.save(
            UserEntity(
                appleSub = identity.sub,
                email = identity.email,
                displayName = displayName?.takeIf { it.isNotBlank() },
            ),
        )
    }

    @Transactional(readOnly = true)
    fun requireById(id: UUID): UserEntity =
        users.findById(id).orElseThrow { RecordNotFoundException("user: $id") }
}
