/*
 *
 *  * Copyright 2005-2015 Red Hat, Inc.
 *  * Red Hat licenses this file to you under the Apache License, version
 *  * 2.0 (the "License"); you may not use this file except in compliance
 *  * with the License.  You may obtain a copy of the License at
 *  *    http://www.apache.org/licenses/LICENSE-2.0
 *  * Unless required by applicable law or agreed to in writing, software
 *  * distributed under the License is distributed on an "AS IS" BASIS,
 *  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 *  * implied.  See the License for the specific language governing
 *  * permissions and limitations under the License.
 *
 */

/**
 * Expecting a mapping of <protocol>:<namespace>:<brokerGroup>:<brokerName>
 */
package io.fabric8.mq.util;

public class ProtocolMapping {
    private static final int ELEMENTS = 4;
    private String protocol;
    private String namespace;
    private String brokerGroup;
    private String brokerName;

    public ProtocolMapping(String mapping) throws Exception {
        String string = mapping.trim();
        if (string.isEmpty() || !string.contains(":")) {
            throw new IllegalArgumentException("Not a valid parameter " + string);
        }

        String[] strs = string.split(":", ELEMENTS);
        if (strs.length != ELEMENTS) {
            throw new IllegalArgumentException("Not a valid parameter " + string);
        }
        protocol = strs[0];
        namespace = strs[1];
        brokerGroup = strs[2];
        brokerName = strs[3];
    }

    public ProtocolMapping() {
    }

    public String getBrokerName() {
        return brokerName;
    }

    public void setBrokerName(String brokerName) {
        this.brokerName = brokerName;
    }

    public String getBrokerGroup() {
        return brokerGroup;
    }

    public void setBrokerGroup(String brokerGroup) {
        this.brokerGroup = brokerGroup;
    }

    public String getNamespace() {
        return namespace;
    }

    public void setNamespace(String namespace) {
        this.namespace = namespace;
    }

    public String getProtocol() {
        return protocol;
    }

    public void setProtocol(String protocol) {
        this.protocol = protocol;
    }

    public String toString() {
        return "ProtocolMapping[" + protocol + ":" + namespace + ":" + brokerGroup + ":" + brokerName;
    }

}
