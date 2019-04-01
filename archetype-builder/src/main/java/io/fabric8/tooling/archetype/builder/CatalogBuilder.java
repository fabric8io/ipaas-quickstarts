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
import io.fabric8.utils.DomHelper;
import io.fabric8.utils.Files;
import io.fabric8.utils.IOHelpers;
import io.fabric8.utils.Objects;
import io.fabric8.utils.Strings;
import org.apache.commons.text.StringEscapeUtils;
import org.eclipse.jgit.api.CloneCommand;
import org.eclipse.jgit.api.Git;
import org.kohsuke.github.GHOrganization;
import org.kohsuke.github.GHRepository;
import org.kohsuke.github.GitHub;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.Text;
import org.xml.sax.InputSource;

import java.io.*;
import java.util.*;
import java.util.regex.Pattern;

/**
 * This class is a replacement for <code>mvn archetype:create-from-project</code> without dependencies to
 * maven-archetype related libraries.
 */
public class CatalogBuilder extends AbstractBuilder {

    public static Logger LOG = LoggerFactory.getLogger(CatalogBuilder.class);

    private File bomFile;
    private File catalogXmlFile;
    private PrintWriter printWriter;
    private final Map<String, String> versionProperties = new HashMap<>();

    private File archetypesPomFile;
    private Set<String> archetypesPomArtifactIds;
    private Set<String> missingArtifactIds = new TreeSet<>();

    public CatalogBuilder(File catalogXmlFile) {
        this.catalogXmlFile = catalogXmlFile;
    }

    public Set<String> getMissingArtifactIds() {
        return missingArtifactIds;
    }

    public void setBomFile(File bomFile) {
        this.bomFile = bomFile;
    }

    /**
     * Starts generation of Archetype Catalog (see: http://maven.apache.org/xsd/archetype-catalog-1.0.0.xsd)
     *
     * @throws IOException
     */
    public void configure() throws IOException {
        if (archetypesPomFile != null) {
            archetypesPomArtifactIds = loadArchetypesPomArtifactIds(archetypesPomFile);
        }
        catalogXmlFile.getParentFile().mkdirs();
        LOG.info("Writing catalog: " + catalogXmlFile);
        printWriter = new PrintWriter(new OutputStreamWriter(new FileOutputStream(catalogXmlFile), "UTF-8"));

        printWriter.println("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
            "<archetype-catalog xmlns=\"http://maven.apache.org/plugins/maven-archetype-plugin/archetype-catalog/1.0.0\"\n" +
            indent + indent + "xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n" +
            indent + indent + "xsi:schemaLocation=\"http://maven.apache.org/plugins/maven-archetype-plugin/archetype-catalog/1.0.0 http://maven.apache.org/xsd/archetype-catalog-1.0.0.xsd\">\n" +
            indent + "<archetypes>");

        if (bomFile != null && bomFile.exists()) {
            // read all properties of the bom, so we have default values for ${ } placeholders
            String text = IOHelpers.readFully(bomFile);
            Document doc = archetypeUtils.parseXml(new InputSource(new StringReader(text)));
            Element root = doc.getDocumentElement();

            // lets load all the properties defined in the <properties> element in the bom pom.
            NodeList propertyElements = root.getElementsByTagName("properties");
            if (propertyElements.getLength() > 0)  {
                Element propertyElement = (Element) propertyElements.item(0);
                NodeList children = propertyElement.getChildNodes();
                for (int cn = 0; cn < children.getLength(); cn++) {
                    Node e = children.item(cn);
                    if (e instanceof Element) {
                        versionProperties.put(e.getNodeName(), e.getTextContent());
                    }
                }
            }
            if (LOG.isDebugEnabled()) {
                for (Map.Entry<String, String> entry : versionProperties.entrySet()) {
                    LOG.debug("bom property: {}={}", entry.getKey(), entry.getValue());
                }
            }
        }
    }

    protected Set<String> loadArchetypesPomArtifactIds(File archetypesPomFile) throws IOException {
        Set<String> answer = new TreeSet<>();
        if (!archetypesPomFile.isFile() || !archetypesPomFile.exists()) {
            LOG.warn("archetypes pom.xml file does not exist!: " + archetypesPomFile);
            return null;

        }
        try {
            Document doc = archetypeUtils.parseXml(new InputSource(new FileReader(archetypesPomFile)));
            Element root = doc.getDocumentElement();

            // lets load all the properties defined in the <properties> element in the bom pom.
            NodeList moduleElements = root.getElementsByTagName("module");
            for (int i = 0, size = moduleElements.getLength(); i < size; i++) {
                Element moduleElement = (Element) moduleElements.item(i);
                String module = moduleElement.getTextContent();
                if (Strings.isNotBlank(module)) {
                    answer.add(module);
                }
            }
            LOG.info("Loaded archetypes module names: "+ answer);
            return answer;
        } catch (FileNotFoundException e) {
            throw new IOException("Failed to parse " + archetypesPomFile + ". " + e, e);
        }
    }

    /**
     * Completes generation of Archetype Catalog.
     */
    public void close() {
        printWriter.println(indent + "</archetypes>\n" +
                "</archetype-catalog>");
        printWriter.close();
    }

    protected void addArchetypeMetaData(File pom, String outputName) throws FileNotFoundException {
        Document doc = archetypeUtils.parseXml(new InputSource(new FileReader(pom)));
        Element root = doc.getDocumentElement();

        String groupId = archetypeUtils.firstElementText(root, "groupId", "io.fabric8.archetypes");
        String artifactId = archetypeUtils.firstElementText(root, "artifactId", outputName);
        String description = archetypeUtils.firstElementText(root, "description", "");
        String version = "";

        NodeList parents = root.getElementsByTagName("parent");
        if (parents.getLength() > 0) {
            version = archetypeUtils.firstElementText((Element) parents.item(0), "version", "");
        }
        if (version.length() == 0) {
            version = archetypeUtils.firstElementText(root, "version", "");
        }

        if (archetypesPomArtifactIds != null) {
            if (!archetypesPomArtifactIds.contains(artifactId)) {
                LOG.warn("Not adding archetype: " + artifactId + " to the  catalog as it is not included in the " + archetypesPomFile);
                missingArtifactIds.add(artifactId);
                return;
            }
        }
        printWriter.println(String.format(indent + indent + "<archetype>\n" +
            indent + indent + indent + "<groupId>%s</groupId>\n" +
            indent + indent + indent + "<artifactId>%s</artifactId>\n" +
            indent + indent + indent + "<version>%s</version>\n" +
            indent + indent + indent + "<description>%s</description>\n" +
            indent + indent + "</archetype>", groupId, artifactId, version, StringEscapeUtils.escapeXml10(description)));
    }

    public void setArchetypesPomFile(File archetypesPomFile) {
        this.archetypesPomFile = archetypesPomFile;
    }


}
