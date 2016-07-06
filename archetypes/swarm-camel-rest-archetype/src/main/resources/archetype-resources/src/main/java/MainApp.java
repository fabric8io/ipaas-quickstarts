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
package ${package};

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.jboss.shrinkwrap.impl.base.spec.WebArchiveImpl;
import org.wildfly.swarm.container.Container;
import org.wildfly.swarm.jolokia.JolokiaFraction;
import org.wildfly.swarm.undertow.WARArchive;

public class MainApp {

	public static void main(String[] args) throws Exception {
		new Container().fraction(new JolokiaFraction("/jmx")).start(true).deploy();
	}
}
