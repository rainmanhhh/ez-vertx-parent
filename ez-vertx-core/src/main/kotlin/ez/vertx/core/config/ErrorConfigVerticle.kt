package ez.vertx.core.config

class ErrorConfigVerticle : ConfigVerticle<ErrorConfig>() {
  override val key = "error"
  override var configValue = ErrorConfig()
}

class ErrorConfig {
  var message = ErrorMessageConfig()
}

class ErrorMessageConfig {
  @Description("required param not found in request")
  var paramRequired = "param required:"

  @Description("param format error")
  var paramFormatError = "param format error:"

  @Description("server-side apiKey config value is null")
  var serverApiKeyNotSet = "server api key not set"

  @Description("apiKey in request is not match with server-side config value")
  var requestApiKeyIncorrect = "request api key incorrect"
}
