/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.fabric8.hubot.notifier;

import io.fabric8.kubernetes.client.Watcher;
import io.fabric8.utils.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The flags as to whether we should send a notification message based on the different kinds of event
 */
public class NotifyConfig {
    public static final String ENV_VAR_PREFIX = "HUBOT_NOTIFY_KUBERNETES_";

    private static final transient Logger LOG = LoggerFactory.getLogger(NotifyConfig.class);

    private final String name;
    private final String envVarPrefix;

    private boolean notifyAdd = true;
    private boolean notifyModify = false;
    private boolean notifyDelete = true;
    private boolean notifyError = true;

    public NotifyConfig(String name) {
        this.name = name;
        this.envVarPrefix = ENV_VAR_PREFIX + name.toUpperCase() + "_";

        // lets default the values form environment variables
        this.notifyAdd = overrideFromEnvVar("ADD", notifyAdd);
        this.notifyModify = overrideFromEnvVar("MODIFY", notifyModify);
        this.notifyDelete = overrideFromEnvVar("DELETE", notifyDelete);
        this.notifyError = overrideFromEnvVar("ERROR", notifyError);
    }

    public String getName() {
        return name;
    }

    public boolean isEnabled(Watcher.Action action) {
        switch (action) {
            case ADDED:
                return isNotifyAdd();
            case MODIFIED:
                return isNotifyModify();
            case DELETED:
                return isNotifyDelete();
            case ERROR:
                return isNotifyError();
        }
        return false;
    }

    public boolean isNotifyAdd() {
        return notifyAdd;
    }

    public void setNotifyAdd(boolean notifyAdd) {
        this.notifyAdd = notifyAdd;
    }

    public boolean isNotifyDelete() {
        return notifyDelete;
    }

    public void setNotifyDelete(boolean notifyDelete) {
        this.notifyDelete = notifyDelete;
    }

    public boolean isNotifyModify() {
        return notifyModify;
    }

    public void setNotifyModify(boolean notifyModify) {
        this.notifyModify = notifyModify;
    }

    public boolean isNotifyError() {
        return notifyError;
    }

    public void setNotifyError(boolean notifyError) {
        this.notifyError = notifyError;
    }


    protected boolean overrideFromEnvVar(String name, boolean defaultValue) {
        String envVar = envVarPrefix + name;
        String value = null;
        try {
            value = System.getenv(envVar);
            LOG.debug("Checking env var: " + envVar);
            if (Strings.isNullOrBlank(value)) {
                // lets try the generic value for all entities
                value = System.getProperty(ENV_VAR_PREFIX + name);
            }
            if (Strings.isNotBlank(value)) {
                Boolean b = Boolean.parseBoolean(value);
                if (b != null) {
                    return b.booleanValue();
                }
            }
        } catch (Exception e) {
            LOG.warn("Failed to parse boolean value for env var " + envVar + " with value `" + value + "`. " + e, e);
        }
        return defaultValue;
    }


}
