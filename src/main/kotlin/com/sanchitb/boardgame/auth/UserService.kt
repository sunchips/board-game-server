package com.sanchitb.boardgame.auth

import com.sanchitb.boardgame.domain.UserEntity
import com.sanchitb.boardgame.repo.UserRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class UserService(private val users: UserRepository) {

    /**
     * Find by Apple's stable `sub` (subject) claim, or create a fresh user. Apple only
     * sends the user's name/email on the FIRST sign-in for a given Apple ID — on
     * subsequent sign-ins those fields come back null in the identity token, so we
     * only overwrite when the incoming value is non-null.
     */
    @Transactional
    fun findOrCreate(appleSub: String, email: String?, name: String?): UserEntity {
        val existing = users.findByAppleSub(appleSub)
        if (existing != null) {
            var dirty = false
            if (email != null && existing.email != email) {
                existing.email = email
                dirty = true
            }
            if (name != null && existing.name != name) {
                existing.name = name
                dirty = true
            }
            return if (dirty) users.save(existing) else existing
        }
        return users.save(UserEntity(appleSub = appleSub, email = email, name = name))
    }
}
