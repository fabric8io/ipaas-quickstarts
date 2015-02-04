/**
 *  Copyright 2005-2014 Red Hat, Inc.
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
package io.fabric8.app.library;

import io.fabric8.app.library.support.GitServlet;
import io.fabric8.app.library.support.UploadServlet;
import io.fabric8.rest.utils.Servers;
import io.fabric8.utils.Function;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletContextHandler;

public class Main {

    public static void main(final String[] args) throws Exception {
        startServer().join();
    }

    public static Server startServer() throws Exception {
        return Servers.startServer("App Library", new Function<ServletContextHandler, Void>() {
            @Override
            public Void apply(ServletContextHandler context) {
                context.addServlet(UploadServlet.class, "/upload/*");
                context.addServlet(GitServlet.class, "/git/*");
                return null;
            }
        });
    }

}
