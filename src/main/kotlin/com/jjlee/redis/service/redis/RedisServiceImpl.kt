package com.jjlee.redis.service.redis

import org.springframework.data.redis.core.RedisTemplate
import org.springframework.data.redis.core.ZSetOperations
import org.springframework.stereotype.Service

@Service
class RedisServiceImpl(
    private val redisTemplate: RedisTemplate<String, Any>
) : RedisService {

    override fun <T : Any> zSet(key: String, value: T, score: Double) {
        redisTemplate.opsForZSet().add(key, value, score)
    }

    override fun zGetList(key: String, start: Int, end: Int): MutableSet<ZSetOperations.TypedTuple<Any>>? {
        return redisTemplate.opsForZSet().reverseRangeWithScores(key, start.toLong(), end.toLong()) ?: return null
    }

    override fun getRank(key: String, value: String): Long? {
        return redisTemplate.opsForZSet().reverseRank(key, value)
    }
}