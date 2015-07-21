/**
 *  Copyright 2005-2015 Red Hat, Inc.
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
package io.fabric8.image.linker;


import org.eclipse.jetty.server.Server;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

public final class ImageLinkerTest {

    private static final Logger LOG = LoggerFactory.getLogger(ImageLinkerTest.class);
    private URL url;
    private InputStream in;

    private static Server server = null;
    
    @BeforeClass
    public static void before() {
    	try {
    		server = Main.startServer();
    	} catch (Exception e) {
    		LOG.error(e.getMessage(),e);
    		Assert.fail("Jetty Server could not be started");
    	}
    }
    
    @AfterClass
    public static void after() throws Exception {
    	if (server!=null) server.stop();
    }
    

    @Test
    public void dummyTest() throws IOException {

    }

    /*
     * Just a simple helper method to read bytes from an InputStream and return the String representation.
     */
    private static String getStringFromInputStream(InputStream in) throws Exception {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        int c = 0;
        while ((c = in.read()) != -1) {
            bos.write(c);
        }
        in.close();
        bos.close();
        return bos.toString();
    }


}
