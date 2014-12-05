/**
 *
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.fabric8.api.registry;

/**
 * Used as a key to associate a container to Swagger APIs and other resources
 */
public class PodAndContainerId {
    private final String podId;
    private final String containerId;

    public PodAndContainerId(String podId, String containerId) {
        this.podId = podId;
        this.containerId = containerId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        PodAndContainerId that = (PodAndContainerId) o;

        if (!containerId.equals(that.containerId)) return false;
        if (!podId.equals(that.podId)) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = podId.hashCode();
        result = 31 * result + containerId.hashCode();
        return result;
    }

    @Override
    public String toString() {
        return "PodAndContainerId{" +
                "podId='" + podId + '\'' +
                ", containerId='" + containerId + '\'' +
                '}';
    }

    public String getPodId() {
        return podId;
    }

    public String getContainerId() {
        return containerId;
    }
}
