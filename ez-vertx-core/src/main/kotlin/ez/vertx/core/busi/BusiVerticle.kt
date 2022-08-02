package ez.vertx.core.busi

import ez.vertx.core.err.HttpException
import ez.vertx.core.message.receiveMessage
import ez.vertx.core.message.res.SimpleRes
import ez.vertx.core.util.httpMethod
import ez.vertx.core.util.path
import io.netty.handler.codec.http.HttpResponseStatus
import io.vertx.core.http.HttpMethod
import io.vertx.core.json.JsonObject
import io.vertx.kotlin.coroutines.CoroutineVerticle

/**
 * @param ResData see [serve]
 */
abstract class BusiVerticle<ResData> : CoroutineVerticle() {
  override suspend fun start() {
    val p = path() ?: throw NullPointerException(javaClass.name + ".path should not return null")
    if (p == "/" || p.startsWith("/_admin/"))
      throw IllegalArgumentException(
        javaClass.name + " should not use `/` or `/_admin/**/*` path which are reserved by system handlers"
      )
    receiveMessage(p) {
      val httpMethod = it.headers.httpMethod!!.let(HttpMethod::valueOf)
      val path = it.headers.path!!
      serveAsync(httpMethod, path, it.body)
    }
  }

  /**
   * http request path(start with `/`). eg: `/getUserList`.
   * will be used as eventbus message address too. (when [HttpServerVerticle] received a request, it will send a message with this address)
   */
  abstract fun path(): String?

  /**
   * async version of [serve]. if override this function, [get], [post], [put], [delete], [patch] will be ignored
   */
  open suspend fun serveAsync(httpMethod: HttpMethod, path: String, params: JsonObject): ResData =
    serve(httpMethod, path, params)

  /**
   * handle the http request.
   * if return a [SimpleRes] and it's code is [HttpResponseStatus.CONTINUE],
   * the response will be sent to "next" verticle:
   * - [SimpleRes.message] will be used as eventbus message address;
   * - [SimpleRes.data] will be used as request param object(so it should be a [JsonObject],
   * or an object which could be mapped into [JsonObject]).
   *
   * **examples**:
   * - sending `{a: 1}` to address "foo.bar"(you should register another verticle to handle it)
   * ```kotlin
   * return SimpleRes.continueTo("foo.bar", mapOf("a" to 1))
   * ```
   * - write `[1, 2, 3]` to client
   * ```kotlin
   * return arrayOf(1, 2, 3)
   * ```
   * - report 403 error to client
   * ```kotlin
   * throw ez.vertx.core.err.HttpException.forbidden("access denied")
   * ```
   *
   * @param path request path. valid only when [ez.vertx.core.config.HttpServerConfig.busiAddress] is not empty
   * @param params merge url query params with request body(support json or form data)
   * @return a [SimpleRes], or data field of it
   */
  open fun serve(httpMethod: HttpMethod, path: String, params: JsonObject): ResData =
    when (httpMethod) {
      HttpMethod.GET -> get(path, params)
      HttpMethod.POST -> post(path, params)
      HttpMethod.DELETE -> delete(path, params)
      HttpMethod.PUT -> put(path, params)
      HttpMethod.PATCH -> patch(path, params)
      else -> throw HttpException.methodNotAllowed(httpMethod)
    }

  /**
   * like [serve] but only deal with [HttpMethod.GET]
   */
  open fun get(path: String?, params: JsonObject): ResData =
    throw HttpException.methodNotAllowed(HttpMethod.GET)

  /**
   * like [serve] but only deal with [HttpMethod.POST]
   */
  open fun post(path: String?, params: JsonObject): ResData =
    throw HttpException.methodNotAllowed(HttpMethod.POST)

  /**
   * like [serve] but only deal with [HttpMethod.DELETE]
   */
  open fun delete(path: String?, params: JsonObject): ResData =
    throw HttpException.methodNotAllowed(HttpMethod.DELETE)

  /**
   * like [serve] but only deal with [HttpMethod.PUT]
   */
  open fun put(path: String?, params: JsonObject): ResData =
    throw HttpException.methodNotAllowed(HttpMethod.PUT)

  /**
   * like [serve] but only deal with [HttpMethod.PATCH]
   */
  open fun patch(path: String?, params: JsonObject): ResData =
    throw HttpException.methodNotAllowed(HttpMethod.PATCH)
}
