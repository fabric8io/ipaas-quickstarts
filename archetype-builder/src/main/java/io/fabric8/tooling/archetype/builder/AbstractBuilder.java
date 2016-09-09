/**
 *  Copyright 2005-2016 Red Hat, Inc.
 *
 *  Red Hat licenses this file to you under the Apache License, version
 *  2.0 (the "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 *  implied.  See the License for the specific language governing
 *  permissions and limitations under the License.
 */
package io.fabric8.tooling.archetype.builder;

import io.fabric8.tooling.archetype.ArchetypeUtils;

/**
 * A base support class for Builders.
 */
public class AbstractBuilder {

    protected ArchetypeUtils archetypeUtils = new ArchetypeUtils();
    protected int indentSize = 2;
    protected String indent = "  ";

    public void setIndentSize(int indentSize) {
        this.indentSize = Math.min(indentSize <= 0 ? 0 : indentSize, 8);
        indent = "";
        for (int c = 0; c < this.indentSize; c++) {
            indent += " ";
        }
    }


}
