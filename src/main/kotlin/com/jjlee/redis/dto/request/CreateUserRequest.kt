package com.jjlee.redis.dto.request

import com.jjlee.redis.domain.user.User

data class CreateUserRequest(
    val name: String
) {
    fun toUserEntity() : User {
        return User(name)
    }
}