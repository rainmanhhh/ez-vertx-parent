package ez.vertx.webproxy

import ez.vertx.core.config.Description
import io.vertx.ext.web.client.WebClientOptions

class WebClientConfig : WebClientOptions() {
  @Description(
    """
    base uri of target server. eg: `https://www.example.com`
    - should not be null or empty
    - should not end with `/`
    """
  )
  var uriRoot: String? = null

  @Description("if set to null, use original path; otherwise all requests will use this same value as path")
  var uriFixedPath: String? = null

  @Description("if set, all requests will use the same httpMethod")
  var httpMethod: String? = null

  @Description("target server response charset")
  var responseCharset = "UTF-8"

  @Description("""
    when sending request to target server with method POST/PUT/PATCH, use this content type
    - if type is json or form, request body will be encoded automatically
    - otherwise, you should inherit [ReqEncoderVerticle] and encode request body to json object `{"value": "text content"}`(eg. `{"value": "<root>this is xml node</root>"}`)
  """)
  var contentType: String = "application/json;charset=utf-8"

  @Description("request headers other than contentType")
  var headers: Map<String, String>? = null
}
