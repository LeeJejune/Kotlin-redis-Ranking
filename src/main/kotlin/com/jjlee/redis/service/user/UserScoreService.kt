package com.jjlee.redis.service.user

import com.jjlee.redis.domain.user.User
import com.jjlee.redis.domain.user.UserScore
import com.jjlee.redis.domain.user.UserScoreRepository
import com.jjlee.redis.dto.reponse.UserRankingResponse
import com.jjlee.redis.dto.request.UserScoreRequest
import com.jjlee.redis.service.redis.RedisService
import com.jjlee.redis.utils.RedisKeys
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.format.DateTimeFormatter

@Service
@Transactional
class UserScoreService(
    private val userService: UserService,
    private val userScoreRepository: UserScoreRepository,
    private val redisService: RedisService
) {

    fun saveUserScore(userId: Long, req: UserScoreRequest) {
        val findUser = userService.getUser(userId)

        val newUserScore = UserScore(findUser, req.score)

        // 점수 저장
        userScoreRepository.save(newUserScore)

        // 시간 값을 통해 중복 점수 시간 순 정렬을 위해, 시간 값을 1에서 빼 더한 값을 저장.
        val time = newUserScore.createdAt
        val formattedTime = time.format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"))
        val calculateTime = 1 - ("0.$formattedTime".toDouble())

        // Redis Sorted Set에 저장
        redisService.zSet(
            RedisKeys.userLeaderBoard(),
            findUser.id.toString(),
            getUserScore(userId) + calculateTime
        )
    }

    fun getUserScore(userId: Long): Int {
        return userScoreRepository.findByUserId(userId).score
    }

    fun getUserRanking(): List<UserRankingResponse> {
        val key = RedisKeys.userLeaderBoard()

        // Redis 랭킹 데이터 가져오기 (페이징은 테스트 값)
        val data = redisService.zGetList(key, 0, 50) ?: emptyList()

        // User 정보 가져오기.
        val users = userService.getUsers()

        val res = users.map { u ->
            val rank = data.find { it.value == u.id.toString() }!!
            val userRank = redisService.getRank(key, u.id.toString())

            UserRankingResponse(
                u.id!!,
                u.name,
                rank.score!!.toInt(),
                userRank!!.toInt() + 1
            )
        }.sortedBy { it.rank }
        return res
    }
}