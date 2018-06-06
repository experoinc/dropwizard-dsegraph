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
        <dropwizard-desgraph.version>1.4.0</dropwizard-desgraph.version>
        <dropwizard.version>1.2.5</dropwizard.version>
        <desgraph.version>1.2.6</desgraph.version>
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
    </dependencies>
```
#### ApplicationConfig.java
Include a `DseGraphConfiguration` instance in your application config
```java
public class ApplicationConfig extends Configuration {
    @Getter
    @Valid
    @NonNull
    private DseGraphBundleConfiguration dseGraph;
}
```
#### Config.yml
Define the graph properties:
```yaml
dseGraph:
  graphName: ${DB_NAME:-dse_graph}
  #port: 9042 # WARNING: port is non-functional currently, but coming soon
  contactPoints:
    - ${DB_HOST:-1.2.3.4}
    - ${DB_HOST:-5.6.7.8}
  # These are optional and defaulted to the values below
  shutdownTimeout: 60000
  validationQuery: "g.inject(1).hasNext()"
  validationQueryTimeout: 10000
```

#### Optional Security Configuration: Authentication
Optionally, [DSE authentication][3] can be enabled by including `userName` & `password` values:  
```yaml
dseGraph:
<...>
  userName: ${DB_USER:-username}
  password: ${DB_PASS:-password}
```

#### Optional Security Configuration: SSL Encryption
Optionally, [SSL encrypted conections to the cluster][4] can be configured. This approach uses truststore and keystore files which were genereated with OpenSSL and Java `keytool` as described in [Setting up SSL certificates][5] in the DataStax documentation 

Basic SSL only requires a truststore with the DSE cluster public certificates or the public key which has signed the certificates. This is configured with `sslTruststoreFile` and `sslTruststorePassword`.

If authentication of the client certificates is also required, this is configured with `sslKeystoreFile` and `sslKeystorePassword`.
```yaml
dseGraph:
<...>
 sslTruststoreFile: ${SSL_TRUSTSTORE_FILE:-\path\to\client.truststore}
 sslTruststorePassword: "${SSL_TRUSTSTORE_PASSWORD:-sslTruststorePassword}"
 sslKeystoreFile: ${SSL_KEYSTORE_FILE:-\path\to\client.keystore}
 sslKeystorePassword: "${SSL_KEYSTORE_PASSWORD:-sslKeystorePassword}"
```

#### Application.java
Instantiate and add the DseGraph bundle in your `Application` class and refer to it in the `run(ApplicationConfig configuration, Environment environment)` method:
```java
public class App extends Application<ApplicationConfig> {
    
    private final DseGraphBundle<ApplicationConfig> dseGraphBundle =
        new DseGraphBundle<ApplicationConfig>() {
            @Override
            protected DseGraphBundleConfiguration getDseGraphBundleConfiguration(ApplicationConfig configuration) {
                return configuration.getDseGraph();
            }
        };

    @Override
    public void initialize(Bootstrap<ApplicationConfig> bootstrap) {
        bootstrap.addBundle(dseGraphBundle);
    }

    @Override
    public void run(final ApplicationConfig configuration, final Environment environment) {
        environment.jersey().register(new MyClusterResource(dseGraphBundle.getCluster()));
        environment.jersey().register(new MySessionResource(dseGraphBundle.getSession()));
        environment.jersey().register(new MyTraversalResource(dseGraphBundle.getG()));
    }
}
```

[1]: https://dropwizard.io
[2]: https://www.datastax.com/products/datastax-enterprise-graph
[3]: http://docs.datastax.com/en/developer/java-driver-dse/1.4/manual/auth/
[4]: http://docs.datastax.com/en/developer/java-driver-dse/1.4/manual/ssl/
[5]: http://docs.datastax.com/en/dse/5.1/dse-admin/datastax_enterprise/security/secSetUpSSLCert.html

