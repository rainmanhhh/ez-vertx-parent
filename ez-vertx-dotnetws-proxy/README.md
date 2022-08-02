proxy service inherits `ez-vertx-dotnetws-proxy` should config `META-INF/services/ez.vertx.core.AutoDeployVerticle`

content:
```
ez.vertx.dotnetws.DotnetwsResVerticle
```
or
```
some.package.FooVerticle
```
`FooVerticle` should be child class of `DotnetwsResVerticle`
