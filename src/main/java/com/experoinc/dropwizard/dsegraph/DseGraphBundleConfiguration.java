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

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.dropwizard.util.Duration;
import io.dropwizard.validation.MinDuration;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import org.hibernate.validator.constraints.NotEmpty;

import javax.validation.constraints.Min;
import java.util.concurrent.TimeUnit;

/**
 * @author Dan Sorak, Ted Wilmes
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class DseGraphBundleConfiguration {

    private static final int DEFAULT_PORT = 8182;

    @JsonProperty
    @Getter
    @Setter
    @NonNull
    @NotEmpty
    private String[] contactPoints;

    @JsonProperty
    @Getter
    @Setter
    @Min(1)
    private int port = DEFAULT_PORT;

    /**
     * This can either be a traversal or a CQL command. The healthcheck code determines
     * which it is by checking for the prefix "g.". If it starts with that, it is assumed
     * to be a traversal and "executeGraphAsync()" is called. Otherwise, it is assumed to
     * be CQL, which causes "executeAsync()" to be called instead.
     */
    @JsonProperty
    @Getter
    @Setter
    @NonNull
    @NotEmpty
    private String validationQuery = "g.inject(1).hasNext()"; // "select release_version from system.local"

    @JsonProperty
    @Getter
    @Setter
    @NonNull
    @MinDuration(value = 1, unit = TimeUnit.MILLISECONDS)
    private Duration validationQueryTimeout = Duration.seconds(10);

    @JsonProperty
    @Getter
    @Setter
    @NonNull
    @NotEmpty
    private String graphName;

    @JsonProperty
    @Getter
    @Setter
    @NonNull
    @NotEmpty
    private String cqlKeySpace;

    @JsonProperty
    @Getter
    @Setter
    private String userName;

    @JsonProperty
    @Getter
    @Setter
    private String password;

    @JsonProperty
    @Getter
    @Setter
    private String sslTruststoreFile;

    @JsonProperty
    @Getter
    @Setter
    private String sslTruststorePassword;

    @JsonProperty
    @Getter
    @Setter
    private String sslKeystoreFile;

    @JsonProperty
    @Getter
    @Setter
    private String sslKeystorePassword;

    @JsonProperty
    @Getter
    @Setter
    @NonNull
    private Duration shutdownTimeout = Duration.seconds(60);
}
