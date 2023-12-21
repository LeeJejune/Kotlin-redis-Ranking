package com.jjlee.redis

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.data.jpa.repository.config.EnableJpaAuditing

@SpringBootApplication
@EnableJpaAuditing
class RedisApplication

fun main(args: Array<String>) {
	runApplication<RedisApplication>(*args)
}
