package ez.vertx.core.busi

import ez.vertx.core.config.ConfigVerticle
import ez.vertx.core.config.ErrorConfig
import ez.vertx.core.config.HttpServerConfig
import ez.vertx.core.err.HttpException
import io.vertx.core.json.JsonObject

class ApiKey {
  companion object {
    private const val paramName = "_apiKey"

    @JvmStatic
    fun check(params: JsonObject) {
      check(params.getString(paramName) ?: throw HttpException.require(paramName))
    }

    @JvmStatic
    fun check(input: String?) {
      val httpServer: HttpServerConfig by ConfigVerticle
      val error: ErrorConfig by ConfigVerticle
      if (httpServer.apiKey == "")
        throw HttpException.internalErr(error.message.serverApiKeyNotSet)
      if (input != httpServer.apiKey)
        throw HttpException.forbidden(error.message.requestApiKeyIncorrect)
    }
  }
}
