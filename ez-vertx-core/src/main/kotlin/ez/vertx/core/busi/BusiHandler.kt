package ez.vertx.core.busi

import ez.vertx.core.err.HttpException
import ez.vertx.core.handler.CoroutineHandler
import ez.vertx.core.message.res.SimpleRes
import ez.vertx.core.message.res.check
import ez.vertx.core.message.sendMessage
import ez.vertx.core.util.httpMethod
import ez.vertx.core.util.paramsAsJson
import ez.vertx.core.util.path
import io.netty.handler.codec.http.HttpResponseStatus
import io.vertx.core.MultiMap
import io.vertx.core.eventbus.DeliveryOptions
import io.vertx.core.http.HttpHeaders
import io.vertx.core.json.Json
import io.vertx.core.json.JsonObject
import io.vertx.ext.web.RoutingContext
import io.vertx.kotlin.coroutines.await
import kotlinx.coroutines.CoroutineScope
import org.slf4j.LoggerFactory

/**
 * send message to [ez.vertx.core.busi.BusiVerticle]
 */
class BusiHandler(
  scope: CoroutineScope,
  private val busiAddress: String
) : CoroutineHandler(scope) {
  companion object {
    private val logger = LoggerFactory.getLogger(BusiHandler::class.java)
  }

  override suspend fun handleAsync(ctx: RoutingContext): Boolean {
    if (ctx.response().ended()) return false
    val path = ctx.normalizedPath()
    val httpMethod = ctx.request().method().name()
    var reqBody: Any = ctx.paramsAsJson()
    logger.debug("req path: {}, httpMethod: {}, reqBody: {}", path, httpMethod, reqBody)
    val deliveryOptions = DeliveryOptions().apply {
      headers = MultiMap.caseInsensitiveMultiMap()
      headers.httpMethod = httpMethod
    }
    deliveryOptions.headers.path = path
    var address = busiAddress.ifEmpty { path }
    var res: SimpleRes<*>
    do {
      res = sendMessage(address, reqBody, SimpleRes::class.java, deliveryOptions)
      val resCode = res.code ?: 0
      if (resCode == HttpResponseStatus.CONTINUE.code()) {
        val nextAddress = res.message
        if (nextAddress.isNullOrBlank()) throw NullPointerException("nextAddress is null or empty")
        if (nextAddress == address) throw HttpException.internalErr("nextAddress is the same as current: $address")
        address = nextAddress
        val resData = res.data
        reqBody =
          when (resData) {
            null -> JsonObject()
            is JsonObject -> resData
            else -> JsonObject.mapFrom(resData)
          }
        logger.debug("next address: {}, reqBody: {}", address, reqBody)
      }
    } while (resCode == HttpResponseStatus.CONTINUE.code())
    val resData = res.check()
    logger.debug("busiVerticle resData: {}", resData)
    ctx.response().putHeader(
      HttpHeaders.CONTENT_TYPE, "application/json;charset=utf-8"
    ).end(
      Json.encode(resData)
    ).await()
    return false
  }
}
