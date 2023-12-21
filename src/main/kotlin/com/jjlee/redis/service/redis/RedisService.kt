package com.jjlee.redis.service.redis

import org.springframework.data.redis.core.ZSetOperations

interface RedisService {
    // Redis Sorted Set 저장
    fun <T : Any> zSet(key: String, value: T, score: Double)
    // Redis Sorted Set 가져오기
    fun zGetList(key: String, start: Int, end: Int): MutableSet<ZSetOperations.TypedTuple<Any>>?
    // Redis Sorted Set을통해 유저 랭킹 가져오기
    fun getRank(key: String, value: String): Long?
}