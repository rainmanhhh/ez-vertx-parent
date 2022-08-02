package ez.vertx.core.util

import io.vertx.core.MultiMap
import io.vertx.core.json.JsonObject

fun JsonObject.toMultiMap(): MultiMap {
  val m = MultiMap.caseInsensitiveMultiMap()
  for (entry in this) {
    entry.value?.let {
      m.set(entry.key, it.toString())
    }
  }
  return m
}
