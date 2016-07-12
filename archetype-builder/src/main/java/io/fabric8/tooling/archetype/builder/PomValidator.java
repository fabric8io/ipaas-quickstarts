/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.fabric8.tooling.archetype.builder;

import io.fabric8.tooling.archetype.ArchetypeUtils;
import io.fabric8.utils.DomHelper;
import io.fabric8.utils.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.Text;
import org.xml.sax.InputSource;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

/**
 * Validates the pom.xml files from the quickstarts to check that
 * all the depdency and plugin versions are configured using maven properties
 */
public class PomValidator {
    private static final transient Logger LOG = LoggerFactory.getLogger(PomValidator.class);

    private final File gitCloneDir;
    private final ArchetypeUtils archetypeUtils = new ArchetypeUtils();
    private final Map<String, String> mavenDependenciesProperties = new HashMap<>();
    private final Map<String, String> addMavenDependency = new TreeMap<>();


    public PomValidator(File gitCloneDir) {
        this.gitCloneDir = gitCloneDir;
    }

    public static void main(String[] args) {
        String path = "../git-clones";
        if (args.length > 9) {
            path = args[0];
        }

        PomValidator validator = new PomValidator(new File(path));
        validator.validate();
    }

    public void validate() {
        File[] files = gitCloneDir.listFiles();
        if (files == null || files.length == 0) {
            warn("No folders found inside git clone dir: " + gitCloneDir);
        } else {
            loadMavenDependencyProperties();

            for (File file : files) {
                if (file.isDirectory()) {
                    File pom = new File(file, "pom.xml");
                    if (pom.exists() && pom.isFile()) {
                        validatePom(file.getName(), pom);
                    }
                }
            }

            if (!addMavenDependency.isEmpty()) {
                System.out.println();
                System.out.println();
                if (!addPropertiesToMavenDependenciesPom()) {
                    System.out.println("Please add the following lines to: https://github.com/fabric8io/fabric8-maven-dependencies/blob/master/pom.xml#L39");
                    System.out.println("Inside the <properties> element");
                    System.out.println();
                    for (Map.Entry<String, String> entry : addMavenDependency.entrySet()) {
                        String propertyName = entry.getKey();
                        String propertyValue = entry.getValue();
                        System.out.println("    <" + propertyName + ">" + propertyValue + "</" + propertyName + ">");
                    }
                }
                System.out.println();
            }
        }
    }

    protected void warn(String message) {
        System.out.println("WARNING: " + message);
    }

    protected void loadMavenDependencyProperties() {
        String urlText = "https://raw.githubusercontent.com/fabric8io/fabric8-maven-dependencies/master/pom.xml";

        Element root = null;
        try {
            URL url = new URL(urlText);
            Document doc = archetypeUtils.parseXml(new InputSource(url.openStream()));
            root = doc.getDocumentElement();
        } catch (Exception e) {
            LOG.error("Failed to parse " + urlText + ". " + e, e);
            return;
        }
        if (root == null) {
            return;
        }

        // lets load all the properties defined in the <properties> element
        Element propertyElement = getPropertiesElement(root);
        if (propertyElement != null) {
            NodeList children = propertyElement.getChildNodes();
            for (int cn = 0; cn < children.getLength(); cn++) {
                Node e = children.item(cn);
                if (e instanceof Element) {
                    mavenDependenciesProperties.put(e.getNodeName(), e.getTextContent());
                }
            }
        }
    }

    protected Element getPropertiesElement(Element root) {
        Element propertyElement = null;
        NodeList propertyElements = root.getElementsByTagName("properties");
        if (propertyElements.getLength() > 0) {
            propertyElement = (Element) propertyElements.item(0);
        }
        return propertyElement;
    }

    protected void validatePom(String projectName, File pom) {
        LOG.debug("Validating " + pom);

        String prefix = "quickstart " + Strings.stripSuffix(projectName, "-archetype") + " pom.xml";

        Element root = null;
        try {
            Document doc = archetypeUtils.parseXml(new InputSource(new FileReader(pom)));
            root = doc.getDocumentElement();
        } catch (Exception e) {
            LOG.error("Failed to parse " + pom + ". " + e, e);
            return;
        }
        if (root == null) {
            return;
        }

        Element propertyElement = getPropertiesElement(root);

        // lets load all the properties defined in the <properties> element in the bom pom.
        NodeList dependencyElements = root.getElementsByTagName("dependency");
        for (int i = 0, size = dependencyElements.getLength(); i < size; i++) {
            Element dependency = (Element) dependencyElements.item(i);
            validateVersion(prefix + " dependency", dependency, propertyElement);
        }

        NodeList pluginElements = root.getElementsByTagName("plugin");
        for (int i = 0, size = pluginElements.getLength(); i < size; i++) {
            Element plugin = (Element) pluginElements.item(i);
            validateVersion(prefix + " plugin", plugin, propertyElement);
        }
    }

