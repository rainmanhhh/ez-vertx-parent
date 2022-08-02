package ez.vertx.core.util

import io.vertx.core.http.HttpMethod
import io.vertx.core.json.JsonObject
import io.vertx.ext.web.RoutingContext
import io.vertx.ext.web.impl.ParsableMIMEValue

/**
 * - Merge queryParams and body into a json object.
 * - All params in query or form will be treated as strings.
 * - Supported method: POST, PUT, PATCH
 * - Supported content type: *&#47;json, *&#47;x-www-form-urlencoded (other content types will make the body ignored)
 */
fun RoutingContext.paramsAsJson(): JsonObject {
  val params = queryParams().toJson()
  val bodyJson = when (request().method()) {
    HttpMethod.POST, HttpMethod.PUT, HttpMethod.PATCH -> {
      val contentType = parsedHeaders().contentType() as ParsableMIMEValue
      contentType.forceParse()
      when (contentType.subComponent()) {
        "json" -> body().asJsonObject()
        "x-www-form-urlencoded" -> request().formAttributes().toJson()
        else -> null
      }
    }
    else -> null
  }
  bodyJson?.let { params.mergeIn(it) }
  return params
}
