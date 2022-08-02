package ez.vertx.core.handler

import io.vertx.core.Handler
import io.vertx.ext.web.RoutingContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

abstract class CoroutineHandler(
  @Suppress("MemberVisibilityCanBePrivate") protected val scope: CoroutineScope
) : Handler<RoutingContext> {

  override fun handle(ctx: RoutingContext) {
    scope.launch {
      try {
        val next = handleAsync(ctx)
        if (next) ctx.next()
      } catch (e: Throwable) {
        ctx.fail(e)
      }
    }
  }

  /**
   * if [io.vertx.core.http.HttpServerResponse.end] already called in this function,
   * it will return false to tip the router skipping all remained handlers in the chain
   * @return whether to go to the next handler
   */
  abstract suspend fun handleAsync(ctx: RoutingContext): Boolean
}
