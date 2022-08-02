package ez.vertx.core.message

import ez.vertx.core.config.ConfigVerticle
import ez.vertx.core.config.VertxConfig
import ez.vertx.core.message.req.Req
import ez.vertx.core.message.res.SimpleRes
import ez.vertx.core.util.VertxUtil
import ez.vertx.core.util.httpMethod
import io.vertx.core.eventbus.DeliveryOptions
import io.vertx.core.eventbus.Message
import io.vertx.core.json.Json
import io.vertx.core.json.JsonArray
import io.vertx.core.json.JsonObject
import io.vertx.kotlin.coroutines.await
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.slf4j.LoggerFactory

object MessageEx {
  internal val logger = LoggerFactory.getLogger(MessageEx::class.java)
}

/**
 * @param address eventbus message address
 * @param reqBody should be a [JsonObject] or [JsonArray] (or an object/list which can be mapped to them)
 * @param resClass should be an object class which can be mapped from a [JsonObject]
 * @param deliveryOptions eventbus message delivery options
 */
suspend fun <ReqBody, Res : SimpleRes<*>> sendMessage(
  address: String,
  reqBody: ReqBody,
  resClass: Class<Res>,
  deliveryOptions: DeliveryOptions = DeliveryOptions()
): Res {
  MessageEx.logger.debug(
    "send message to: {}, reqBody: {}, resClass: {}",
    address,
    reqBody,
    resClass
  )
  val jsonReq: Any =
    when (reqBody) {
      is JsonObject, is JsonArray -> reqBody
      is List<*> -> JsonArray(reqBody)
      null -> JsonObject()
      else -> JsonObject.mapFrom(reqBody)
    }
  val vertx: VertxConfig by ConfigVerticle
  val minTimeout = vertx.minMessageTimeout
  if (deliveryOptions.sendTimeout < minTimeout) deliveryOptions.sendTimeout = minTimeout
  val resBody = VertxUtil.vertx().eventBus().request<JsonObject>(
    address, jsonReq, deliveryOptions
  ).await().body()
  MessageEx.logger.debug("sendMessage resBody: {}", resBody)
  return resBody.mapTo(resClass)
}

/**
 * @param address eventbus message address
 * @param reqBodyClass should be an object class which can be mapped from a [JsonObject]
 * @param handler could return a [SimpleRes], or raw response body(which will be used as [SimpleRes.data])
 */
@Suppress("unused")
fun <ReqBody> CoroutineScope.receiveMessage(
  address: String,
  reqBodyClass: Class<ReqBody>,
  handler: suspend (req: Req<ReqBody>) -> Any?
) {
  MessageEx.logger.debug(
    "register message handler for address: {}, scope: {}",
    address,
    javaClass.name
  )
  val vertx = VertxUtil.vertx()
  vertx.eventBus().consumer(address) {
    val body = it.body() ?: JsonObject()
    if (MessageEx.logger.isDebugEnabled) {
      val httpMethod = it.headers().httpMethod
      MessageEx.logger.debug(
        "received message at address: {}, httpMethod: {}, body: {}",
        address,
        httpMethod,
        Json.encodePrettily(body)
      )
    }
    handleReq(it, Req(it.headers(), body.mapTo(reqBodyClass)), handler)
  }
}

/**
 * @param address eventbus message address
 * @param handler could return a [SimpleRes], or raw response body(which will be used as [SimpleRes.data])
 */
fun CoroutineScope.receiveMessage(
  address: String,
  handler: suspend (req: Req<JsonObject>) -> Any?
) {
  MessageEx.logger.debug(
    "register message handler for address: {}, scope: {}",
    address,
    javaClass.name
  )
  val vertx = VertxUtil.vertx()
  vertx.eventBus().consumer(address) {
    if (MessageEx.logger.isDebugEnabled) {
      val httpMethod = it.headers().httpMethod
      MessageEx.logger.debug(
        "received message at address: {}, httpMethod: {}, body: {}",
        address,
        httpMethod,
        it.body()
      )
    }
    handleReq(it, Req(it.headers(), it.body() ?: JsonObject()), handler)
  }
}

private fun <ReqBody> CoroutineScope.handleReq(
  message: Message<JsonObject>,
  req: Req<ReqBody>,
  handler: suspend (req: Req<ReqBody>) -> Any?
) {
  launch {
    val res = try {
      val res = handler(req)
      if (res is SimpleRes<*>) res
      else SimpleRes<Any>().apply { data = res }
    } catch (e: Throwable) {
      MessageEx.logger.error(
        "handle message error! address: {}, req: {}",
        message.address(),
        req,
        e
      )
      SimpleRes.fromError(e)
    }
    try {
      message.reply(JsonObject.mapFrom(res))
    } catch (e: Throwable) {
      MessageEx.logger.error("reply message error! address: {}", message.address(), e)
    }
  }
}
