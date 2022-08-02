package ez.vertx.core.config

class VertxConfigVerticle : ConfigVerticle<VertxConfig>() {
  override val key = "vertx"
  override var configValue = VertxConfig()
}
