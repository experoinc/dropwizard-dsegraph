/*
 * Copyright 2017 Expero, Inc.
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
import com.fasterxml.jackson.annotation.JsonIgnore;
import io.dropwizard.setup.Environment;
import io.dropwizard.util.Duration;
import io.dropwizard.validation.MinDuration;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import org.hibernate.validator.constraints.NotEmpty;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;
import javax.validation.constraints.Min;
import java.io.FileInputStream;
import java.io.IOException;
import java.security.*;
import java.security.cert.CertificateException;
import java.util.concurrent.TimeUnit;

public class DseGraphFactory {

    private static final int DEFAULT_PORT = 8182;

    @Getter
    @Setter
    @NonNull
    @NotEmpty
    private String[] contactPoints;

    @Getter
    @Setter
    @Min(1)
    private int port = DEFAULT_PORT;

    // NOTE: This can either be a traversal or a CQL command. The healthcheck code determines
    //       which it is by checking for the prefix "g.". If it starts with that, it is assumed
    //       to be a traversal and "executeGraphAsync()" is called. Otherwise, it is assumed to
    //       be CQL, which causes "executeAsync()" to be called instead.
    @Getter
    @Setter
    @NonNull
    @NotEmpty
    private String validationQuery = "g.inject(1).hasNext()";
    //private String validationQuery = "select release_version from system.local";

    @Getter
    @Setter
    @NonNull
    @MinDuration(value = 1, unit = TimeUnit.MILLISECONDS)
    private Duration validationQueryTimeout = Duration.seconds(10);

    @Getter
    @Setter
    @NonNull
    @NotEmpty
    private String graphName;

    @Getter
    @Setter
    private String userName;

    @Getter
    @Setter
    private String password;

    @Getter
    @Setter
    private String sslTruststoreFile;

    @Getter
    @Setter
    private String sslTruststorePassword;

    @Getter
    @Setter
    private String sslKeystoreFile;

    @Getter
    @Setter
    private String sslKeystorePassword;

    @Getter
    @Setter
    @NonNull
    private Duration shutdownTimeout = Duration.seconds(60);

    @JsonIgnore
    public DseCluster build(Environment environment) throws CertificateException, UnrecoverableKeyException, NoSuchAlgorithmException, KeyStoreException, KeyManagementException, IOException {
        DseCluster.Builder builder = DseCluster.builder()
                .addContactPoints(contactPoints)
                .withGraphOptions(new GraphOptions().setGraphName(graphName));

        if (null != userName && userName.length() > 0 && null != password && password.length() > 0) {
            builder = builder.withCredentials(userName, password);
        }

        if (null != sslTruststoreFile && sslTruststoreFile.length() > 0 && null != sslTruststorePassword && sslTruststorePassword.length() > 0) {
            builder = withSSL(builder);
        }

        DseCluster cluster = builder.build();
        DseSession session = cluster.newSession();

        environment.lifecycle().manage(new DseGraphManaged(cluster, getShutdownTimeout()));
        environment.healthChecks().register("dsegraph",
                new DseGraphHealthCheck(session, validationQuery, validationQueryTimeout));

        return cluster;
    }

    private DseCluster.Builder withSSL(DseCluster.Builder builder) throws KeyStoreException, NoSuchAlgorithmException, IOException, CertificateException, KeyManagementException, UnrecoverableKeyException {

        // JKS Truststore
        KeyStore truststore = KeyStore.getInstance("JKS");
        truststore.load(new FileInputStream(sslTruststoreFile), sslTruststorePassword.toCharArray());
        TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        tmf.init(truststore);

        SSLContext sslContext = SSLContext.getInstance("TLSv1.2");

        // Keystore details means supporting client authentication
        if (null != sslKeystoreFile && sslKeystoreFile.length() > 0 && null != sslKeystorePassword && sslKeystorePassword.length() > 0) {
            KeyStore keystore = KeyStore.getInstance("JKS");
            keystore.load(new FileInputStream(sslKeystoreFile), sslKeystorePassword.toCharArray());
            KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
            kmf.init(keystore, sslKeystorePassword.toCharArray());

            sslContext.init(kmf.getKeyManagers(), tmf.getTrustManagers(), new java.security.SecureRandom());
        } else {
            sslContext.init(null, tmf.getTrustManagers(), new java.security.SecureRandom());
        }

        return builder.withSSL(RemoteEndpointAwareJdkSSLOptions.builder().withSSLContext(sslContext).build());
    }
}
