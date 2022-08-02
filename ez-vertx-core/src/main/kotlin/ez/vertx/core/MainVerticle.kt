package ez.vertx.core

import com.fasterxml.jackson.databind.DeserializationFeature
import ez.vertx.core.config.ConfigLoaderVerticle
import ez.vertx.core.config.VertxConfigVerticle
import ez.vertx.core.handler.DeployHandler
import ez.vertx.core.busi.HttpServerVerticle
import io.vertx.core.DeploymentOptions
import io.vertx.core.Vertx
import io.vertx.core.VertxOptions
import io.vertx.core.json.jackson.DatabindCodec
import io.vertx.kotlin.coroutines.CoroutineVerticle
import io.vertx.kotlin.coroutines.await
import org.slf4j.LoggerFactory
import java.io.File
import java.util.*

class MainVerticle : CoroutineVerticle() {
  companion object {
    private val logger = LoggerFactory.getLogger(MainVerticle::class.java)!!

    init {
      DatabindCodec.mapper()
        .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
      DatabindCodec.prettyMapper()
        .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
    }
  }

  override suspend fun start() {
    // begin
    logger.info("cwd: {}", File(".").absolutePath)
    // load config json base config(read config json value and map to VertxOptions)
    vertx.deployVerticle(ConfigLoaderVerticle()).await()
    val vertxConfigVerticle = VertxConfigVerticle()
    vertx.deployVerticle(vertxConfigVerticle).await()
    // use VertxOptions to create new Vertx instance
    val vertxOptions: VertxOptions = vertxConfigVerticle.configValue
    val newVertx = Vertx.vertx(vertxOptions)
    // http server verticle is special: use event loop pool size as instance amount
    newVertx.deployVerticle(
      HttpServerVerticle::class.java,
      DeploymentOptions()
        .setInstances(vertxOptions.eventLoopPoolSize)
    ).await()
    // auto deploy verticles
    for (verticle in ServiceLoader.load(AutoDeployVerticle::class.java)) {
      logger.info("auto deploy verticle: {}", verticle)
      newVertx.deployVerticle(verticle).await()
    }
    // deploy dynamic verticles
    DeployHandler.deployDir(newVertx, "verticles")
    // end
    logger.info("MainVerticle started")
    // close old Vertx instance. should not use await here because waiting in start will block close
    vertx.close {
      logger.info("launcher vertx instance closed")
    }
  }
}
