package com.sanchitb.boardgame.domain

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.hibernate.annotations.CreationTimestamp
import org.hibernate.annotations.UpdateTimestamp
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "users")
class UserEntity(
    @Id
    @Column(name = "id", nullable = false, updatable = false)
    var id: UUID = UUID.randomUUID(),

    @Column(name = "apple_sub", nullable = false, unique = true, updatable = false)
    var appleSub: String = "",

    @Column(name = "email")
    var email: String? = null,

    @Column(name = "display_name")
    var displayName: String? = null,

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    var createdAt: Instant? = null,

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    var updatedAt: Instant? = null,
)
