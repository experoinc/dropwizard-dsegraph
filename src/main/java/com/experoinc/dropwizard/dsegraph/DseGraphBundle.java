/*
 * Copyright 2018 Expero, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package com.experoinc.dropwizard.dsegraph;

import com.datastax.driver.core.RemoteEndpointAwareJdkSSLOptions;
import com.datastax.driver.dse.DseCluster;
import com.datastax.driver.dse.DseSession;
import com.datastax.driver.dse.graph.GraphOptions;
import com.datastax.dse.graph.api.DseGraph;
import io.dropwizard.Configuration;
import io.dropwizard.ConfiguredBundle;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import lombok.Getter;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource;
import org.bouncycastle.jce.provider.BouncyCastleProvider;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;
import java.io.FileInputStream;
import java.io.IOException;
import java.security.*;
import java.security.cert.CertificateException;

/**
 * @author Dan Sorak, Ted Wilmes
 */
public abstract class DseGraphBundle<T extends Configuration> implements ConfiguredBundle<T> {

    @Getter
    private DseCluster cluster;

    @Getter
    private DseSession session;

    @Getter
    private GraphTraversalSource g;

    //    @Override
    public void initialize(Bootstrap<?> bootstrap) {
        Security.addProvider(new BouncyCastleProvider());

//        bootstrap.addBundle(new ViewBundle<Configuration>());
//        ModelConverters.getInstance()
//                       .addConverter(new ModelResolver(bootstrap.getObjectMapper()));
    }

    //    @Override
    public void run(T configuration, Environment environment) throws Exception {
        final DseGraphBundleConfiguration dseGraphBundleConfiguration = getDseGraphBundleConfiguration(configuration);
        if (dseGraphBundleConfiguration == null) {
            throw new IllegalStateException(
                    "You need to provide an instance of DseGraphBundleConfiguration");
        }

//    public DseCluster build(Environment environment) throws CertificateException, UnrecoverableKeyException, NoSuchAlgorithmException, KeyStoreException, KeyManagementException, IOException {
        DseCluster.Builder builder = DseCluster.builder()
                                               .addContactPoints(dseGraphBundleConfiguration.getContactPoints())
                                               .withGraphOptions(new GraphOptions().setGraphName(dseGraphBundleConfiguration.getGraphName()));

        if (null != dseGraphBundleConfiguration.getUserName()
            && dseGraphBundleConfiguration.getUserName().length() > 0
            && null != dseGraphBundleConfiguration.getPassword()
            && dseGraphBundleConfiguration.getPassword().length() > 0) {
            builder = builder.withCredentials(dseGraphBundleConfiguration.getUserName(), dseGraphBundleConfiguration.getPassword());
        }

        if (null != dseGraphBundleConfiguration.getSslTruststoreFile()
            && dseGraphBundleConfiguration.getSslTruststoreFile().length() > 0
            && null != dseGraphBundleConfiguration.getSslTruststorePassword()
            && dseGraphBundleConfiguration.getSslTruststorePassword().length() > 0) {
//            builder = withSSL(builder, configuration);
            builder = withSSL(builder, dseGraphBundleConfiguration);
        }

        cluster = builder.build();
        session = cluster.newSession();
        g = DseGraph.traversal(session);

        environment.lifecycle().manage(new DseGraphManaged(cluster, dseGraphBundleConfiguration.getShutdownTimeout()));
        environment.healthChecks().register("dsegraph",
                                            new DseGraphHealthCheck(session,
                                                                    dseGraphBundleConfiguration.getValidationQuery(),
                                                                    dseGraphBundleConfiguration.getValidationQueryTimeout()));

//        final ConfigurationHelper configurationHelper = new ConfigurationHelper(configuration, dseGraphBundleConfiguration);
//        new AssetsBundle("/swagger-static", configurationHelper.getSwaggerUriPath(), null, "swagger-assets").run(environment);
//        new AssetsBundle("/swagger-static/oauth2-redirect.html", configurationHelper.getOAuth2RedirectUriPath(), null, "swagger-oauth2-connect").run(environment);
//
//        dseGraphBundleConfiguration.build(configurationHelper.getUrlPattern());
//
//        FilterFactory.setFilter(new AuthParamFilter());
//
//        environment.jersey().register(new ApiListingResource());
//        environment.jersey().register(new SwaggerSerializers());
//        if (dseGraphBundleConfiguration.isIncludeSwaggerResource()) {
//            environment.jersey()
//                    .register(new SwaggerResource(
//                            configurationHelper.getUrlPattern(),
//                            dseGraphBundleConfiguration
//                                    .getSwaggerViewConfiguration(),
//                            dseGraphBundleConfiguration.getSwaggerOAuth2Configuration(),
//                            dseGraphBundleConfiguration.getContextRoot()));
//        }
    }

    protected abstract DseGraphBundleConfiguration getDseGraphBundleConfiguration(T configuration);

    private DseCluster.Builder withSSL(DseCluster.Builder builder, DseGraphBundleConfiguration dseGraphBundleConfiguration) throws KeyStoreException, NoSuchAlgorithmException, IOException, CertificateException, KeyManagementException, UnrecoverableKeyException {
//    private DseCluster.Builder withSSL(DseCluster.Builder builder, T configuration) throws KeyStoreException, NoSuchAlgorithmException, IOException, CertificateException, KeyManagementException, UnrecoverableKeyException {
//        final DseGraphBundleConfiguration dseGraphBundleConfiguration = getDseGraphBundleConfiguration(configuration);

        // JKS Truststore
        KeyStore truststore = KeyStore.getInstance("JKS");
        truststore.load(new FileInputStream(dseGraphBundleConfiguration.getSslTruststoreFile()), dseGraphBundleConfiguration.getSslTruststorePassword().toCharArray());
        TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        tmf.init(truststore);

        SSLContext sslContext = SSLContext.getInstance("TLSv1.2");

        // Keystore details means supporting client authentication
        if (null != dseGraphBundleConfiguration.getSslKeystoreFile()
            && dseGraphBundleConfiguration.getSslKeystoreFile().length() > 0
            && null != dseGraphBundleConfiguration.getSslKeystorePassword()
            && dseGraphBundleConfiguration.getSslKeystorePassword().length() > 0) {
            KeyStore keystore = KeyStore.getInstance("JKS");
            keystore.load(new FileInputStream(dseGraphBundleConfiguration.getSslKeystoreFile()), dseGraphBundleConfiguration.getSslKeystorePassword().toCharArray());
            KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
            kmf.init(keystore, dseGraphBundleConfiguration.getSslKeystorePassword().toCharArray());

            sslContext.init(kmf.getKeyManagers(), tmf.getTrustManagers(), new java.security.SecureRandom());
        } else {
            sslContext.init(null, tmf.getTrustManagers(), new java.security.SecureRandom());
        }

        return builder.withSSL(RemoteEndpointAwareJdkSSLOptions.builder().withSSLContext(sslContext).build());
    }
}
