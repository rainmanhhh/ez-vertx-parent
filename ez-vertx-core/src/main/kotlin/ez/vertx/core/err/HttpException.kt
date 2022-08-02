package ez.vertx.core.err

import ez.vertx.core.config.ConfigVerticle
import ez.vertx.core.config.ErrorConfig
import io.netty.handler.codec.http.HttpResponseStatus
import io.vertx.core.http.HttpMethod

@Suppress("unused", "MemberVisibilityCanBePrivate")
class HttpException(val code: Int, message: String) : Exception(message) {
  override fun fillInStackTrace(): Throwable {
    return this
  }

  companion object {
    private val config: ErrorConfig
      get() {
        val error: ErrorConfig by ConfigVerticle
        return error
      }

    fun badRequest(message: String) = HttpResponseStatus.BAD_REQUEST.err(message)
    fun forbidden(message: String) = HttpResponseStatus.FORBIDDEN.err(message)
    fun internalErr(message: String) = HttpResponseStatus.INTERNAL_SERVER_ERROR.err(message)
    fun require(name: String) = badRequest(config.message.paramRequired + name)
    fun formatError(name: String) = badRequest(config.message.paramFormatError + name)
    fun methodNotAllowed(httpMethod: HttpMethod?) = HttpResponseStatus.METHOD_NOT_ALLOWED.err("method not allowed: $httpMethod")
  }
}

fun HttpResponseStatus.err(message: String = this.reasonPhrase()) = HttpException(code(), message)
