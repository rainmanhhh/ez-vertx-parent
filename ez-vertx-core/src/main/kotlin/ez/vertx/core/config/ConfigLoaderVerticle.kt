package ez.vertx.core.config

import io.vertx.config.ConfigRetriever
import io.vertx.config.ConfigRetrieverOptions
import io.vertx.config.ConfigStoreOptions
import io.vertx.core.json.JsonObject
import io.vertx.kotlin.core.json.jsonArrayOf
import io.vertx.kotlin.core.json.jsonObjectOf
import io.vertx.kotlin.coroutines.CoroutineVerticle
import io.vertx.kotlin.coroutines.await
import org.slf4j.LoggerFactory

@Suppress("SameParameterValue")
class ConfigLoaderVerticle : CoroutineVerticle() {
  companion object {
    private val logger = LoggerFactory.getLogger(ConfigLoaderVerticle::class.java)
    const val configDir = "conf"
    lateinit var configJson: JsonObject
  }

  override suspend fun start() {
    logger.info("reading config...")
    configJson = ConfigRetriever.create(
      vertx,
      ConfigRetrieverOptions().addStore(
        ConfigStoreOptions()
          .setType("directory")
          .setConfig(
            jsonObjectOf(
              "path" to configDir,
              "filesets" to jsonArrayOf(
                configFileAttrs("*.yml"),
                configFileAttrs("*.yaml")
              )
            )
          )
      )
    ).config.await()
    logger.debug("configJson: {}", configJson.encodePrettily())
  }

  private fun configFileAttrs(pattern: String, format: String = "yaml") = JsonObject()
    .put("format", format)
    .put("pattern", pattern)
}
