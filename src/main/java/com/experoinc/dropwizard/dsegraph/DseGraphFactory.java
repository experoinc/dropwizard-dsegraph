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

import com.datastax.driver.dse.DseCluster;
import com.datastax.driver.dse.DseSession;
import com.datastax.driver.dse.graph.GraphOptions;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.dropwizard.setup.Environment;
import io.dropwizard.util.Duration;
import io.dropwizard.validation.MinDuration;
import org.hibernate.validator.constraints.NotEmpty;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import java.util.concurrent.TimeUnit;

/**
 * @author Ted Wilmes
 */
public class DseGraphFactory {

    private static final int DEFAULT_PORT = 8182;

    @NotEmpty
    private String[] contactPoints;

    @Min(1)
    private int port = DEFAULT_PORT;

    @NotEmpty
    private String validationQuery = "g.V().hasNext()";

    @NotNull @MinDuration(value = 1, unit = TimeUnit.MILLISECONDS)
    private Duration validationQueryTimeout = Duration.seconds(10);

    @NotEmpty
    private String graphName;

    @JsonProperty
    public String getGraphName() { return graphName; }

    @JsonProperty
    public void setGraphName(String graphName) { this.graphName = graphName; }

    @JsonProperty
    public String[] getContactPoints() {
        return contactPoints;
    }

    @JsonProperty
    public void setContactPoints(String[] contactPoints) {
        this.contactPoints = contactPoints;
    }

    @JsonProperty
    public int getPort() {
        return port;
    }

    @JsonProperty
    public void setPort(int port) {
        this.port = port;
    }

    @JsonProperty
    public String getValidationQuery() {
        return validationQuery;
    }

    @JsonProperty
    public void setValidationQuery(String validationQuery) {
        this.validationQuery = validationQuery;
    }

    @JsonProperty
    public Duration getValidationQueryTimeout() {
        return validationQueryTimeout;
    }

    @NotNull
    private Duration shutdownTimeout = Duration.seconds(60);

    @JsonProperty
    public void setValidationQueryTimeout(Duration validationQueryTimeout) {
        this.validationQueryTimeout = validationQueryTimeout;
    }

    @JsonProperty
    public Duration getShutdownTimeout() {
        return shutdownTimeout;
    }

    @JsonProperty
    public void setShutdownTimeout(Duration shutdownTimeout) {
        this.shutdownTimeout = shutdownTimeout;
    }

    @JsonIgnore
    public DseCluster build(Environment environment) {
        DseCluster cluster = DseCluster.builder().
                addContactPoints(contactPoints).
                withGraphOptions(new GraphOptions().setGraphName(graphName)).
                build();

        DseSession session = cluster.newSession();

        environment.lifecycle().manage(new DseGraphManaged(cluster, getShutdownTimeout()));
        environment.healthChecks().register("dsegraph",
                new DseGraphHealthCheck(session, validationQuery, validationQueryTimeout));

        return cluster;
    }
}