    private boolean addPropertiesToMavenDependenciesPom() {
        File pom = new File("../../fabric8-maven-dependencies/pom.xml");
        if (pom.isFile() && pom.exists()) {
            Document doc;
            try {
                doc = archetypeUtils.parseXml(new InputSource(new FileReader(pom)));
            } catch (Exception e) {
                LOG.error("Failed to parse " + pom + ". " + e, e);
                return false;
            }
            Element root = doc.getDocumentElement();

            Element properties = getPropertiesElement(root);
            if (properties != null) {
                Set<String> addedProperties = new TreeSet<>();
                for (Map.Entry<String, String> entry : addMavenDependency.entrySet()) {
                    String propertyName = entry.getKey();
                    String propertyValue = entry.getValue();
                    if (addPropertyElement(properties, propertyName, propertyValue)) {
                        addedProperties.add(propertyName);
                    }
                }

                if (addedProperties.size() > 0) {
                    System.out.println("Added properties " + addMavenDependency.keySet() + " to " + pom);
                    System.out.println("Please commit this change and submit a Pull Request!!!");

                    try {
                        archetypeUtils.writeXmlDocument(doc, pom);
                        return true;
                    } catch (IOException e) {
                        LOG.error("Failed to write " + pom + ". " + e, e);
                    }
                }
            }
        }
        return false;
    }

    /**
     * Lets add a child element at the right place
     */
    private boolean addPropertyElement(Element element, String elementName, String textContent) {
        Document doc = element.getOwnerDocument();
        Element newElement = doc.createElement(elementName);
        newElement.setTextContent(textContent);
        Text textNode = doc.createTextNode("\n    ");

        NodeList childNodes = element.getChildNodes();
        for (int i = 0, size = childNodes.getLength(); i < size; i++) {
            Node item = childNodes.item(i);
            if (item instanceof Element) {
                Element childElement = (Element) item;
                int value = childElement.getTagName().compareTo(elementName);
                if (value == 0) {
                    return false;
                }
                if (value > 0) {
                    element.insertBefore(textNode, childElement);
                    element.insertBefore(newElement, textNode);
                    return true;
                }
            }
        }
        // lets insert at the end
        element.appendChild(textNode);
        element.appendChild(newElement);
        return true;
    }

    private void validateVersion(String kind, Element element, Element propertyElement) {
        Element version = DomHelper.firstChild(element, "version");
        if (version != null) {
            String textContent = version.getTextContent();
            if (textContent != null) {
                textContent = textContent.trim();
                if (Strings.isNotBlank(textContent)) {
                    String groupId = textContent(DomHelper.firstChild(element, "groupId"));
                    String artifactId = textContent(DomHelper.firstChild(element, "artifactId"));

                    String coords = groupId + ":" + artifactId;
                    if (Strings.isNullOrBlank(groupId)) {
                        coords = artifactId;
                    }

                    if (!textContent.startsWith("${") || !textContent.endsWith("}")) {
                        warn("" + kind + " " + coords + " does not use a maven property for version; has value: " + textContent);
                    } else {
                        String propertyName = textContent.substring(2, textContent.length() - 1);
                        String propertyValue = "????";
                        boolean unknownValue = true;
                        if (propertyElement != null) {
                            Element property = DomHelper.firstChild(propertyElement, propertyName);
                            if (property != null) {
                                propertyValue = property.getTextContent();
                                if (Strings.isNotBlank(propertyValue)) {
                                    unknownValue = false;
                                }
                            }
                        }
                        if (!mavenDependenciesProperties.containsKey(propertyName)) {
                            if (!addMavenDependency.containsKey(propertyName) || !unknownValue) {
                                addMavenDependency.put(propertyName, propertyValue);
                            }
                        }
                    }
                }
            }
        }

    }

    private String textContent(Element element) {
        if (element != null) {
            return element.getTextContent();
        }
        return null;
    }
}
