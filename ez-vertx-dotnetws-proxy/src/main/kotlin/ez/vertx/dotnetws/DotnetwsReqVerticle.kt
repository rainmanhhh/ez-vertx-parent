package ez.vertx.dotnetws

import ez.vertx.core.AutoDeployVerticle
import ez.vertx.webproxy.ReqEncoderVerticle
import io.vertx.core.http.HttpMethod
import io.vertx.core.json.JsonObject
import io.vertx.kotlin.core.json.jsonObjectOf
import org.apache.commons.text.StringEscapeUtils

/**
 * request encoder verticle for dotnet webservice
 */
open class DotnetwsReqVerticle : ReqEncoderVerticle(), AutoDeployVerticle {

  override fun encodeReq(httpMethod: HttpMethod, path: String, params: JsonObject): JsonObject {
    val reqBodyName = path.split("/").last()
    val paramsBuilder = StringBuilder()
    for (entry in params) {
      entry.toXml()?.let { paramsBuilder.append(it) }
    }
    //language=XML
    val xml = """
      <?xml version="1.0" encoding="utf-8"?>
      <soap:Envelope xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xmlns:xsd="http://www.w3.org/2001/XMLSchema" xmlns:soap="http://schemas.xmlsoap.org/soap/envelope/">
        <soap:Body>
          <${reqBodyName} xmlns="http://tempuri.org/">
            $paramsBuilder
          </${reqBodyName}>
        </soap:Body>
      </soap:Envelope>
    """.trimIndent()
    return jsonObjectOf("value" to xml)
  }

  private fun Map.Entry<String, Any?>.toXml() = value?.let {
    val v = StringEscapeUtils.escapeXml11(it.toString())
    "<$key>$v</${key}>"
  }
}
