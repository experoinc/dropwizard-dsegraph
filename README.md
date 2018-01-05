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

## Dependency Managed Usage ##

This utility includes references to DSE Graph and DropWizard. To maintain correct
versions of these 3rd party libraries, there is a Dependency Management section in
the POM. To use it, include the following in your pom:

Top Level:
```xml
    <properties>
        <dropwizard-desgraph.version>1.2.2-1.2.4-SNAPSHOT</dropwizard-desgraph.version>
    </properties>
```

```xml
    <dependencyManagement>
        <dependencies>
            <dependency>
                <groupId>com.experoinc</groupId>
                <artifactId>dropwizard-dsegraph</artifactId>
                <version>${dropwizard-desgraph.version}</version>
                <type>pom</type>
                <scope>import</scope>
            </dependency>
        </dependencies>
    </dependencyManagement>
```

Dependencies Section (add or remove dependencies as needed - Dependency Management will auto-assign the version):
```xml
    <dependencies>
        <dependency>
            <groupId>com.experoinc</groupId>
            <artifactId>dropwizard-dsegraph</artifactId>
            <version>${dropwizard-desgraph.version}</version>
        </dependency>
        <dependency>
            <groupId>io.dropwizard</groupId>
            <artifactId>dropwizard-forms</artifactId>
        </dependency>
        <dependency>
            <groupId>com.datastax.dse</groupId>
            <artifactId>dse-java-driver-graph</artifactId>
        </dependency>
        <dependency>
            <groupId>com.datastax.dse</groupId>
            <artifactId>dse-java-driver-core</artifactId>
        </dependency>
        <dependency>
            <groupId>com.datastax.dse</groupId>
            <artifactId>dse-java-driver-extras</artifactId>
        </dependency>
        <dependency>
            <groupId>com.datastax.dse</groupId>
            <artifactId>dse-java-driver-mapping</artifactId>
        </dependency>
        <dependency>
            <groupId>org.slf4j</groupId>
            <artifactId>slf4j-api</artifactId>
        </dependency>
    </dependencies>
```

Include a `DseGraphFactory` instance in your application config

```java
public class ApplicationConfig extends Configuration {
    @Valid
    @NotNull
    private DseGraphFactory dseGraphFactory = new DseGraphFactory();

    @JsonProperty("graphFactory")
    public DseGraphFactory getDseGraphFactory() {
        return dseGraphFactory;
    }

    @JsonProperty("graphFactory")
    public void setDseGraphFactory(DseGraphFactory dseGraphFactory) {
        this.dseGraphFactory = dseGraphFactory;
    }
}
```

Include a section in config.yml to define the graph properties:

```yaml
graphFactory:
 graphName: ${DPS_DB_NAME:-dps_graph}
 contactPoints:
   - ${DPS_DB_HOST:-1.2.3.4}
```

Build the DSEGraph cluster in your applications `run(ApplicationConfig configuration, Environment environment)` method.

```java
public class App extends Application<ApplicationConfig> {
    
    @Override
    public void run(final ApplicationConfig configuration,
                    final Environment environment) {
        DseGraphFactory graphFactory = configuration.getDseGraphFactory();

        DseCluster c = graphFactory.build(environment);
        DseSession s = c.newSession();
        GraphTraversalSource g = DseGraph.traversal(s);

        environment.jersey().register(new MyResource(g));
    }
}
```

[1]: https://dropwizard.io
[2]: https://www.datastax.com/products/datastax-enterprise-graph
