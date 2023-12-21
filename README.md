## Redis의 Sorted Set과 시간 값을 활용한 랭킹 시스템 구현

### ⭐️ 1. 랭킹 시스템의 구현 목표
나의 목표는 랭킹 데이터를 시간 순 및 점수 순으로 정렬할 수 있으며, 빠르게 데이터 조회가 필요한 상황에서도 빠르게 데이터를 조회할 수 있는 기능을 구현하는 것이었다.
선택지는 아래와 같았다.
- RDB 집계 및 정렬을 통해 랭킹 데이터를 조회하는 방법
- Redis의 Sorted Set을 활용하여 랭킹 데이터를 조회하는 방법

위 두 가지를 통해 랭킹 데이터를 조회하는 방법을 구현해보았다.

### 🤔 2. 랭킹 시스템의 구현 방법에 대한 고민
RDB를 통해 유저의 현재 랭킹과 점수 등 통합적으로 보여주는 방법이 있다.
물론, 프로덕션 환경에서는 조금 더 고민이 필요한 부분이다. (더욱 복잡한 조건과 더 많은 데이터를 처리해야 하기 때문이다.)

나의 경우는 개발 단계에서 RDB의 SQL로 이를 구현했었다. 
하지만, 실시간 성을 가지고 있는 랭킹 시스템에서는 그리 효율적인 방법은 아니었다. 
실제로 개발 단계에서 RDB를 통해 랭킹 시스템을 구현했을 때, 랭킹 데이터를 조회하는데 1~2초 이상의 시간이 소요되었다. (당연히, 요청 수와 랭킹 데이터가 많아질수록 더 오래 걸릴 것이다.)

그래서 고민이 생겼다. 실시간 성을 보장하면서, 빠르게 랭킹 데이터를 조회할 수 있는 방법은 무엇일까?
나에게 필요한 건 아래와 같았다.
- 실시간으로 랭킹 데이터 조회
- 빠르게 랭킹 데이터 조회
- 랭킹 데이터를 시간 순 및 점수 순으로 정렬
- 중복 점수의 경우 랭킹 데이터를 시간 순으로 정렬

위의 조건을 만족하는 방법을 열심히 찾아보고 고민해보았다..! 

이에 대한 고민을 통해 나는 결론적으로 Redis의 Sorted Set을 활용하는 방법을 선택했다.
Redis를 선택한 이유는 다음과 같다.
- Redis는 메모리 기반의 데이터 저장소이기 때문에 빠른 속도로 데이터를 조회할 수 있다.
- Redis의 Sorted Set은 집합 데이터이기 때문에 중복을 해결해준다.
- score를 하나하나 비교할 필요가 없다! (score 값을 비교해 알아서 정렬해준다!!)
- 가장 큰 이유로 우리 시스템에서 추가적인 인프라 자원이 필요하지 않았다. (Redis를 이미 사용하고 있었기 때문이다.)

위 같은 이유들로 인해 Redis의 Sorted Set을 활용하여 랭킹 시스템을 구현하기로 결정했다.

### 📌 3. 랭킹 시스템의 구현 방법

[Redis의 Sorted Set 공식 문서를 많이 참고했다.](https://redis.io/docs/data-types/sorted-sets/)

구현 방식은 크게 어렵진 않았지만 중복 점수의 경우 시간 순으로 정렬되도록 하는 것이 고민이었다.
내가 선택한 방법은 유저가 마지막으로 점수를 획득한 시간을 1에서 빼서 이를 score와 함께 저장하는 것이었다.
이를 통해 중복 점수의 경우 시간 순으로 정렬할 수 있었다.

1. 유저가 점수를 얻으면 그 점수를 RDB에 저장한다.
2. 중복 점수를 해결하기 위해 유저가 점수를 얻은 시간을 1에서 빼서 이를 score와 함께 Redis의 Sorted Set에 저장한다.
2. Redis의 Sorted Set에 쌓인 데이터를 점수 순으로 조회한다.
3. Redis의 데이터가 삭제됐을 경우를 대비하여 RDB에 저장된 데이터를 통해 Redis에 데이터를 저장하는 방법을 구현한다.

위와 같은 방법으로 랭킹 시스템을 구현했다.
간단하게 이를 구현할 수 있는 방법을 소개하겠다.

```kotlin
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
```

위와 같은 구현 방식을 통해 유저의 점수를 저장하고, 중복 점수의 경우 시간 순으로 정렬할 수 있었다.
물론 위와 같은 방법 말고도 다양한 방법이 있을 것이다. (Ex - Redis 말고 다른 데이터 저장소를 사용하는 방법 등..)

조회의 경우 아래와 같이 구현했다.

```kotlin
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
```

위와 같은 방법을 통해 랭킹 데이터를 조회할 수 있었다.
(물론, 프로덕션 환경에서는 이보다 복잡하고 페이징을 통해 더욱 빠르게 조회할 수 있도록 설계했다. ex - 유저정보 캐싱)

이를 통해 랭킹 데이터를 빠르게 조회할 수 있었다!!

아래는 테스트로 직접 구현한 랭킹 시스템의 결과이다.
점수 저장은 다음과 같은 순서로 저장했다.
1. 1번 유저가 10점을 획득
2. 2번 유저가 5점을 획득
3. 3번 유저가 10점을 획득

```redis
127.0.0.1:6379> ZRANGE "UserLeaderBoard" 0 -1 WITHSCORES
1) "2"
2) "5.79768780785461"
3) "3"
4) "10.7976878078409"
5) "1"
6) "10.79768780785549"
```
아래는 랭킹 데이터를 API를 통해 조회한 결과이다.
```json
[
    {
        "userId": 1,
        "name": "제준",
        "score": 10,
        "rank": 1
    },
    {
        "userId": 3,
        "name": "수진",
        "score": 10,
        "rank": 2
    },
    {
        "userId": 2,
        "name": "민수",
        "score": 5,
        "rank": 3
    }
]
```

## 👀 4. 랭킹 시스템의 Redis 도입의 결과 및 느낀 점
결과적으로 Redis의 활용하는 방법은 랭킹 데이터를 빠르게 조회할 수 있었고, 실제로 프로덕션 환경에서 정확한 정보 및 속도를 보장할 수 있었다.
또한, 추가적인 인프라 환경을 구축할 필요가 없었기 때문에 더욱 좋았다.

물론, Redis가 아닌 다른 데이터 저장소를 활용하는 방법도 있을 것이고, 위 같은 방법이 아닌 다른 방법도 있을 것이다.
이를 종합해서 돌아봤을 때 우리에게 주어진 환경에서 Redis를 활용한 랭킹 시스템을 구현한 것은 타당했다고 생각한다.

