package com.sanchitb.boardgame.repo

import com.sanchitb.boardgame.domain.UserEntity
import org.springframework.data.jpa.repository.JpaRepository
import java.util.Optional
import java.util.UUID

interface UserRepository : JpaRepository<UserEntity, UUID> {
    fun findByAppleSub(appleSub: String): Optional<UserEntity>
}
