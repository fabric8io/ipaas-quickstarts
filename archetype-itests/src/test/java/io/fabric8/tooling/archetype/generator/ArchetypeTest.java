/**
 * Copyright 2005-2015 Red Hat, Inc.
 * <p>
 * Red Hat licenses this file to you under the Apache License, version
 * 2.0 (the "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 * implied.  See the License for the specific language governing
 * permissions and limitations under the License.
 */
package io.fabric8.tooling.archetype.generator;

import io.fabric8.kubernetes.api.KubernetesHelper;
import io.fabric8.kubernetes.client.DefaultKubernetesClient;
import io.fabric8.utils.DomHelper;
import io.fabric8.utils.Files;
import io.fabric8.utils.IOHelpers;
import io.fabric8.utils.Strings;
import io.fabric8.utils.XmlUtils;
import org.apache.maven.cli.MavenCli;
import org.junit.AfterClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.junit.Assert.assertNotNull;

public class ArchetypeTest {
    private static final transient Logger LOG = LoggerFactory.getLogger(ArchetypeTest.class);
    public static final String TEST_ARCHETYPE_SYSTEM_PROPERTY = "test.archetype";
    public static final String ARQUILLIAN_SYSTEM_PROPERTY = "test.arq";

    private static boolean failed;

    private static File basedir = new File(System.getProperty("basedir", "."));
    private static String arqTesting = System.getProperty(ARQUILLIAN_SYSTEM_PROPERTY, "");
    private static File projectsOutputFolder = new File(basedir, "target/createdProjects");

    // for an up to date list of failing system tests see
    // https://github.com/fabric8io/ipaas-quickstarts/issues?q=is%3Aissue+is%3Aopen+label%3A%22system+test%22

    // the following lists the sets of archetype prefixes which are not bet capable of being system tested yet
    private static List<String> ignoreArchetypePrefixes = Arrays.asList(
            // TODO https://github.com/fabric8io/ipaas-quickstarts/issues/1366
            "karaf-",

            // TODO https://github.com/fabric8io/ipaas-quickstarts/issues/1367
            "war-",

            // TODO https://github.com/fabric8io/ipaas-quickstarts/issues/1368
            "wildfly-camel-"
    );


    // the following lists the archetypes which currently fail the system tests
    private static Set<String> ignoreArchetypes = new HashSet<>(Arrays.asList(
            // TODO requires infinispan-server to be deployed
            // https://github.com/fabric8io/ipaas-quickstarts/issues/1362
            "infinispan-client-archetype",

            // TODO https://github.com/fabric8io/ipaas-quickstarts/issues/1363
            "spring-boot-hystrix-archetype",
            // TODO https://github.com/fabric8io/ipaas-quickstarts/issues/1364
            "spring-boot-ribbon-archetype"
    ));

    private boolean verbose = true;

    private String packageName = "org.acme.mystuff";

    // lets get the versions from the pom.xml via a system property
    private String groupId = "io.fabric8.archetypes";
    private String fabric8Version = System.getProperty("fabric8.version", "2.2-SNAPSHOT");
    private String projectVersion = System.getProperty("project.version", "2.2-SNAPSHOT");
    private String failsafeVersion = System.getProperty("failsafe.version", "2.19");

    private static List<String> outDirs = new ArrayList<String>();

    @Test
    public void testGenerateQuickstartArchetypes() {
        try {
            Files.recursiveDelete(projectsOutputFolder);

            List<String> archetypes = getArchetypesFromJar();
            assertThat(archetypes).describedAs("Archetypes to create").isNotEmpty();

            String testArchetype = System.getProperty(TEST_ARCHETYPE_SYSTEM_PROPERTY, "");
            if (Strings.isNotBlank(testArchetype)) {
                LOG.info("Due to system property: '" + TEST_ARCHETYPE_SYSTEM_PROPERTY + "' we will just run the test on archetype: " + testArchetype);
                assertArchetypeCreated(testArchetype);
            } else {
                LOG.info("Generating archetypes: " + archetypes);
                for (String archetype : archetypes) {
                    if (ignoreArchetype(archetype)) {
                        LOG.warn("Ignoring archetype: " + archetype);
                    } else {
                        assertArchetypeCreated(archetype);
                    }
                }
            }

            removeSnapshotFabric8Artifacts();
        } catch (Exception e) {
            fail("Failed to create archetypes: " + e, e);
            failed = true;
        }
    }

