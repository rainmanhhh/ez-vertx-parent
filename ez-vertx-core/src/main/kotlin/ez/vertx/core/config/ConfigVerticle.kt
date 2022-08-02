package ez.vertx.core.config

import com.fasterxml.jackson.databind.node.ObjectNode
import com.github.victools.jsonschema.generator.OptionPreset
import com.github.victools.jsonschema.generator.SchemaGenerator
import com.github.victools.jsonschema.generator.SchemaGeneratorConfigBuilder
import com.github.victools.jsonschema.generator.SchemaVersion
import ez.vertx.core.AutoDeployVerticle
import ez.vertx.core.config.ConfigVerticle.Companion.configMap
import ez.vertx.core.util.VertxUtil
import io.vertx.core.buffer.Buffer
import io.vertx.core.json.JsonObject
import io.vertx.kotlin.coroutines.CoroutineVerticle
import io.vertx.kotlin.coroutines.await
import org.slf4j.LoggerFactory
import kotlin.reflect.KProperty

/**
 * - All config verticles will be deployed after [ConfigLoaderVerticle] **one by one**
 * - A config verticle generates its config json schema by [ConfigType]
 * - Config value will be picked from [ConfigLoaderVerticle] and parsed into [ConfigType], then stored at [configMap]
 * - [ConfigType] should be json serializable(as a [JsonObject])
 */
abstract class ConfigVerticle<ConfigType : Any> : AutoDeployVerticle, CoroutineVerticle() {
  companion object {
    private val logger = LoggerFactory.getLogger(ConfigVerticle::class.java)
    private val configMap = HashMap<String, Any>()

    @JvmStatic
    @Suppress("UNCHECKED_CAST")
    fun <C : Any> get(key: String) = configMap[key] as? C
      ?: throw RuntimeException("config not found! key: $key")

    operator fun <C : Any> getValue(nothing: Nothing?, property: KProperty<*>): C {
      return get(property.name)
    }
  }

  abstract val key: String

  /**
   * - Should be init by child class with default config value
   * - Config data picked from [ConfigLoaderVerticle] will be merged into it
   */
  abstract var configValue: ConfigType

  final override suspend fun start() {
    try {
      logger.debug("generate json schema for [{}]", key)
      generateConfigSchema(ConfigLoaderVerticle.configDir, "$key.schema.json")
    } catch (e: Throwable) {
      logger.warn("generate json schema for [{}] failed", key, e)
    }
    val configJson = ConfigLoaderVerticle.configJson.getJsonObject(key, JsonObject())
    configValue = configJson.mapTo(configValue.javaClass)
    configMap[key] = configValue
    afterConfig()
  }

  /**
   * Do something after config(like create beans)
   */
  open suspend fun afterConfig() {}

  private fun ObjectNode.getObj(childName: String) =
    get(childName) as ObjectNode? ?: putObject(childName)

  private suspend fun generateConfigSchema(configDir: String, fileName: String) {
    val cv = configValue
    val configBuilder = SchemaGeneratorConfigBuilder(SchemaVersion.DRAFT_7, OptionPreset.PLAIN_JSON)
    val typeCache = mutableMapOf<Class<*>, Any>()
    configBuilder.forFields()
      .withDefaultResolver {
        it.findGetter()?.let { getter ->
          val type = getter.declaringTypeMembers.mainTypeAndOverrides()[0].erasedType
          val method = getter.rawMember
          if (type == cv.javaClass) method.invoke(cv)
          else {
            val instance = typeCache.computeIfAbsent(type) {
              type.getDeclaredConstructor().newInstance()
            }
            method.invoke(instance)
          }
        }
      }
      .withDescriptionResolver {
        it.rawMember.getAnnotation(Description::class.java)?.value
      }
      .withEnumResolver {
        val erasedType = it.type.erasedType
        if (erasedType.isEnum) {
          erasedType.enumConstants.toList()
        } else null
      }
    val config = configBuilder.build()
    val generator = SchemaGenerator(config)
    val jsonSchema = generator.generateSchema(cv.javaClass)

    val defsNode = jsonSchema.getObj("definitions")
    defsNode.putObject(cv.javaClass.name)
      .put("type", "object")
      .putPOJO("properties", jsonSchema.getObj("properties"))
    jsonSchema.putObject("properties")
      .putObject(key)
      .put("\$ref", "#/definitions/" + cv.javaClass.name)

    val configSchemaStr = jsonSchema.toPrettyString()
    val vertx = VertxUtil.vertx()
    val schemaDir = "$configDir/schema"
    vertx.fileSystem().mkdirs(schemaDir)
    vertx.fileSystem().writeFile("$schemaDir/$fileName", Buffer.buffer(configSchemaStr)).await()
  }
}
