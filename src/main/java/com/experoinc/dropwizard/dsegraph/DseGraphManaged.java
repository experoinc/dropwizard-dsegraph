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

import com.datastax.driver.core.CloseFuture;
import com.datastax.driver.dse.DseCluster;
import io.dropwizard.lifecycle.Managed;
import io.dropwizard.util.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * @author Ted Wilmes
 */
public class DseGraphManaged implements Managed {

    private static final Logger LOG = LoggerFactory.getLogger(DseGraphManaged.class);

    private final DseCluster cluster;
    private final Duration shutdownTimeout;

    public DseGraphManaged(DseCluster cluster, Duration shutdownTimeout) {
        this.cluster = cluster;
        this.shutdownTimeout = shutdownTimeout;
    }

    public void start() throws Exception {
        // no-op
    }

    public void stop() throws Exception {
        LOG.info("Attempting to shutdown TinkerPop cluster connection.");

        CloseFuture future = cluster.closeAsync();
        try {
            future.get(shutdownTimeout.toMilliseconds(), TimeUnit.MILLISECONDS);
        } catch (TimeoutException ex) {
            LOG.warn("Unable to close TinkerPop cluster after {}", shutdownTimeout);
        }
    }
}
