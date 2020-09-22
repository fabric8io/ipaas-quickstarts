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
package io.fabric8.tooling.archetype.builder;

import io.fabric8.tooling.archetype.ArchetypeUtils;
import io.fabric8.utils.*;
import io.fabric8.utils.Objects;
import org.eclipse.jgit.api.CloneCommand;
import org.eclipse.jgit.api.Git;
import org.kohsuke.github.GHOrganization;
import org.kohsuke.github.GHRepository;
import org.kohsuke.github.GitHub;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.*;
import org.xml.sax.InputSource;

import java.io.*;
import java.util.*;
import java.util.Arrays;
import java.util.regex.Pattern;

/**
 * This class is a replacement for <code>mvn archetype:create-from-project</code> without dependencies to
 * maven-archetype related libraries.
 */
public class ArchetypeBuilder extends AbstractBuilder{

    public static Logger LOG = LoggerFactory.getLogger(ArchetypeBuilder.class);
    public static final String ARCHETYPE_RESOURCES_PATH = "src/main/resources/archetype-resources";
    public static final String ARCHETYPE_RESOURCES_XML = "src/main/resources-filtered/META-INF/maven/archetype-metadata.xml";
    public static final String ARCHETYPE_POST_GROOVY = "src/main/resources/META-INF/";
    public static final String FUNKTION_YML = "funktion.yml";

    private static final String[] specialVersions = new String[]{
            "camel.version", "cxf.version", "cxf.plugin.version", "activemq.version",
            "karaf.version", "spring-boot.version", "weld.version"
    };

    private static final Set<String> sourceFileNames = new HashSet<String>(Arrays.asList("application.properties"));

    private static final Set<String> sourceFileExtensions = new HashSet<String>(Arrays.asList(
        "bpmn",
        "csv",
        "drl",
        "html",
        "groovy",
        "jade",
        "java",
        "jbpm",
        "js",
        "json",
        "jsp",
        "kotlin",
        "kt",
        "ks",
        "md",
        "properties",
        "scala",
        "sh",   /* sometimes we have .sh scripts in src/test/resources */
        "sql",
        "ssp",
        "ts",
        "txt",
        "xml",
        "yml",
        "yaml"
    ));

    private final Map<String, String> versionProperties = new HashMap<>();

    /**
     * Iterates through all projects in the given github organisation and generates an archetype for it
     */
    public void generateArchetypesFromGithubOrganisation(String githubOrg, File outputDir, List<String> dirs) throws IOException {
        GitHub github = GitHub.connectAnonymously();
        GHOrganization organization = github.getOrganization(githubOrg);
        Objects.notNull(organization, "No github organisation found for: " + githubOrg);
        Map<String, GHRepository> repositories = organization.getRepositories();
        Set<Map.Entry<String, GHRepository>> entries = repositories.entrySet();

        File cloneParentDir = new File(outputDir, "../git-clones");
        if (cloneParentDir.exists()) {
            Files.recursiveDelete(cloneParentDir);
        }
        for (Map.Entry<String, GHRepository> entry : entries) {
            String repoName = entry.getKey();
            GHRepository repo = entry.getValue();
            String url = repo.getGitTransportUrl();

            generateArchetypeFromGitRepo(outputDir, dirs, cloneParentDir, repoName, url, null);
        }
    }

    /**
     * Iterates through all projects in the given properties file adn generate an archetype for it
     */
    public void generateArchetypesFromGitRepoList(File file, File outputDir, List<String> dirs) throws IOException {
        File cloneParentDir = new File(outputDir, "../git-clones");
        if (cloneParentDir.exists()) {
            Files.recursiveDelete(cloneParentDir);
        }

        Properties properties = new Properties();
        try (FileInputStream is = new FileInputStream(file)) {
            properties.load(is);
        }

        for (Map.Entry<Object, Object> entry : properties.entrySet()) {
            LinkedList<String> values = new LinkedList<>(Arrays.asList(((String) entry.getValue()).split("\\|")));
            String gitrepo = values.removeFirst();
            String tag = values.isEmpty() ? null : values.removeFirst();
            generateArchetypeFromGitRepo(outputDir, dirs, cloneParentDir, (String)entry.getKey(), gitrepo, tag);
        }
    }

