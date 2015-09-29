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

package io.fabric8.mq.util;

import org.jolokia.client.J4pClient;
import org.jolokia.client.request.J4pReadRequest;
import org.jolokia.client.request.J4pResponse;
import org.json.simple.JSONObject;

import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class BrokerJmxUtils {

    /**
     * @param names - should be of the form name=foo,type=blah etc
     * @return ObjectName
     */
    public static ObjectName getObjectName(String domain, String... names) throws MalformedObjectNameException {
        String string = "";
        for (int i = 0; i < names.length; i++) {
            string += names[i];
            if ((i + 1) < names.length) {
                string += ",";
            }
        }
        String name = getOrderedProperties(getProperties(string));
        return new ObjectName(domain + ":" + name);
    }

    public static Hashtable<String, String> getProperties(String string) {
        Hashtable<String, String> result = new Hashtable<>();
        String[] props = string.split(",");
        for (String prop : props) {
            String[] keyValues = prop.split("=");
            result.put(keyValues[0].trim(), ObjectName.quote(keyValues[1].trim()));
        }
        return result;
    }

    public static String getOrderedProperties(Hashtable<String, String> properties) {
        TreeMap<String, String> map = new TreeMap<>(properties);
        String result = "";
        String separator = "";
        for (Map.Entry<String, String> entry : map.entrySet()) {
            result += separator;
            result += entry.getKey() + "=" + entry.getValue();
            separator = ",";
        }
        return result;
    }

    public static ObjectName getRoot(J4pClient client) throws Exception {
        String type = "org.apache.activemq:*,type=Broker";
        String attribute = "BrokerName";
        ObjectName objectName = new ObjectName(type);
        J4pResponse<J4pReadRequest> response = client.execute(new J4pReadRequest(objectName, attribute));

        JSONObject jsonObject = response.getValue();
        JSONObject nameObject = (JSONObject) jsonObject.values().iterator().next();
        String name = nameObject.values().iterator().next().toString();
        return new ObjectName("org.apache.activemq:type=Broker,brokerName=" + name);
    }

    public static List<ObjectName> getDestinations(J4pClient client, ObjectName root, String type) throws Exception {
        List<ObjectName> list = new ArrayList<>();
        String objectNameStr = root.toString() + ",destinationType=" + type + ",destinationName=*";
        J4pResponse<J4pReadRequest> response = client.execute(new J4pReadRequest(new ObjectName(objectNameStr), "Name"));
        JSONObject value = response.getValue();
        for (Object key : value.keySet()) {
            ObjectName objectName = new ObjectName(key.toString());
            list.add(objectName);
        }

        return list;
    }

    public static Object getAttribute(J4pClient client, ObjectName objectName, String attribute) throws Exception {
        J4pResponse<J4pReadRequest> result = client.execute(new J4pReadRequest(objectName, attribute));
        return result.getValue();
    }
}
