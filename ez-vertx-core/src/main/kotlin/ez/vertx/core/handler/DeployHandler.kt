package ez.vertx.core.handler

import ez.vertx.core.busi.ApiKey
import ez.vertx.core.util.paramsAsJson
import io.vertx.core.Vertx
import io.vertx.core.json.Json
import io.vertx.ext.web.RoutingContext
import io.vertx.kotlin.coroutines.await
import kotlinx.coroutines.CoroutineScope
import org.slf4j.LoggerFactory
import java.nio.file.Paths

class DeployHandler(scope: CoroutineScope) : CoroutineHandler(scope) {
  companion object {
    private const val MAP_VERTICLES = "MAP_VERTICLES"
    private val logger = LoggerFactory.getLogger(DeployHandler::class.java)

    suspend fun deployDir(vertx: Vertx, dir: String) {
      val absoluteRoot = Paths.get(dir).toAbsolutePath().toString().replace('\\', '/')
      deployDir(vertx, dir, dir, absoluteRoot)
    }

    private suspend fun deployDir(vertx: Vertx, dir: String, root: String, absoluteRoot: String) {
      val fs = vertx.fileSystem()
      val list = fs.readDir(dir).await()
      for (it in list) {
        val fileProps = fs.lprops(it).await()
        if (fileProps.isDirectory) {
          deployDir(vertx, it, root, absoluteRoot)
        } else {
          val normalizedPath = it.replace('\\', '/')
          val relativePath = normalizedPath.replace(absoluteRoot, root)
          deploy(vertx, relativePath)
        }
      }
    }

    /**
     * @param verticleName relative path of cwd, or absolute path
     */
    suspend fun deploy(vertx: Vertx, verticleName: String): String {
      logger.info("deploy verticle, name: {}", verticleName)
      val map = vertx.sharedData().getAsyncMap<String, String>(MAP_VERTICLES).await()
      val id = map.get(verticleName).await()
      if (id != null) {
        logger.info("undeploy existing instance, id: {}", id)
        vertx.undeploy(id).await()
      }
      val newId = vertx.deployVerticle(verticleName).await()
      map.put(verticleName, newId).await()
      val msg = "deployed verticle, id: $newId"
      logger.info(msg)
      return msg
    }
  }

  override suspend fun handleAsync(ctx: RoutingContext): Boolean {
    val vertx = ctx.vertx()
    val jsonParams = ctx.paramsAsJson()
    ApiKey.check(jsonParams)
    val verticleName = jsonParams.getString("verticleName")
    if (verticleName.isNullOrBlank()) {
      val map = vertx.sharedData()
        .getAsyncMap<String, String>(MAP_VERTICLES).await()
        .entries().await()
      ctx.response().end(Json.encodeToBuffer(map))
    } else {
      val msg = deploy(vertx, verticleName)
      ctx.response().end(msg)
    }
    return false
  }
}
