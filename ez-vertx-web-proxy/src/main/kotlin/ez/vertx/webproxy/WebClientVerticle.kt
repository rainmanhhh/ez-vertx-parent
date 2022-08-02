package ez.vertx.webproxy

import ez.vertx.core.AutoDeployVerticle
import ez.vertx.core.config.ConfigVerticle
import ez.vertx.core.message.receiveMessage
import ez.vertx.core.message.req.Req
import ez.vertx.core.message.res.SimpleRes
import ez.vertx.core.util.httpMethod
import ez.vertx.core.util.path
import ez.vertx.core.util.toMultiMap
import io.vertx.core.buffer.Buffer
import io.vertx.core.http.HttpHeaders
import io.vertx.core.http.HttpMethod
import io.vertx.core.json.JsonObject
import io.vertx.ext.web.client.HttpResponse
import io.vertx.ext.web.client.WebClient
import io.vertx.ext.web.impl.ParsableMIMEValue
import io.vertx.kotlin.coroutines.await
import org.slf4j.LoggerFactory

open class WebClientVerticle : AutoDeployVerticle, ConfigVerticle<WebClientConfig>() {
  companion object {
    private val logger = LoggerFactory.getLogger(WebClientVerticle::class.java)
    val messageSendReq = WebClientVerticle::class.java.name + ".messageSendReq"
  }

  lateinit var client: WebClient
  lateinit var mimeValue: ParsableMIMEValue

  private suspend fun sendRequest(req: Req<JsonObject>): HttpResponse<Buffer> {
    val httpMethod = HttpMethod.valueOf((configValue.httpMethod ?: req.headers.httpMethod)!!)
    val uri = configValue.uriRoot!! + (configValue.uriFixedPath ?: req.headers.path)
    logger.info("sendRequest: {} {}", httpMethod, uri)
    val request = client.requestAbs(httpMethod, uri)
    configValue.headers?.let { request.headers().addAll(it) } // set common headers
    val response = when (httpMethod) {
      HttpMethod.POST, HttpMethod.PUT, HttpMethod.PATCH -> { // send params in body
        when (mimeValue.subComponent()) {
          "json" -> request.sendJsonObject(req.body)
          "x-www-form-urlencoded" -> request.sendForm(req.body.toMultiMap())
          else -> { // other contentTypes will not be set automatically by sendXXX
            request.putHeader(HttpHeaders.CONTENT_TYPE.toString(), configValue.contentType)
            request.sendBuffer(Buffer.buffer(req.body.getString("value")))
          }
        }
      }
      else -> { // send params in querystring
        request.queryParams().addAll(req.body.toMultiMap())
        request.send()
      }
    }
    return response.await()
  }

  /**
   * return response body, or throw a [ez.vertx.core.err.HttpException].
   * default: decode response body as json
   */
  open fun decodeRes(res: HttpResponse<Buffer>): SimpleRes<Any?> = SimpleRes<Any?>().apply {
    code = res.statusCode()
    if (isSuccess()) data = res.bodyAsBuffer()?.toJson()
    else message = res.bodyAsString(configValue.responseCharset)
  }

  override val key: String = "webClient"
  override var configValue: WebClientConfig = WebClientConfig()

  override suspend fun afterConfig() {
    configValue.uriRoot ?: throw NullPointerException("uriRoot is null")
    client = WebClient.create(vertx, configValue)
    mimeValue = ParsableMIMEValue(configValue.contentType).forceParse()
    receiveMessage(messageSendReq) {
      it.headers.path
      val res = sendRequest(it) // uri ?
      decodeRes(res)
    }
  }
}
