package ez.vertx.core.config

@Target(AnnotationTarget.CLASS, AnnotationTarget.FIELD)
annotation class Description(
  val value: String = ""
)
