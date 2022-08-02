package ez.vertx.core.message.req

import io.vertx.core.MultiMap

data class Req<Body>(
  val headers: MultiMap,
  val body: Body
)