    protected void generateArchetypeFromGitRepo(File outputDir, List<String> dirs, File cloneParentDir, String repoName, String repoURL, String tag) throws IOException {
        String archetypeFolderName = repoName + "-archetype";
        File projectDir = new File(outputDir, archetypeFolderName);
        File destDir = new File(projectDir, ARCHETYPE_RESOURCES_PATH);
        //File cloneDir = new File(projectDir, ARCHETYPE_RESOURCES_PATH);
        File cloneDir = new File(cloneParentDir, archetypeFolderName);
        cloneDir.mkdirs();

        System.out.println("Cloning repo " + repoURL + " to " + cloneDir);
        cloneDir.getParentFile().mkdirs();
        if (cloneDir.exists()) {
            Files.recursiveDelete(cloneDir);
        }

        CloneCommand command = Git.cloneRepository().setCloneAllBranches(false).setURI(repoURL).setDirectory(cloneDir);

        try {
            command.call();
        } catch (Throwable e) {
            LOG.error("Failed to command remote repo " + repoURL + " due: " + e.getMessage(), e);
            throw new IOException("Failed to command remote repo " + repoURL + " due: " + e.getMessage(), e);
        }

        // Try to checkout a specific tag.
        if (tag == null) {
            tag = System.getProperty("repo.tag", "").trim();
        }
        if( !tag.isEmpty() ) {
            try {
                Git.open(cloneDir).checkout().setName(tag).call();
            } catch (Throwable e) {
                LOG.error("Failed checkout " + tag + " due: " + e.getMessage(), e);
                throw new IOException("Failed checkout " + tag + " due: " + e.getMessage(), e);
            }
        }

        File gitFolder = new File(cloneDir, ".git");
        Files.recursiveDelete(gitFolder);

        File pom = new File(cloneDir, "pom.xml");
        if (pom.exists()) {
            generateArchetype(cloneDir, pom, projectDir, false, dirs);
        } else {
            File from = cloneDir.getCanonicalFile();
            File to = destDir.getCanonicalFile();
            LOG.info("Copying git checkout from " + from + " to " + to);
            Files.copy(from, to);
        }

        String description = repoName.replace('-', ' ');

        dirs.add(repoName);
        File outputGitIgnoreFile = new File(projectDir, ".gitignore");
        if (!outputGitIgnoreFile.exists()) {
            ArchetypeUtils.writeGitIgnore(outputGitIgnoreFile);
        }
    }


    /**
     * Iterates through all nested directories and generates archetypes for all found, non-pom Maven projects.
     *
     * @param baseDir a directory to look for projects which may be converted to Maven Archetypes
     * @param outputDir target directory where Maven Archetype projects will be generated
     * @param clean regenerate the archetypes (clean the archetype target dir)?
     * @throws IOException
     */
    public void generateArchetypes(String containerType, File baseDir, File outputDir, boolean clean, List<String> dirs) throws IOException {
        LOG.debug("Generating archetypes from {} to {}", baseDir.getCanonicalPath(), outputDir.getCanonicalPath());
        File[] files = baseDir.listFiles();
        if (files != null) {
            for (File file: files) {
                if (file.isDirectory()) {
                    File projectDir = file;
                    File projectPom = new File(projectDir, "pom.xml");
                    if (projectPom.exists() && !skipImport(projectDir)) {
                        if (archetypeUtils.isValidProjectPom(projectPom)) {
                            String fileName = file.getName();
                            String archetypeDirName = fileName.replace("example", "archetype");
                            if (fileName.equals(archetypeDirName)) {
                                archetypeDirName += "-archetype";
                            }
                            archetypeDirName = containerType + "-" + archetypeDirName;

                            File archetypeDir = new File(outputDir, archetypeDirName);
                            generateArchetype(projectDir, projectPom, archetypeDir, clean, dirs);
                        } else {
                            // lets iterate through the children
                            String childContainerType = file.getName();
                            if (Strings.isNotBlank(containerType)) {
                                childContainerType = containerType + "-" + childContainerType;
                            }
                            generateArchetypes(childContainerType, file, outputDir, clean, dirs);
                        }
                    }
                }
            }
        }
    }

