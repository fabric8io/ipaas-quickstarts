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

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import io.fabric8.utils.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GenerateArchetypes {

    public static Logger LOG = LoggerFactory.getLogger(GenerateArchetypes.class);

    public static void main(String[] args) throws Exception {

        String sourcedir = System.getProperty("sourcedir");
        String outputPath = System.getProperty("outputdir", ",");
        File outputDir = new File(outputPath);

        ArchetypeBuilder builder = new ArchetypeBuilder();

        List<String> dirs = new ArrayList<>();
        try {
            if( sourcedir != null ) {
                File sourceDirectory = new File(sourcedir);
                if (!sourceDirectory.exists() || !sourceDirectory.isDirectory()) {
                    throw new IllegalArgumentException("Source directory: " + sourcedir + " is not a valid directory");
                }
                builder.generateArchetypes("", sourceDirectory, outputDir, false, dirs);
            }

            String repoListFile = System.getProperty("repos", "").trim();
            if( repoListFile.isEmpty() ) {
                builder.generateArchetypesFromGithubOrganisation("fabric8-quickstarts", outputDir, dirs);
            } else {
                builder.generateArchetypesFromGitRepoList(new File(repoListFile), outputDir, dirs);
            }

        } finally {
            LOG.debug("Completed the generation. Closing!");
        }

        StringBuffer sb = new StringBuffer();
        for (String dir : dirs) {
            sb.append("\n\t<module>" + dir + "</module>");
        }
        System.out.println("Done creating archetypes:\n" + sb + "\n");

        PomValidator validator = new PomValidator(new File("../git-clones"));
        validator.validate();
    }
}
