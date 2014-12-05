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
package io.fabric8.api.registry.rules;

import io.fabric8.swagger.model.Api;
import io.fabric8.swagger.model.ApiDeclaration;
import io.fabric8.swagger.model.Operation;
import io.fabric8.swagger.model.Parameter;
import io.fabric8.swagger.model.ResponseMessage;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static io.fabric8.utils.URLUtils.urlPathJoin;

/**
 */
public class SwaggerHelpers {
    public static List<ResponseMessage> getOrCreateResponseMessages(Operation operation) {
        List<ResponseMessage> responseMessages = operation.getResponseMessages();
        if (responseMessages == null) {
            responseMessages = new ArrayList<>();
            operation.setResponseMessages(responseMessages);
        }
        return responseMessages;
    }

    public static List<Parameter> getOrCreateParameters(Operation operation) {
        List<Parameter> parameters = operation.getParameters();
        if (parameters == null) {
            parameters = new ArrayList<>();
            operation.setParameters(parameters);
        }
        return parameters;
    }

    public static List<Operation> getOrCreateOperations(Api api) {
        List<Operation> operations = api.getOperations();
        if (operations == null) {
            operations = new ArrayList<>();
            api.setOperations(operations);
        }
        return operations;
    }

    public static List<Api> getOrCreateApis(ApiDeclaration apiDeclaration) {
        List<Api> apis = apiDeclaration.getApis();
        if (apis == null) {
            apis = new ArrayList<>();
            apiDeclaration.setApis(apis);
        }
        return apis;
    }

    /**
     * Returns the {@link io.fabric8.swagger.model.Api} for the given path or creates a new one and appends it to the list
     */
    public static Api findOrCreateApiForPath(List<Api> apis, String path) {
        for (Api api : apis) {
            String aPath = api.getPath();
            if (Objects.equals(path, aPath)) {
                return api;
            }
        }
        Api api = new Api();
        api.setPath(path);
        apis.add(api);
        return api;
    }

    /**
     * Returns all the APIs which begin with the given path
     */
    public static List<Api> filterApis(ApiDeclaration apiDeclaration, String path) {
        String resourcePath = urlPathJoin("/", apiDeclaration.getResourcePath());
        List<Api> answer = new ArrayList<>();
        for (Api api : apiDeclaration.getApis()) {
            String aPath = urlPathJoin(resourcePath, api.getPath());
            if (aPath != null && aPath.startsWith(path)) {
                answer.add(api);
            }
        }
        return answer;
    }
}