    /**
     * We should skip importing some quickstarts and if so, we should also not create an archetype for it
     */
    private static boolean skipImport(File dir) {
        String[] files = dir.list();
        if (files != null) {
            for (String name : files) {
                if (".skipimport".equals(name)) {
                    return true;
                }
            }
        }

        return false;
    }

    /**
     * Generates Maven archetype from existing project. This is lightweight version of <code>mvn archetype:create-from-project</code>.
     *
     * @param projectDir directory of source project which will be converted to Maven Archetype
     * @param projectPom pom file of source  project
     * @param archetypeDir output directory where Maven Archetype project will be created
     * @param clean remove the archetypeDir entirely?
     * @throws IOException
     */
    protected void generateArchetype(File projectDir, File projectPom, File archetypeDir, boolean clean, List<String> dirs) throws IOException {
        LOG.debug("Generating archetype from {} to {}", projectDir.getName(), archetypeDir.getCanonicalPath());

        // add to dirs
        dirs.add(archetypeDir.getName());

        File srcDir = new File(projectDir, "src/main");
        File testDir = new File(projectDir, "src/test");
        File outputSrcDir = new File(archetypeDir, "src");
        File outputGitIgnoreFile = new File(archetypeDir, ".gitignore");

        if (clean) {
            LOG.debug("Removing generated archetype dir {}", archetypeDir);
            Files.recursiveDelete(archetypeDir);
        } else if (outputSrcDir.exists() && outputGitIgnoreFile.exists() && fileIncludesLine(outputGitIgnoreFile, "src")) {
            LOG.debug("Removing generated src dir {}", outputSrcDir);
            Files.recursiveDelete(outputSrcDir);
            if (outputSrcDir.exists()) {
                throw new RuntimeException("The projectDir " + outputSrcDir + " should not exist!");
            }
        }

        // GenerateArchetypes dir for archetype resources - copied from original maven project. Sources will have
        // package names replaced with variable placeholders - to make them parameterizable during
        // mvn archetype:generate
        File archetypeOutputDir = new File(archetypeDir, ARCHETYPE_RESOURCES_PATH);
        // target archetype-metadata.xml file. it'll end in resources-filtered, so most of variables will be replaced
        // during the build of archetype project
        File metadataXmlOutFile = new File(archetypeDir, ARCHETYPE_RESOURCES_XML);

        Replacement replaceFunction = null;

        File mainSrcDir = null;
        for (String it : ArchetypeUtils.sourceCodeDirNames) {
            File dir = new File(srcDir, it);
            if (dir.exists()) {
                mainSrcDir = dir;
                break;
            }
        }

        Set<String> extraIgnorefiles = new HashSet<>();

        if (mainSrcDir != null) {
            // lets find the first projectDir which contains more than one child
            // to find the root-most package
            File rootPackage = archetypeUtils.findRootPackage(mainSrcDir);

            if (rootPackage != null) {
                String packagePath = archetypeUtils.relativePath(mainSrcDir, rootPackage);
                String packageName = packagePath.replace(File.separatorChar, '.');
                LOG.debug("Found root package in {}: {}", mainSrcDir, packageName);
                replaceFunction = new PatternReplacement(packageName);

                // lets recursively copy files replacing the package names
                File outputMainSrc = new File(archetypeOutputDir, archetypeUtils.relativePath(projectDir, mainSrcDir));
                copyCodeFiles(rootPackage, outputMainSrc, replaceFunction);

                // lets see if there's a funktion to replace
                File funktionYaml = new File(projectDir, FUNKTION_YML);
                if (funktionYaml.isFile()) {
                    File outFile = new File(archetypeDir, ARCHETYPE_RESOURCES_PATH + "/" + FUNKTION_YML);
                    copyFile(funktionYaml, outFile, replaceFunction, true);
                    extraIgnorefiles.add(FUNKTION_YML);
                    extraIgnorefiles.add("/" + FUNKTION_YML);
                }
            }
        }

        File testSrcDir = null;
        for (String it : ArchetypeUtils.sourceCodeDirNames) {
            File dir = new File(testDir, it);
            if (dir.exists()) {
                testSrcDir = dir;
                break;
            }
        }

        if (testSrcDir != null) {
            File rootPackage = archetypeUtils.findRootTestPackage(testSrcDir);

            if (rootPackage != null) {
                String packagePath = archetypeUtils.relativePath(testSrcDir, rootPackage);
                String packageName = packagePath.replace(File.separatorChar, '.');
                LOG.debug("Found root package in {}: {}", testSrcDir, packageName);

                Replacement testReplaceFunction = new PatternReplacement(packageName);
                if (replaceFunction == null) {
                    replaceFunction = testReplaceFunction;
                }

                File rootTestDir = new File(testSrcDir, packagePath);
                File outputTestSrc = new File(archetypeOutputDir, archetypeUtils.relativePath(projectDir, testSrcDir));
                if (rootTestDir.exists()) {
                    copyCodeFiles(rootTestDir, outputTestSrc, testReplaceFunction);
                } else {
                    copyCodeFiles(testSrcDir, outputTestSrc, testReplaceFunction);
                }
            }
        }

        if (replaceFunction == null) {
            replaceFunction= new IdentityReplacement();
        }
        // now copy pom.xml
        createArchetypeDescriptors(projectPom, archetypeDir, new File(archetypeOutputDir, "pom.xml"), metadataXmlOutFile, replaceFunction);

        // now lets copy all non-ignored files across
        copyOtherFiles(projectDir, projectDir, archetypeOutputDir, replaceFunction, extraIgnorefiles);

        // add missing .gitignore if missing
        if (!outputGitIgnoreFile.exists()) {
            ArchetypeUtils.writeGitIgnore(outputGitIgnoreFile);
        }
    }

