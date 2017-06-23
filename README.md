# dropwizard-dsegraph #

A [Dropwizard][1] library that enables connectivity to [DSEGraph][2] enabled gremlin servers.

## Components ##

* Configuration
* Health Check
* Managed Cluster

### Configuration ###

The configuration class includes all of the parameters required to configure a remote connection to
a DSEGraph cluster. The default values correspond to all of the defaults specified in the DSEGraph
cluster builder.

### Health Check ###

A health check is setup using the dropwizard health check registry. It executes the 
`validationQuery` specified in the configuration. It will succeed if the query responds within the
`validationQueryTimeout` specified.

### Managed Connection ###

The `Cluster` built from the `DseGraphFactory` is included in dropwizard's managed lifecycle. This
will allow dropwizard to try and gracefully shut down the DSEGraph client connections on shutdown.

## Usage ##

Include the dropwizard-dsegraph library as a dependency

```xml
<dependency>
  <groupId>com.experoinc</groupId>
  <artifactId>dropwizard-dsegraph</artifactId>
  <version>${dropwizard-dsegraph.version}</version>
</dependency>
```

Include a `DseGraphFactory` instance in your application config

```java
public class AppConfig extends Configuration {

    @Valid
    @NotNull
    private DseGraphFactory dseGraphFactory = new DseGraphFactory();

    @JsonProperty("dsegraph")
    public DseGraphFactory getDseGraphFactory() {
        return cassandra;
    }

    @JsonProperty("dseGraph")
    public void setDseGraphFactory(DseGraphFactory dseGraphFactory) {
        this.dseGraphFactory = dseGraphFactory;
    }
}
```

Build the DSEGraph cluster in your applications `run(AppConfig ac, Environment environment)` method.

```java
public class App extends Application<AppConfig> {
    
    @Override
    public void run(AppConfig configuration, Environment environment) throws Exception {
        DseGraphFactory graphFactory = configuration.getGraphFactory();
        DseCluster cluster = graphFactory.build(environment);
    }
}
```

[1]: https://dropwizard.io
[2]: https://www.datastax.com/products/datastax-enterprise-graph
