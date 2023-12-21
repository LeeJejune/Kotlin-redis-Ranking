package com.jjlee.redis.service.user

import com.jjlee.redis.domain.user.User
import com.jjlee.redis.domain.user.UserRepository
import com.jjlee.redis.dto.request.CreateUserRequest
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional
class UserService(
    private val userRepository: UserRepository
) {

    fun saveUser(req: CreateUserRequest){
        userRepository.save(req.toUserEntity())
    }

    @Transactional(readOnly = true)
    fun getUser(userId: Long) : User {
        return userRepository.findByIdOrNull(userId) ?: throw IllegalArgumentException("존재 하지 않는 유저!")
    }

    @Transactional(readOnly = true)
    fun getUsers() : List<User> {
        return userRepository.findAll()
    }

}