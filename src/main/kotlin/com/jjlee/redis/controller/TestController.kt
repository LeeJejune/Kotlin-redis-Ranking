package com.jjlee.redis.controller

import com.jjlee.redis.dto.reponse.UserRankingResponse
import com.jjlee.redis.dto.request.CreateUserRequest
import com.jjlee.redis.dto.request.UserScoreRequest
import com.jjlee.redis.service.user.UserScoreService
import com.jjlee.redis.service.user.UserService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController

@RestController
class TestController(
    private val userService: UserService,
    private val userScoreService: UserScoreService
) {


    // 단순 유저 저장 Test Controller
    @PostMapping("/api/users")
    fun createUser(@RequestBody req: CreateUserRequest) : ResponseEntity<String>{
        userService.saveUser(req)
        return ResponseEntity.ok("유저 저장 성공!")
    }

    // 단순 유저 점수 저장 Test Controller
    @PostMapping("/api/users/{userId}/score")
    fun createUserScore(@PathVariable userId: Long, @RequestBody req: UserScoreRequest) : ResponseEntity<String>{
        userScoreService.saveUserScore(userId, req)
        return ResponseEntity.ok("유저 점수 저장 성공!")
    }

    // 랭킹 정보를 보기 위한 Test Controller
    @GetMapping("/api/users/ranking")
    fun getUserRanking() : ResponseEntity<List<UserRankingResponse>>{
        return ResponseEntity.ok(userScoreService.getUserRanking())
    }

}