    /**
     * This method:<ul>
     *     <li>Copies POM from original project to archetype-resources</li>
     *     <li>Generates <code></code>archetype-descriptor.xml</code></li>
     *     <li>Generates Archetype's <code>pom.xml</code> if not present in target directory.</li>
     * </ul>
     *
     * @param projectPom POM file of original project
     * @param archetypeDir target directory of created Maven Archetype project
     * @param archetypePom created POM file for Maven Archetype project
     * @param metadataXmlOutFile generated archetype-metadata.xml file
     * @param replaceFn replace function
     * @throws IOException
     */
    private void createArchetypeDescriptors(File projectPom, File archetypeDir, File archetypePom, File metadataXmlOutFile, Replacement replaceFn) throws IOException {
        LOG.debug("Parsing " + projectPom);
        String text = replaceFn.replace(IOHelpers.readFully(projectPom));

        // lets update the XML
        Document doc = archetypeUtils.parseXml(new InputSource(new StringReader(text)));
        Element root = doc.getDocumentElement();

        // let's get some values from the original project
        String originalArtifactId, originalName, originalDescription;
        Element artifactIdEl = (Element) findChild(root, "artifactId");

        Element nameEl = (Element) findChild(root, "name");
        Element descriptionEl = (Element) findChild(root, "description");
        if (artifactIdEl != null && artifactIdEl.getTextContent() != null && artifactIdEl.getTextContent().trim().length() > 0) {
            originalArtifactId = artifactIdEl.getTextContent().trim();
        } else {
            originalArtifactId = archetypeDir.getName();
        }
        if (nameEl != null && nameEl.getTextContent() != null && nameEl.getTextContent().trim().length() > 0) {
            originalName = nameEl.getTextContent().trim();
        } else {
            originalName = originalArtifactId;
        }
        if (descriptionEl != null && descriptionEl.getTextContent() != null && descriptionEl.getTextContent().trim().length() > 0) {
            originalDescription = descriptionEl.getTextContent().trim();
        } else {
            originalDescription = originalName;
        }

        Map<String, String> propertyNameSet = new LinkedHashMap<>();

        if (root != null) {
            // remove the parent element and the following text Node
            NodeList parents = root.getElementsByTagName("parent");
            if (parents.getLength() > 0) {
                boolean removeParentPom = true;
                Element parentNode = (Element) parents.item(0);
                Element groupId = DomHelper.firstChild(parentNode, "groupId");
                if (groupId != null) {
                    String textContent = groupId.getTextContent();
                    if (textContent != null) {
                        textContent = textContent.trim();
                        if (Objects.equal(textContent, "io.fabric8.funktion.starter")) {
                            removeParentPom = false;
                        }
                    }
                }
                if (removeParentPom) {
                    if (parentNode.getNextSibling().getNodeType() == Node.TEXT_NODE) {
                        root.removeChild(parents.item(0).getNextSibling());
                    }
                    root.removeChild(parents.item(0));
                }
            }

            // lets load all the properties defined in the <properties> element in the pom.
            Map<String, String> pomProperties = new LinkedHashMap<>();

            NodeList propertyElements = root.getElementsByTagName("properties");
            if (propertyElements.getLength() > 0)  {
                Element propertyElement = (Element) propertyElements.item(0);
                NodeList children = propertyElement.getChildNodes();
                for (int cn = 0; cn < children.getLength(); cn++) {
                    Node e = children.item(cn);
                    if (e instanceof Element) {
                        pomProperties.put(e.getNodeName(), e.getTextContent());
                    }
                }
            }
            if (LOG.isDebugEnabled()) {
                for (Map.Entry<String, String> entry : pomProperties.entrySet()) {
                    LOG.debug("pom property: {}={}", entry.getKey(), entry.getValue());
                }
            }

            // lets find all the property names
            NodeList children = root.getElementsByTagName("*");
            for (int cn = 0; cn < children.getLength(); cn++) {
                Node e = children.item(cn);
                if (e instanceof Element) {
                    String cText = e.getTextContent();
                    String prefix = "${";
                    if (cText.startsWith(prefix)) {
                        int offset = prefix.length();
                        int idx = cText.indexOf("}", offset + 1);
                        if (idx > 0) {
                            String name = cText.substring(offset, idx);
                            if (!pomProperties.containsKey(name) && isValidRequiredPropertyName(name)) {
                                // use default value if we have one, but favor value from this pom over the bom pom
                                String value = pomProperties.get(name);
                                if (value == null) {
                                    value = versionProperties.get(name);
                                }
                                // lets use dash instead of dot
                                name = name.replace('.', '-');
                                propertyNameSet.put(name, value);
                            }
                        }
                    } else {
                        // pickup some special property names we want to be in the archetype as requiredProperty
                        String cName = e.getNodeName();
                        if (isValidRequiredPropertyName(cName) && isSpecialPropertyName(cName)) {
                            String value = e.getTextContent();
                            if (value != null) {
                                value = value.trim();
                                // lets use dash instead of dot
                                cName = cName.replace('.', '-');
                                propertyNameSet.put(cName, value);
                                // and use a placeholder token in its place, so we can allow to specify the version dynamically in the archetype
                                String token = "${" + cName + "}";
                                e.setTextContent(token);
                            }
                        }
                    }
                }
            }

            // now lets replace the contents of some elements (adding new elements if they are not present)
            List<String> beforeNames = Arrays.asList("artifactId", "version", "packaging", "name", "properties");
            replaceOrAddElementText(doc, root, "version", "${version}", beforeNames);
            replaceOrAddElementText(doc, root, "artifactId", "${artifactId}", beforeNames);
            replaceOrAddElementText(doc, root, "groupId", "${groupId}", beforeNames);
        }
        archetypePom.getParentFile().mkdirs();

        // remove copyright header which is the first comment, as we do not want that in the archetypes
        removeCommentNodes(doc);

        archetypeUtils.writeXmlDocument(doc, archetypePom);

        // lets update the archetype-metadata.xml file
        String archetypeXmlText;
        if (archetypeDir.getName().contains("groovy")) {
            archetypeXmlText = groovyArchetypeXmlText();
        } else if (archetypeDir.getName().contains("kotlin")) {
            archetypeXmlText = kotlinArchetypeXmlText();
        } else if (archetypeDir.getName().contains("soap-rest-bridge")) {
            archetypeXmlText = soapRestBridgeArchetypeXmlText();
            File postGroovyFile = new File(archetypeDir, ARCHETYPE_POST_GROOVY);
            String groovyScript = soapRestBridgePostGroovyText();
            postGroovyFile.mkdirs();
            ArchetypeUtils.writeFile(new File(postGroovyFile, "archetype-post-generate.groovy"), groovyScript, false);
        } else {
            archetypeXmlText = defaultArchetypeXmlText();
        }

        Document archDoc = archetypeUtils.parseXml(new InputSource(new StringReader(archetypeXmlText)));
        Element archRoot = archDoc.getDocumentElement();

        // replace @name attribute on root element
        archRoot.setAttribute("name", archetypeDir.getName());

        LOG.debug(("Found property names: {}"), propertyNameSet);
        // lets add all the properties
        Element requiredProperties = replaceOrAddElement(archDoc, archRoot, "requiredProperties", Arrays.asList("fileSets"));

        // lets add the various properties in
        for (Map.Entry<String, String> entry : propertyNameSet.entrySet()) {
            requiredProperties.appendChild(archDoc.createTextNode("\n" + indent + indent));
            Element requiredProperty = archDoc.createElement("requiredProperty");
            requiredProperties.appendChild(requiredProperty);
            requiredProperty.setAttribute("key", entry.getKey());
            if (entry.getValue() != null) {
                requiredProperty.appendChild(archDoc.createTextNode("\n" + indent + indent + indent));
                Element defaultValue = archDoc.createElement("defaultValue");
                requiredProperty.appendChild(defaultValue);
                defaultValue.appendChild(archDoc.createTextNode(entry.getValue()));
            }
            requiredProperty.appendChild(archDoc.createTextNode("\n" + indent + indent));
        }
        requiredProperties.appendChild(archDoc.createTextNode("\n" + indent));

        metadataXmlOutFile.getParentFile().mkdirs();
        archetypeUtils.writeXmlDocument(archDoc, metadataXmlOutFile);

        generatePomIfRequired(archetypeDir, originalName, originalDescription);
    }

