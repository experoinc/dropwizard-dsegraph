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
 */
public class DseGraphConfiguration {
    @Getter
    @Setter
    private String use;

    @Getter
    @Setter
    private DseGraphFactory dseGraph;

    @Getter
    @Setter
    private DseGraphFactory[] graphs;

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
