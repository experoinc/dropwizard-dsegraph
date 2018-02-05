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

import lombok.Getter;
import lombok.Setter;

/**
 * @author Ted Wilmes
 *
 * Configuration class for DseGraph. Configuration properties may be set directly under "dseGraph:' in config.yml
 * or may be grouped under named configurations which appear under "dseGraph:" "graphs:". If the "use:" parameter
 * is missing or the empty string, the global configuration is used, otherwise one of the "graphs:" is selected
 * based on the value of the "use:" parameter, whose value if present must match the "name:" parameter under one
 * of the list elements under "graphs:"
 */
public class DseGraphConfiguration {
    /**
     * Name of one of the (possibly multiple) configurations of DseGraph
     */
    @Getter
    @Setter
    private String use;

    /**
     * Contains global configurations for DseGraphFactory
     */
    @Getter
    @Setter
    private DseGraphFactory dseGraph;

    /**
     * Contains zero or  more named configurations for DseGraphFactory that can be selected by the "use:" property.
     */
    @Getter
    @Setter
    private DseGraphFactory[] graphs;

    /**
     * Returns a configured DseGraphFactory - either the global configuration or one of the named configurations
     * depending on the value of "use:"
     */
    public DseGraphFactory useDseGraph() {
        if(use == null || use.equals("")) {
            return dseGraph;
        }
        for(DseGraphFactory graph : graphs) {
            if(graph.getName() != null && graph.getName().equals(use)) {
                return graph;
            }
        }
        return null;
    }
}
