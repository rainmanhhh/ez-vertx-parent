package ez.vertx.core.util

import io.vertx.core.MultiMap
import io.vertx.core.json.JsonObject

/**
 * All param values will be treated as string, so the result is a `Map<String, String>` json object.
 * Values in the result object will not be null or blank
 */
fun MultiMap.toJson() = JsonObject().also {
  for (entry in entries()) {
    entry.value?.let { value ->
      it.put(entry.key, value)
    }
  }
}

private const val httpMethodKey = "httpMethod"

/**
 * original request method. saved in [io.vertx.core.eventbus.DeliveryOptions.headers]
 */
var MultiMap.httpMethod: String?
  get() = get(httpMethodKey)
  set(value) {
    if (value == null) remove(httpMethodKey)
    else set(httpMethodKey, value)
  }

private const val pathKey = "path"

/**
 * original request path. saved in [io.vertx.core.eventbus.DeliveryOptions.headers]
 */
var MultiMap.path: String?
  get() = get(pathKey)
  set(value) {
    if (value == null) remove(pathKey)
    else set(pathKey, value)
  }
