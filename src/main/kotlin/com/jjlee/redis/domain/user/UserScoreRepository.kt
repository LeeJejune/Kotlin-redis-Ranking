package com.jjlee.redis.domain.user

import org.springframework.data.jpa.repository.JpaRepository

interface UserScoreRepository : JpaRepository<UserScore, Long> {
    fun findByUserId(userId: Long) : UserScore
}