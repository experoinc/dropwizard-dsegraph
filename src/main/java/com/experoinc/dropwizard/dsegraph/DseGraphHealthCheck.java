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
import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.ResultSetFuture;
import com.datastax.driver.dse.DseSession;
import io.dropwizard.util.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * A {@link HealthCheck} that validates the servers ability to connect to the DSE server
 *
 * @author Ted Wilmes
 * @author Dan Sorak - updated query call and log messages
 */
public class DseGraphHealthCheck extends HealthCheck {

    private static final Logger LOGGER = LoggerFactory.getLogger(DseGraphHealthCheck.class);

    private final DseSession session;
    private final String validationQuery;
    private final Duration validationTimeout;

    public DseGraphHealthCheck(
            DseSession session,
            String validationQuery,
            Duration validationTimeout) {

        this.session = session;
        this.validationQuery = validationQuery;
        this.validationTimeout = validationTimeout;
    }

    @Override
    protected Result check() {

        try {
            ResultSetFuture future = session.executeAsync(validationQuery);
            ResultSet result = future.get(validationTimeout.toMilliseconds(), TimeUnit.MILLISECONDS);

            String msg = String.format("Validation query completed: '%s' = '%s'",
                    validationQuery,
                    result.toString());
            LOGGER.info(msg);
            return Result.healthy();

        } catch (TimeoutException ex) {
            String msg = String.format("Validation query was unable to complete after %d ms: '%s'",
                    validationTimeout.toMilliseconds(),
                    validationQuery);

            LOGGER.error(msg);
            return Result.unhealthy(msg);
        } catch (Exception ex) {
            String msg = String.format("Validation query was unable to execute: '%s' (%s)",
                    validationQuery,
                    ex.getMessage());
            LOGGER.error(msg);
            return Result.unhealthy(msg);
        }
    }
}
