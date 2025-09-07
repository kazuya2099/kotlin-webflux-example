package org.zupzup.kotlinwebfluxdemo.controller

import mu.KLogging
import org.springframework.http.MediaType.*
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestMethod
import org.springframework.web.bind.annotation.RestController
import org.zupzup.kotlinwebfluxdemo.model.LightComment
import org.zupzup.kotlinwebfluxdemo.model.Response
import org.zupzup.kotlinwebfluxdemo.service.APIService
import reactor.core.publisher.Mono
import reactor.core.scheduler.Schedulers

@RestController
@RequestMapping(path = ["/api"], produces = [ APPLICATION_JSON_VALUE ])
class APIController(
        private val apiService: APIService
) {

    companion object : KLogging()

    @RequestMapping(method = [RequestMethod.GET])
    fun getData(): Mono<ResponseEntity<List<Response>>> {
        return apiService.fetchPosts()
                .filter { it -> it.userId % 2 == 0 }
                .take(20)
                .parallel(4)
                .runOn(Schedulers.parallel())
                .flatMap {post ->
                    apiService.fetchComments(post.id)
                        .map { comment -> LightComment(email = comment.email, body = comment.body) }
                        .collectList()
                        .map {
                            comments -> Response(
                                postId = post.id,
                                userId = post.userId,
                                title = post.title,
                                comments = comments
                            )
                        }
                }
                .sequential()
                .collectList()
                .map { body -> ResponseEntity.ok().body(body) }
    }
}