    protected boolean ignoreArchetype(String archetype) {
        for (String prefix : ignoreArchetypePrefixes) {
            if (archetype.startsWith(prefix)) {
                return true;
            }
        }
        return ignoreArchetypes.contains(archetype);
    }

    protected List<String> getArchetypesFromJar() throws IOException {
        String entryName = "archetype-catalog.xml";
        URL url = getClass().getClassLoader().getResource(entryName);
        assertThat(url).describedAs("Could not find resource " + entryName + " on the classpath!").isNotNull();

        SortedSet<String> artifactIds = new TreeSet<>();
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            InputSource is = new InputSource(url.openStream());
            Document doc = builder.parse(is);
            NodeList artifactTags = doc.getElementsByTagName("artifactId");
            for (int i = 0, size = artifactTags.getLength(); i < size; i++) {
                Node element = artifactTags.item(i);
                String artifactId = element.getTextContent();
                if (Strings.isNotBlank(artifactId)) {
                    artifactIds.add(artifactId);
                }
            }
        } catch (Exception e) {
            fail("Failed to parse " + entryName + " in catalog " + url + ". Exception " + e, e);
        }
        return new ArrayList<>(artifactIds);
    }

    protected void removeSnapshotFabric8Artifacts() {
/*
        File fabric8F/older = new File(localMavenRepo, "io/fabric8");
        if (Files.isDirectory(fabric8Folder)) {
            File[] artifactFolders = fabric8Folder.listFiles();
            if (artifactFolders != null) {
                for (File artifactFolder : artifactFolders) {
                    File[] versionFolders = artifactFolder.listFiles();
                    if (versionFolders != null) {
                        for (File versionFolder : versionFolders) {
                            if (versionFolder.getName().toUpperCase().endsWith("-SNAPSHOT")) {
                                LOG.info("Removing snapshot version from local maven repo: " + versionFolder);
                                Files.recursiveDelete(versionFolder);
                            }
                        }
                    }
                }
            }
        }
*/
    }


    protected void assertArchetypeCreated(String artifactId) throws Exception {
        String homeDir = System.getProperty("user.home", "~");
        File mvnRepoDir = new File(homeDir, ".m2/repository");

        File archetypeJar = new File(mvnRepoDir, groupId.replace('.', '/') + "/" + artifactId + "/" + projectVersion + "/"
                + artifactId + "-" + projectVersion + ".jar");
        assertThat(archetypeJar).describedAs("Could not find archetype jar for artifact id: " + artifactId).isFile();

        assertArchetypeCreated(artifactId, "io.fabric8.archetypes.itests", projectVersion, archetypeJar);
    }

    private void assertArchetypeCreated(String artifactId, String groupId, String version, File archetypejar) throws Exception {
        artifactId = Strings.stripSuffix(artifactId, "-archetype");
        artifactId = Strings.stripSuffix(artifactId, "-example");
        File outDir = new File(projectsOutputFolder, artifactId);

        LOG.info("Creating Archetype " + groupId + ":" + artifactId + ":" + version);
        Map<String, String> properties = new ArchetypeHelper(archetypejar, outDir, groupId, artifactId, version, null, null).parseProperties();
        LOG.info("Has preferred properties: " + properties);

        ArchetypeHelper helper = new ArchetypeHelper(archetypejar, outDir, groupId, artifactId, version, null, null);
        helper.setPackageName(packageName);

        // lets override some properties
        HashMap<String, String> overrideProperties = new HashMap<String, String>();
        // for camel-archetype-component
        overrideProperties.put("scheme", "mycomponent");
        helper.setOverrideProperties(overrideProperties);

        // this is where the magic happens
        helper.execute();

        LOG.info("Generated archetype " + artifactId);

        // expected pom file
        File pom = new File(outDir, "pom.xml");

        // this archetype might not be a maven project
        if (!pom.isFile()) {
            return;
        }

        String pomText = Files.toString(pom);
        String badText = "${camel-";
        if (pomText.contains(badText)) {
            if (verbose) {
                LOG.info(pomText);
            }
            fail("" + pom + " contains " + badText);
        }

        // now lets ensure we have the necessary test dependencies...
        boolean updated = false;
        Document doc = XmlUtils.parseDoc(pom);
        boolean funktion = isFunktionProject(doc);
        LOG.debug("Funktion project: " + funktion);
        if (!funktion) {
            if (ensureMavenDependency(doc, "io.fabric8", "fabric8-arquillian", "test")) {
                updated = true;
            }
            if (ensureMavenDependency(doc, "org.jboss.arquillian.junit", "arquillian-junit-container", "test")) {
                updated = true;
            }
            if (ensureMavenDependency(doc, "org.jboss.shrinkwrap.resolver", "shrinkwrap-resolver-impl-maven", "test")) {
                updated = true;
            }
            if (ensureMavenDependencyBOM(doc, "io.fabric8", "fabric8-project-bom-with-platform-deps", fabric8Version)) {
                updated = true;
            }
        }
        if (ensureFailsafePlugin(doc)) {
            updated = true;
        }
        if (updated) {
            DomHelper.save(doc, pom);
        }


        // lets generate the system test
        if (!hasGoodSystemTest(new File(outDir, "src/test/java"))) {
            File systemTest = new File(outDir, "src/test/java/io/fabric8/systests/KubernetesIntegrationKT.java");
            systemTest.getParentFile().mkdirs();
            String javaFileName = "KubernetesIntegrationKT.java";
            URL javaUrl = getClass().getClassLoader().getResource(javaFileName);
            assertNotNull("Could not load resource on the classpath: " + javaFileName, javaUrl);
            IOHelpers.copy(javaUrl.openStream(), new FileOutputStream(systemTest));
        }

        outDirs.add(outDir.getPath());
    }

    /**
     * Returns true if we can find a system test Java file which uses the nice <code>isPodReadyForPeriod()</code>
     * assertions
     */
    protected boolean hasGoodSystemTest(File file) {
        if (file.isFile()) {
            String name = file.getName();
            if (name.endsWith("KT.java")) {
                try {
                    String text = IOHelpers.readFully(file);
                    if (text.contains("isPodReadyForPeriod(")) {
                        LOG.info("Found good system test at " + file.getAbsolutePath() + " so not generating a new one for this archetype");
                        return true;
                    }
                } catch (IOException e) {
                    LOG.warn("Failed to load file " + file + ". " + e, e);
                }
            }
        }
        File[] files = file.listFiles();
        if (files != null) {
            for (File child : files) {
                if (hasGoodSystemTest(child)) {
                    return true;
                }
            }
        }
        return false;
    }

    protected static boolean isFunktionProject(Document doc) {
        Element parent = DomHelper.firstChild(doc.getDocumentElement(), "parent");
        if (parent != null) {
            Element groupId = DomHelper.firstChild(parent, "groupId");
            if (groupId != null) {
                String text = groupId.getTextContent();
                return Objects.equals("io.fabric8.funktion.starter", text);
            }
        }
        return false;
    }
    
    protected static boolean ensureMavenDependency(Document doc, String groupId, String artifactId, String scope) {
        Element dependences = DomHelper.firstChild(doc.getDocumentElement(), "dependencies");
        if (dependences == null) {
            dependences = DomHelper.addChildElement(doc.getDocumentElement(), "dependencies");
        }
        NodeList childNodes = dependences.getChildNodes();
        for (int i = 0, size = childNodes.getLength(); i < size; i++) {
            Node item = childNodes.item(i);
            if (item instanceof Element) {
                Element child = (Element) item;
                if (firstChildTextContent(child, "groupId", groupId) &&
                        firstChildTextContent(child, "artifactId", artifactId)) {
                    return false;

                }

            }
        }
        dependences.appendChild(doc.createTextNode("\n    "));
        Element dependency = DomHelper.addChildElement(dependences, "dependency");
        dependency.appendChild(doc.createTextNode("\n      "));
        DomHelper.addChildElement(dependency, "groupId", groupId);
        dependency.appendChild(doc.createTextNode("\n      "));
        DomHelper.addChildElement(dependency, "artifactId", artifactId);
        dependency.appendChild(doc.createTextNode("\n      "));
        DomHelper.addChildElement(dependency, "scope", scope);
        dependency.appendChild(doc.createTextNode("\n    "));
        dependences.appendChild(doc.createTextNode("\n    "));
        return true;
    }

    protected static boolean ensureMavenDependencyBOM(Document doc, String groupId, String artifactId, String version) {
        Element dependencyManagement = DomHelper.firstChild(doc.getDocumentElement(), "dependencyManagement");
        if (dependencyManagement == null) {
            dependencyManagement = DomHelper.addChildElement(doc.getDocumentElement(), "dependencyManagement");
        }
        Element dependences = DomHelper.firstChild(dependencyManagement, "dependencies");
        if (dependences == null) {
            dependences = DomHelper.addChildElement(dependencyManagement, "dependencies");
        }

        NodeList childNodes = dependences.getChildNodes();
        for (int i = 0, size = childNodes.getLength(); i < size; i++) {
            Node item = childNodes.item(i);
            if (item instanceof Element) {
                Element child = (Element) item;
                if (firstChildTextContent(child, "groupId", groupId) &&
                        firstChildTextContent(child, "artifactId", artifactId)) {
                    return false;

                }

            }
        }
        dependences.appendChild(doc.createTextNode("\n      "));
        Element dependency = DomHelper.addChildElement(dependences, "dependency");
        dependency.appendChild(doc.createTextNode("\n        "));
        DomHelper.addChildElement(dependency, "groupId", groupId);
        dependency.appendChild(doc.createTextNode("\n        "));
        DomHelper.addChildElement(dependency, "artifactId", artifactId);
        dependency.appendChild(doc.createTextNode("\n        "));
        DomHelper.addChildElement(dependency, "version", version);
        dependency.appendChild(doc.createTextNode("\n        "));
        DomHelper.addChildElement(dependency, "type", "pom");
        dependency.appendChild(doc.createTextNode("\n        "));
        DomHelper.addChildElement(dependency, "scope", "import");
        dependency.appendChild(doc.createTextNode("\n      "));
        dependences.appendChild(doc.createTextNode("\n      "));
        return true;
    }

    protected boolean ensureFailsafePlugin(Document doc) {
        String artifactId = "maven-failsafe-plugin";
        Element build = DomHelper.firstChild(doc.getDocumentElement(), "build");
        if (build == null) {
            build = DomHelper.addChildElement(doc.getDocumentElement(), "build");
        }
        Element plugins = DomHelper.firstChild(build, "plugins");
        if (plugins == null) {
            plugins = DomHelper.addChildElement(build, "plugins");
        }

        NodeList childNodes = plugins.getChildNodes();
        for (int i = 0, size = childNodes.getLength(); i < size; i++) {
            Node item = childNodes.item(i);
            if (item instanceof Element) {
                Element child = (Element) item;
                if (firstChildTextContent(child, "artifactId", artifactId)) {
                    return false;

                }

            }
        }
        plugins.appendChild(doc.createTextNode("\n      "));
        Element plugin = DomHelper.addChildElement(plugins, "plugin");
        plugin.appendChild(doc.createTextNode("\n        "));
        DomHelper.addChildElement(plugin, "artifactId", artifactId);
        plugin.appendChild(doc.createTextNode("\n        "));
        DomHelper.addChildElement(plugin, "version", failsafeVersion);
        plugin.appendChild(doc.createTextNode("\n        "));
        Element configuration = DomHelper.addChildElement(plugin, "configuration");
        configuration.appendChild(doc.createTextNode("\n        "));
        Element includes = DomHelper.addChildElement(configuration, "includes");
        includes.appendChild(doc.createTextNode("\n          "));
        Element include = DomHelper.addChildElement(includes, "include", "**/*KT.*");
        includes.appendChild(doc.createTextNode("\n          "));
        configuration.appendChild(doc.createTextNode("\n        "));
        plugin.appendChild(doc.createTextNode("\n      "));
        plugins.appendChild(doc.createTextNode("\n      "));
        return true;
    }


    private static boolean firstChildTextContent(Element element, String name, String textContent) {
        Element child = DomHelper.firstChild(element, name);
        if (child != null) {
            String actual = child.getTextContent();
            if (textContent.equals(actual)) {
                return true;
            }
        }
        return false;
    }

    @AfterClass
    public static void afterAll() throws Exception {
        if (failed) {
            return;
        }
        // now let invoke the projects
        final int[] resultPointer = new int[1];
        List<String> failedProjects = new ArrayList<>();
        List<String> successfulProjects = new ArrayList<>();

        for (final String outDir : outDirs) {
            // thread locals are evil (I'm talking to you - org.codehaus.plexus.DefaultPlexusContainer#lookupRealm!)
            Thread t = new Thread(new Runnable() {
                @Override
                public void run() {
                    LOG.info("Invoking projects in " + outDir);
                    System.setProperty("maven.multiModuleProjectDirectory", "$M2_HOME");
                    MavenCli maven = new MavenCli();
                    // Dmaven.multiModuleProjectDirectory
                    String[] args = {"clean", "package", "-Dfabric8.service.name=dummy-service"};
                    boolean useArq = Objects.equals(arqTesting, "true");
                    if (useArq) {
                        args = new String[]{"clean", "install", "-Dfabric8.service.name=dummy-service"};
                        if (KubernetesHelper.isOpenShift(new DefaultKubernetesClient())) {
                            // lets add a workaround for a lack of discovery OOTB with fabric8-maven-plugin
                            args = new String[]{"clean", "install", "-Dfabric8.service.name=dummy-service", "-Dfabric8.mode=openshift"};
                        }
                    }
                    // using an itest settings.xml here similar to jboss-fuse archetypes configuration/settings.xml
                    args = Arrays.copyOf(args, args.length + 2);
                    args[args.length - 2] = "-s";
                    args[args.length - 1] = new File(basedir, "target/test-classes/settings.xml").getAbsolutePath();
                    resultPointer[0] = maven.doMain(args, outDir, System.out, System.out);
                    LOG.info("result: " + resultPointer[0]);

                    if (useArq && resultPointer[0] == 0) {
                        maven = new MavenCli();
                        args = new String[]{"failsafe:integration-test", "failsafe:verify", "-fae"};
                        LOG.info("Now trying to run the integration tests via: mvn " + Strings.join(" ", args));
                        resultPointer[0] = maven.doMain(args, outDir, System.out, System.out);
                        LOG.info("result: " + resultPointer[0]);
                    }
                }
            });
            t.start();
            t.join();
            String projectName = new File(outDir).getName();
            if (resultPointer[0] != 0) {
                failedProjects.add(projectName);
                LOG.error("Failed project: " + projectName);
            } else {
                successfulProjects.add(projectName);
                LOG.info("Successful project: " + projectName);
            }
        }

        for (String failedProject : failedProjects) {
            LOG.info("Successful project: " + failedProject);
        }
        for (String failedProject : failedProjects) {
            LOG.error("Failed project: " + failedProject);
        }
        assertThat(failedProjects).describedAs("Projects failed: " + failedProjects).isEmpty();
    }
}
