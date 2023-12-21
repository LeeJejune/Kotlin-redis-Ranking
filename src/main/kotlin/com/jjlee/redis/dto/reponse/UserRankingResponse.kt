package com.jjlee.redis.dto.reponse

data class UserRankingResponse(
    // 유저 Id
    val userId: Long,

    // 유저 이름
    val name: String,

    // 유저 점수
    val score: Int,

    val rank: Int
) {
}