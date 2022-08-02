package ez.vertx.dotnetws

import ez.vertx.core.message.res.SimpleRes
import ez.vertx.webproxy.WebClientVerticle
import io.netty.handler.codec.http.HttpResponseStatus
import io.vertx.core.buffer.Buffer
import io.vertx.ext.web.client.HttpResponse
import org.slf4j.LoggerFactory

open class DotnetwsResVerticle : WebClientVerticle() {
  companion object {
    private const val soapBodyPrefix = "<soap:Body>"
    private const val soapBodySuffix = "</soap:Body>"
    private val logger = LoggerFactory.getLogger(DotnetwsResVerticle::class.java)
  }

  override fun decodeRes(res: HttpResponse<Buffer>): SimpleRes<Any?> = SimpleRes<Any?>().apply {
    code = res.statusCode()
    val resText = res.bodyAsString(configValue.responseCharset)
    if (isSuccess()) {
      val begin = resText.indexOf(soapBodyPrefix)
      val end = resText.lastIndexOf(soapBodySuffix)
      if (begin < 0 || end < 0) {
        code = HttpResponseStatus.INTERNAL_SERVER_ERROR.code()
        message = "dotnet server returns invalid soap body"
        logger.error("soap body invalid: {}", resText)
      } else {
        val resBody =
          resText.substring(begin + soapBodyPrefix.length, end)
        data = resBody
      }
    } else {
      message = resText
    }
  }
}
