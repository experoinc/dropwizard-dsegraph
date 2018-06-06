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

import com.codahale.metrics.health.HealthCheck;
import com.datastax.driver.core.ResultSetFuture;
import com.datastax.driver.dse.DseSession;
import com.google.common.util.concurrent.ListenableFuture;
import io.dropwizard.util.Duration;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.validation.constraints.NotNull;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * A {@link HealthCheck} that validates the servers ability to connect to the DSE server
 *
 * @author Ted Wilmes
 * @author dan.sorak - updated query call and log messages
 */
@Slf4j
public class DseGraphHealthCheck extends HealthCheck {

    private final DseSession session;
    private final String validationQuery;
    private final Duration validationTimeout;

    public DseGraphHealthCheck(
            @NotNull DseSession session,
            @NotNull String validationQuery,
            @NotNull Duration validationTimeout) {

        this.session = session;
        this.validationQuery = validationQuery;
        this.validationTimeout = validationTimeout;
    }

    @Override
    protected Result check() {

        try {
            Object result;

            if (validationQuery.startsWith("g.")) {
                ListenableFuture future = session.executeGraphAsync(validationQuery);
                result = future.get(validationTimeout.toMilliseconds(), TimeUnit.MILLISECONDS);
            } else {
                ResultSetFuture future = session.executeAsync(validationQuery);
                result = future.get(validationTimeout.toMilliseconds(), TimeUnit.MILLISECONDS);
            }

            String msg = String.format("Validation query completed on %s (%s): '%s' = '%s'",
                    session.getCluster().getConfiguration().getGraphOptions().getGraphName(),
                    session.getCluster().getClusterName(),
                    validationQuery,
                    result.toString());
            log.info(msg);
            return Result.healthy(msg);

        } catch (TimeoutException ex) {
            String msg = String.format("Validation query was unable to complete after %d ms on %s (%s): '%s'",
                    validationTimeout.toMilliseconds(),
                    session.getCluster().getConfiguration().getGraphOptions().getGraphName(),
                    session.getCluster().getClusterName(),
                    validationQuery);

            log.error(msg);
            return Result.unhealthy(msg);

        } catch (Exception ex) {
            String msg = String.format("Validation query was unable to execute on %s (%s): '%s' (%s)",
                    session.getCluster().getConfiguration().getGraphOptions().getGraphName(),
                    session.getCluster().getClusterName(),
                    validationQuery,
                    ex.getMessage());
            log.error(msg);
            return Result.unhealthy(msg);
        }
    }
}
