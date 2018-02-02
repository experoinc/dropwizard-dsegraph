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

This utility includes references to DSE Graph and DropWizard. To maintain correct
versions of these 3rd party libraries, there is a Dependency Management section in
the POM. To use it, include the following in your pom (NOTE: dropwizard-swagger is
only included as an example how to use the proper version to get swagger support):

#### Properties:
```xml
    <properties>
        <dropwizard-desgraph.version>1.2.2-1.2.4-SNAPSHOT</dropwizard-desgraph.version>
        <dropwizard.version>1.2.2</dropwizard.version>
        <desgraph.version>1.2.4</desgraph.version>
        <dropwizard-swagger.version>1.2.2-2</dropwizard-swagger.version>
    </properties>
```
#### Dependency Management:
```xml
    <dependencyManagement>
        <dependencies>
            <dependency>
                <groupId>io.dropwizard</groupId>
                <artifactId>dropwizard-bom</artifactId>
                <version>${dropwizard.version}</version>
                <type>pom</type>
                <scope>import</scope>
            </dependency>
        </dependencies>
    </dependencyManagement>
```
#### Dependencies:
```xml
    <dependencies>
        <dependency>
            <groupId>com.experoinc</groupId>
            <artifactId>dropwizard-dsegraph</artifactId>
            <version>${dropwizard-desgraph.version}</version>
        </dependency>
        <dependency>
            <groupId>io.dropwizard</groupId>
            <artifactId>dropwizard-auth</artifactId>
        </dependency>
        <dependency>
            <groupId>io.dropwizard</groupId>
            <artifactId>dropwizard-forms</artifactId>
        </dependency>
        <dependency>
            <groupId>com.datastax.dse</groupId>
            <artifactId>dse-java-driver-graph</artifactId>
            <version>${desgraph.version}</version>
        </dependency>
        <dependency>
            <groupId>com.smoketurner</groupId>
            <artifactId>dropwizard-swagger</artifactId>
            <version>${dropwizard-swagger.version}</version>
        </dependency>
    </dependencies>
```
#### ApplicationConfig.java
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
#### Config.yml
Define the graph properties:
```yaml
graphFactory:
 graphName: ${DB_NAME:-dps_graph}
 contactPoints:
   - ${DB_HOST:-1.2.3.4}
```

#### Optional Security Configuration: Authentication
Optionally, [DSE authentication][3] can be enabled by including `userName` & `password` values.  
```yaml
graphFactory:
 graphName: ${DB_NAME:-dps_graph}
 userName: ${DB_USER:-username}
 password: ${DB_PASS:-password}
 contactPoints:
   - ${DB_HOST:-1.2.3.4}
```

#### Optional Security Configuration: SSL Encryption
Optionally, [SSL encrypted conections to the cluster][4] can be configured. This approach uses truststore and keystore files which were genereated with OpenSSL and Java `keytool` as described in [Setting up SSL certificates][5] in the DataStax documentation 

Basic SSL only requires a truststore with the DSE cluster public certificates or the public key which has signed the certificates. This is configured with `sslTruststoreFile` and `sslTruststorePassword`.

If authentication of the client certificates is also required, this is configured with `sslKeystoreFile` and `sslKeystorePassword`.
```yaml
graphFactory:
 graphName: ${DB_NAME:-dps_graph}
 sslTruststoreFile: ${SSL_TRUSTSTORE_FILE:-\path\to\client.truststore}
 sslTruststorePassword: ${SSL_TRUSTSTORE_PASSWORD:-sslTruststorePassword}
 sslKeystoreFile: ${SSL_KEYSTORE_FILE:-\path\to\client.keystore}
 sslKeystorePassword: ${SSL_KEYSTORE_PASSWORD:-sslKeystorePassword}
 contactPoints:
   - ${DB_HOST:-1.2.3.4}
```

#### Application.java
Build the DSEGraph cluster in your applications `run(ApplicationConfig configuration, Environment environment)` method:
```java
public class App extends Application<ApplicationConfig> {
    
    @Override
    public void run(final ApplicationConfig configuration,
                    final Environment environment) {
        DseGraphFactory graphFactory = configuration.useDseGraph();

        DseCluster c = graphFactory.build(environment);
        DseSession s = c.newSession();
        GraphTraversalSource g = DseGraph.traversal(s);

        environment.jersey().register(new MyResource(g));
    }
}
```

[1]: https://dropwizard.io
[2]: https://www.datastax.com/products/datastax-enterprise-graph
[3]: http://docs.datastax.com/en/developer/java-driver-dse/1.4/manual/auth/
[4]: http://docs.datastax.com/en/developer/java-driver-dse/1.4/manual/ssl/
[5]: http://docs.datastax.com/en/dse/5.1/dse-admin/datastax_enterprise/security/secSetUpSSLCert.html