    protected File generatePomIfRequired(File archetypeDir, String originalName, String originalDescription) throws IOException {
        File archetypeProjectPom = new File(archetypeDir, "pom.xml");
        // now generate Archetype's pom
        if (!archetypeProjectPom.exists()) {
            StringWriter sw = new StringWriter();
            IOHelpers.copy(new InputStreamReader(getClass().getResourceAsStream("default-archetype-pom.xml")), sw);
            Document pomDocument = archetypeUtils.parseXml(new InputSource(new StringReader(sw.toString())));

            List<String> emptyList = Collections.emptyList();

            // lets replace the parent pom's version
            String projectVersion = System.getProperty("project.version");
            if (Strings.isNotBlank(projectVersion)) {
                Element parent = DomHelper.firstChild(pomDocument.getDocumentElement(), "parent");
                if (parent != null) {
                    Element version = DomHelper.firstChild(parent, "version");
                    if (version != null) {
                        if (!Objects.equal(version.getTextContent(), projectVersion)) {
                            version.setTextContent(projectVersion);
                        }
                    }
                }
            }


            // artifactId = original artifactId with "-archetype"
            Element artifactId = replaceOrAddElement(pomDocument, pomDocument.getDocumentElement(), "artifactId", emptyList);
            artifactId.setTextContent(archetypeDir.getName());

            // name = "Fabric8 :: Qickstarts :: xxx" -> "Fabric8 :: Archetypes :: xxx"
            Element name = replaceOrAddElement(pomDocument, pomDocument.getDocumentElement(), "name", emptyList);
            if (originalName.contains(" :: ")) {
                String[] originalNameTab = originalName.split(" :: ");
                if (originalNameTab.length > 2) {
                    StringBuilder sb = new StringBuilder();
                    sb.append("Fabric8 :: Archetypes");
                    for (int idx = 2; idx < originalNameTab.length; idx++) {
                        sb.append(" :: ").append(originalNameTab[idx]);
                    }
                    name.setTextContent(sb.toString());
                } else {
                    name.setTextContent("Fabric8 :: Archetypes :: " + originalNameTab[1]);
                }
            } else {
                name.setTextContent("Fabric8 :: Archetypes :: " + originalName);
            }

            // description = "Creates a new " + originalDescription
            Element description = replaceOrAddElement(pomDocument, pomDocument.getDocumentElement(), "description", emptyList);
            description.setTextContent("Creates a new " + originalDescription);

            archetypeUtils.writeXmlDocument(pomDocument, archetypeProjectPom);
        }
        return archetypeProjectPom;
    }

