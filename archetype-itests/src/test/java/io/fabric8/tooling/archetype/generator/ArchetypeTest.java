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
package io.fabric8.tooling.archetype.generator;

import io.fabric8.utils.Files;

import org.apache.maven.cli.MavenCli;
import org.junit.AfterClass;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class ArchetypeTest {

    private boolean verbose = true;

    private String packageName = "org.acme.mystuff";

    // lets get the versions from the pom.xml via a system property
    private String projectVersion = System.getProperty("project.version", "2.3-SNAPSHOT");
    private File basedir = new File(System.getProperty("basedir", "."));

    private static List<String> outDirs = new ArrayList<String>();

    @Test
    public void testGenerateQuickstartArchetypes() throws Exception {
        File archetypesDir = new File(basedir, "target/archetypes");
        String[] jars = archetypesDir.list(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                return name.endsWith(".jar") && new File(dir, name).isFile();
            }
        });
        String versionPart = "-" + projectVersion;
        for (String jar : jars) {
            int idx = jar.indexOf(versionPart);
            if (idx <= 0) {
                System.out.println("WARNING: ignoring invalid jar " + jar);
                continue;
            }
            String artifactId = jar.substring(0, idx);
            String projectId = "io.fabric8.archetypes";
            String version = projectVersion;

            File jarFile = new File(archetypesDir, jar);
            assertArchetypeCreated(artifactId, projectId, version, jarFile);
        }
    }

    private void assertArchetypeCreated(String artifactId, String groupId, String version, File archetypejar) throws Exception {
        File outDir = new File(basedir, "target/" + artifactId + "-output");

        System.out.println("Creating Archetype " + groupId + ":" + artifactId + ":" + version);
        Map<String, String> properties = new ArchetypeHelper(archetypejar, outDir, groupId, artifactId, version, null, null).parseProperties();
        System.out.println("Has preferred properties: " + properties);

        ArchetypeHelper helper = new ArchetypeHelper(archetypejar, outDir, groupId, artifactId, version, null, null);
        helper.setPackageName(packageName);

        // lets override some properties
        HashMap<String, String> overrideProperties = new HashMap<String, String>();
        // for camel-archetype-component
        overrideProperties.put("scheme", "mycomponent");
        helper.setOverrideProperties(overrideProperties);

        // this is where the magic happens
        helper.execute();

        // expected pom file
        File pom = new File(outDir, "pom.xml");
        assertFileExists(pom);

        String pomText = Files.toString(pom);
        String badText = "${camel-";
        if (pomText.contains(badText)) {
            if (verbose) {
                System.out.println(pomText);
            }
            fail("" + pom + " contains " + badText);
        }

        outDirs.add(outDir.getPath());
    }

    @AfterClass
    public static void afterAll() throws Exception {
        // now let invoke the projects
        final int[] resultPointer = new int[1];
        StringWriter sw = new StringWriter();
        Set<String> modules = new HashSet<String>();
        for (final String outDir : outDirs) {
            String module = new File(outDir).getName();
            if (modules.add(module)) {
                sw.append(String.format("        <module>%s</module>\n", module));
            }
        }
        sw.close();
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        Files.copy(ArchetypeTest.class.getResourceAsStream("/archetypes-test-pom.xml"), baos);
        String pom = new String(baos.toByteArray()).replace("        <!-- to be replaced -->", sw.toString());
        FileOutputStream modulePom = new FileOutputStream("target/archetypes-test-pom.xml");
        Files.copy(new ByteArrayInputStream(pom.getBytes()), modulePom);
        modulePom.close();

        final String outDir = new File("target").getCanonicalPath();
        // thread locals are evil (I'm talking to you - org.codehaus.plexus.DefaultPlexusContainer#lookupRealm!)
        Thread t = new Thread(new Runnable() {
            @Override
            public void run() {
                System.out.println("Invoking projects in " + outDir);
                System.setProperty("maven.multiModuleProjectDirectory", "$M2_HOME");
                MavenCli maven = new MavenCli();
                // Dmaven.multiModuleProjectDirectory
                resultPointer[0] = maven.doMain(new String[] { "clean", "package", "-f", "archetypes-test-pom.xml", "-Dfabric8.service.name=dummy-service"}, outDir, System.out, System.out);
                System.out.println("result: " + resultPointer[0]);
            }
        });
        t.start();
        t.join();

        assertEquals("Build of project " + outDir + " failed. Result = " + resultPointer[0], 0, resultPointer[0]);
    }

    protected void assertFileExists(File file) {
        assertTrue("file should exist: " + file, file.exists());
    }

}
