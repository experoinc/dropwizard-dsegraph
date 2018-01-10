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
import io.dropwizard.setup.Environment;
import io.dropwizard.util.Duration;
import io.dropwizard.validation.MinDuration;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import org.hibernate.validator.constraints.NotEmpty;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
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

    @Getter
    @Setter
    @NonNull
    @NotEmpty
    private String validationQuery = "g.V().hasNext()";
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
    @NonNull
    private Duration shutdownTimeout = Duration.seconds(60);

    @JsonIgnore
    public DseCluster build(Environment environment) {
        DseCluster.Builder builder = DseCluster.builder()
                .addContactPoints(contactPoints)
                .withGraphOptions(new GraphOptions().setGraphName(graphName));

        if (null != userName && userName.length() > 0 && null != password && password.length() > 0) {
            builder = builder.withCredentials(userName, password);
        }

        DseCluster cluster = builder.build();
        DseSession session = cluster.newSession();

        environment.lifecycle().manage(new DseGraphManaged(cluster, getShutdownTimeout()));
        environment.healthChecks().register("dsegraph",
                new DseGraphHealthCheck(session, validationQuery, validationQueryTimeout));

        return cluster;
    }
}
