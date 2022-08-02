package ez.vertx.core.config

import io.vertx.core.VertxOptions
import io.vertx.core.eventbus.DeliveryOptions

class VertxConfig : VertxOptions() {
  @Description("eventBus message min delivery timeout, in ms")
  var minMessageTimeout = DeliveryOptions.DEFAULT_TIMEOUT
}
