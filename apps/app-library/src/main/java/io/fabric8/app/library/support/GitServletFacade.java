/**
 * Copyright (C) 2013 the original author or authors.
 * See the notice.md file distributed with this work for additional
 * information regarding copyright ownership.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.fabric8.app.library.support;

import io.hawt.git.GitFacade;
import io.hawt.git.WriteCallback;
import io.hawt.git.WriteContext;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.http.server.GitServlet;
import org.osgi.framework.BundleContext;
import org.osgi.util.tracker.ServiceTracker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.util.Enumeration;
import java.util.Hashtable;

/**
 * Delegates to the underlying jgit servlet after we properly lock access to the git repository
 */
public class GitServletFacade extends GitServlet {
    private static final transient Logger LOG = LoggerFactory.getLogger(GitServletFacade.class);

    private GitFacade gitFacade;

    @Override
    public void init(final ServletConfig config) throws ServletException {
        gitFacade = GitFacade.getSingleton();
        validateGitFacade();

        String basePath = gitFacade.getRootGitDirectory().getAbsoluteFile().getParentFile().getAbsolutePath();
        System.out.println("Exposing git base path at: " + basePath);

        final Hashtable<String,String> initParams = new Hashtable<>();
        initParams.put("base-path", basePath);
        initParams.put("repository-root", basePath);
        initParams.put("export-all", "true");

        ServletConfig gitConfig = new ServletConfig() {
            @Override
            public String getServletName() {
                return config.getServletName();
            }

            @Override
            public ServletContext getServletContext() {
                return config.getServletContext();
            }

            @Override
            public String getInitParameter(String paramName) {
                return initParams.get(paramName);
            }

            @Override
            public Enumeration<String> getInitParameterNames() {
                return initParams.keys();
            }
        };
        super.init(gitConfig);
    }

    @Override
    public void destroy() {
        super.destroy();
    }

    @Override
    protected void service(final HttpServletRequest req, final HttpServletResponse resp) throws ServletException, IOException {
        validateGitFacade();
        String branch = "master";
        String path = null;
        try {
            gitFacade.writeFile(branch, path, new WriteCallback<Object>() {
                @Override
                public Object apply(WriteContext writeContext) throws IOException, GitAPIException {
                    try {
                        System.out.println("Invoking request on " + req.getServletPath() + " to git servlet");
                        GitServletFacade.super.service(req, resp);
                    } catch (ServletException e) {
                        throw new IOException(e);
                    }
                    return null;
                }
            });
        } catch (GitAPIException e) {
            throw new IOException(e);
        }
    }

    protected void validateGitFacade() throws ServletException {
        if (gitFacade == null) {
            throw new ServletException("No GitFacade object available!");
        }
    }
}
