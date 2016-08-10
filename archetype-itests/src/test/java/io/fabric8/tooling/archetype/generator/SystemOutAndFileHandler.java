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
package io.fabric8.tooling.archetype.generator;

import org.apache.maven.shared.invoker.PrintStreamHandler;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;

/**
 * Writes output to standard out or some other output stream along with an extra piped output stream or file
 */
public class SystemOutAndFileHandler extends PrintStreamHandler {
    private final PrintStream tee;
    private final boolean alwaysFlush;

    public SystemOutAndFileHandler(File tee) throws FileNotFoundException {
        this(true, new PrintStream(new FileOutputStream(tee, true)));
    }

    public SystemOutAndFileHandler(boolean alwaysFlush, PrintStream tee) {
        this.tee = tee;
        this.alwaysFlush = alwaysFlush;
    }

    public SystemOutAndFileHandler(PrintStream out, boolean alwaysFlush, PrintStream tee) {
        super(out, alwaysFlush);
        this.alwaysFlush = alwaysFlush;
        this.tee = tee;
    }


    @Override
    public void consumeLine(String line) {
        super.consumeLine(line);
        tee.println(line);
        if (alwaysFlush) {
            tee.flush();
        }
    }
}