    /**
     * Remove any comment nodes from the doc (only top level).
     * <p/>
     * This is used to remove copyright headers embedded as comment in the pom.xml files etc.
     */
    private void removeCommentNodes(Document doc) {
        List<Node> toRemove = new ArrayList<>();
        NodeList children = doc.getChildNodes();
        for (int cn = 0; cn < children.getLength(); cn++) {
            Node child = children.item(cn);
            if (Node.COMMENT_NODE == child.getNodeType()) {
                toRemove.add(child);
            }
        }
        for (Node child : toRemove) {
            doc.removeChild(child);
        }
    }

    /**
     * Creates new element as child of <code>parent</code> and sets its text content
     */
    protected Element replaceOrAddElementText(Document doc, Element parent, String name, String content, List<String> beforeNames) {
        Element element = replaceOrAddElement(doc, parent, name, beforeNames);
        element.setTextContent(content);
        return element;
    }

    /**
     * Returns new or existing Element from <code>parent</code>
     */
    private Element replaceOrAddElement(Document doc, Element parent, String name, List<String> beforeNames) {
        NodeList children = parent.getChildNodes();
        List<Element> elements = new LinkedList<Element>();
        for (int cn = 0; cn < children.getLength(); cn++) {
            if (children.item(cn) instanceof Element && children.item(cn).getNodeName().equals(name)) {
                elements.add((Element) children.item(cn));
            }
        }
        Element element;
        if (elements.isEmpty()) {
            Element newElement = doc.createElement(name);
            Node first = null;
            for (String n: beforeNames) {
                first = findChild(parent, n);
                if (first != null) {
                    break;
                }
            }

            Node node;
            if (first != null) {
                node = first;
            } else {
                node = parent.getFirstChild();
            }
            Text text = doc.createTextNode("\n" + indent);
            parent.insertBefore(text, node);
            parent.insertBefore(newElement, text);
            element = newElement;
        } else {
            element = elements.get(0);
        }

        return element;
    }

