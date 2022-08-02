package ez.vertx.webproxy

import ez.vertx.core.busi.BusiVerticle
import ez.vertx.core.config.ConfigVerticle
import ez.vertx.core.config.HttpServerConfig
import ez.vertx.core.message.res.SimpleRes
import io.vertx.core.http.HttpMethod
import io.vertx.core.json.JsonObject

abstract class ReqEncoderVerticle : BusiVerticle<SimpleRes<Any?>>() {
  private lateinit var busiAddress: String

  override suspend fun start() {
    val httpServer: HttpServerConfig by ConfigVerticle
    busiAddress = httpServer.busiAddress
    if (busiAddress.isEmpty()) throw IllegalArgumentException("busiAddress should not be empty")
    super.start()
  }

  override fun path(): String = busiAddress

  /**
   * encode request body.
   * @return a json object. if [WebClientConfig.contentType] is json or form,
   *   this object will be mapped into request body;
   *   otherwise it should have a field named "value" which contains request body string
   *   (eg.`{"value": "<root>this is xml node</root>"}`)
   */
  abstract fun encodeReq(httpMethod: HttpMethod, path: String, params: JsonObject): JsonObject

  final override fun serve(
    httpMethod: HttpMethod,
    path: String,
    params: JsonObject
  ): SimpleRes<Any?> {
    return SimpleRes.continueTo(
      WebClientVerticle.messageSendReq,
      encodeReq(httpMethod, path, params)
    )
  }
}