    /**
     * Checks whether the file contains specific line. Partial matches do not count.
     */
    private boolean fileIncludesLine(File file, String matches) throws IOException {
        for (String line: Files.readLines(file)) {
            String trimmed = line.trim();
            if (trimmed.equals(matches)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Copies all java/groovy/kotlin/scala code recursively. <code>replaceFn</code> is used to modify the content of files.
     */
    protected void copyCodeFiles(File rootPackage, File outDir, Replacement replaceFn) throws IOException {
        if (rootPackage.isFile()) {
            copyFile(rootPackage, outDir, replaceFn);
        } else {
            outDir.mkdirs();
            String[] names = rootPackage.list();
            if (names != null) {
                for (String name: names) {
                    copyCodeFiles(new File(rootPackage, name), new File(outDir, name), replaceFn);
                }
            }
        }
    }

    /**
     * Copies single file from <code>src</code> to <code>dest</code>.
     * If the file is source file, variable references will be escaped, so they'll survive Velocity template merging.
     */
    protected void copyFile(File src, File dest, Replacement replaceFn) throws IOException {
        boolean isSource = isSourceFile(src);
        copyFile(src, dest, replaceFn, isSource);
    }

    protected void copyFile(File src, File dest, Replacement replaceFn, boolean isSource) throws IOException {
        if (replaceFn != null && isSource) {
            String original = IOHelpers.readFully(src);
            String escapedContent = original;
            if (original.contains("${")) {
                String replaced = escapedContent.replaceAll(Pattern.quote("${"), "\\${D}{");
                // add Velocity expression at the beginning of the result file.
                // Velocity is used by mvn archetype:generate
                escapedContent = "#set( $D = '$' )\n" + replaced;
            }
            if (original.contains("##")) {
                String replaced = escapedContent.replaceAll(Pattern.quote("##"), "\\${H}");
                // add Velocity expression at the beginning of the result file.
                // Velocity is used by mvn archetype:generate
                escapedContent = "#set( $H = '##' )\n" + replaced;
            }
            // do additional replacement
            String text = replaceFn.replace(escapedContent);
            IOHelpers.writeFully(dest, text);
        } else {
            if (LOG.isDebugEnabled()) {
                LOG.warn("Not a source dir as the extension is {}", Files.getExtension(src.getName()));
            }
            Files.copy(src, dest);
        }
    }

    /**
     * Copies all other source files which are not excluded
     */
    protected void copyOtherFiles(File projectDir, File srcDir, File outDir, Replacement replaceFn, Set<String> extraIgnorefiles) throws IOException {
        if (archetypeUtils.isValidFileToCopy(projectDir, srcDir, extraIgnorefiles)) {
            if (srcDir.isFile()) {
                copyFile(srcDir, outDir, replaceFn);
            } else {
                outDir.mkdirs();
                String[] names = srcDir.list();
                if (names != null) {
                    for (String name: names) {
                        copyOtherFiles(projectDir, new File(srcDir, name), new File(outDir, name), replaceFn, extraIgnorefiles);
                    }
                }
            }
        }
    }

    /**
     * Returns true if this file is a valid source file name
     */
    protected boolean isSourceFile(File file) {
        String name = file.getName();
        String extension = Files.getExtension(name).toLowerCase();
        return sourceFileExtensions.contains(extension) || sourceFileNames.contains(name);
    }

    /**
     * Returns true if this is a valid archetype property name, so excluding basedir and maven "project." names
     */
    protected boolean isValidRequiredPropertyName(String name) {
        return !name.equals("basedir") && !name.startsWith("project.") && !name.startsWith("pom.") && !name.equals("package");
    }

    /**
     * Returns true if its a special property name such as Camel, ActiveMQ etc.
     */
    protected boolean isSpecialPropertyName(String name) {
        for (String special : specialVersions) {
            if (special.equals(name)) {
                return true;
            }
        }
        return false;
    }

    protected Node findChild(Element parent, String n) {
        NodeList children = parent.getChildNodes();
        for (int cn = 0; cn < children.getLength(); cn++) {
            if (n.equals(children.item(cn).getNodeName())) {
                return children.item(cn);
            }
        }
        return null;
    }

    protected String defaultArchetypeXmlText() throws IOException {
        InputStreamReader reader = new InputStreamReader(getClass().getResourceAsStream("default-archetype-descriptor.xml"));
        StringWriter sw = new StringWriter();
        try {
            IOHelpers.copy(reader, sw);
        } finally {
            IOHelpers.close(reader, sw);
        }
        return sw.toString();
    }

    private String groovyArchetypeXmlText() throws IOException {
        InputStreamReader reader = new InputStreamReader(getClass().getResourceAsStream("groovy-archetype-descriptor.xml"));
        StringWriter sw = new StringWriter();
        try {
            IOHelpers.copy(reader, sw);
        } finally {
            IOHelpers.close(reader, sw);
        }
        return sw.toString();
    }

    private String kotlinArchetypeXmlText() throws IOException {
        InputStreamReader reader = new InputStreamReader(getClass().getResourceAsStream("kotlin-archetype-descriptor.xml"));
        StringWriter sw = new StringWriter();
        try {
            IOHelpers.copy(reader, sw);
        } finally {
            IOHelpers.close(reader, sw);
        }
        return sw.toString();
    }
    
    private String soapRestBridgeArchetypeXmlText() throws IOException {
        InputStreamReader reader = new InputStreamReader(getClass().getResourceAsStream("soap-rest-bridge-archetype-descriptor.xml"));
        StringWriter sw = new StringWriter();
        try {
            IOHelpers.copy(reader, sw);
        } finally {
            IOHelpers.close(reader, sw);
        }
        return sw.toString();
    }
    
    private String soapRestBridgePostGroovyText() throws IOException {
        InputStreamReader reader = new InputStreamReader(getClass().getResourceAsStream("soap-rest-bridge-post-generate.groovy"));
        StringWriter sw = new StringWriter();
        try {
            IOHelpers.copy(reader, sw);
        } finally {
            IOHelpers.close(reader, sw);
        }
        return sw.toString();
    }
    

    /**
     * Interface for (String) => (String) functions
     */
    private static interface Replacement {
        public String replace(String token);
    }

    /**
     * Identity Replacement.
     */
    private static class IdentityReplacement implements Replacement {
        public String replace(String token) {
            return token;
        }
    }

    private static class PatternReplacement implements Replacement {
        private final String pattern;

        public PatternReplacement(String pattern) {
            this.pattern = pattern;
        }

        @Override
        public String replace(String token) {
            return Strings.replaceAllWithoutRegex(token, pattern, "${package}");
        }

        @Override
        public String toString() {
            return "Replacement(" + pattern + ")";
        }
    }
}
