/**
 *  Copyright 2005-2016 Red Hat, Inc.
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
package io.fabric8.quickstarts.camel.teiid;

import org.springframework.jdbc.datasource.SimpleDriverDataSource;
import org.teiid.adminapi.Model;
import org.teiid.adminapi.impl.ModelMetaData;
import org.teiid.runtime.EmbeddedConfiguration;
import org.teiid.runtime.EmbeddedServer;
import org.teiid.translator.jdbc.h2.H2ExecutionFactory;

import java.sql.Driver;
import javax.sql.DataSource;

/**
 * A bean that manages the creation of the global Teiid {@code EmbeddedServer}.
 */
public class TeiidServer {

    private EmbeddedServer server;

    /**
     * Initializes the Teiid embedded server.
     *
     * @throws Exception if the creation fails
     */
    public void init() throws Exception {

        // Create the configuration and start the server
        EmbeddedConfiguration ec = new EmbeddedConfiguration();
        ec.setUseDisk(false);
        server = new EmbeddedServer();
        server.start(ec);

        // Initialize the H2 execution factory
        H2ExecutionFactory ef = new H2ExecutionFactory();
        ef.setSupportsDirectQueryProcedure(true);
        ef.start();
        server.addTranslator("translator-h2", ef);

        // Create the first datasource
        DataSource ds1 = createDatasource("org.h2.Driver", "jdbc:h2:mem:db1", "sa", "");
        server.addConnectionFactory("java:/ext-ds1", ds1);
        ModelMetaData mmd1 = new ModelMetaData();
        mmd1.setName("schema1");
        mmd1.addSourceMapping("schema1", "translator-h2", "java:/ext-ds1");

        // Create the second datasource
        DataSource ds2 = createDatasource("org.h2.Driver", "jdbc:h2:mem:db2", "sa", "");
        server.addConnectionFactory("java:/ext-ds2", ds2);
        ModelMetaData mmd2 = new ModelMetaData();
        mmd2.setName("schema2");
        mmd2.addSourceMapping("schema2", "translator-h2", "java:/ext-ds2");

        // Create the virtual model
        ModelMetaData mmdv = new ModelMetaData();
        mmdv.setName("virt");
        mmdv.setModelType(Model.Type.VIRTUAL);
        mmdv.addSourceMetadata("ddl", "create view \"myinventory\" OPTIONS (UPDATABLE 'false') as select * from \"inventory\"");
        mmdv.addSourceMetadata("ddl", "create view \"mysales\" OPTIONS (UPDATABLE 'false') as select * from \"sales\"");

        // Deploy the virtual database with name 'virtualdb'
        server.deployVDB("virtualdb", mmd1, mmd2, mmdv);
    }

    /**
     * Factory method for the Teiid 'virtualdb' data-source.
     *
     * @return a datasource associated with the teiid virtual database
     * @throws Exception if the connection has problems
     */
    public DataSource getDatasource() throws Exception {
        Driver driver = server.getDriver();
        return createDatasource(driver, "jdbc:teiid:virtualdb", null, null);
    }

    private DataSource createDatasource(String driver, String url, String user, String password) throws Exception {
        return this.createDatasource((Driver) Class.forName(driver).newInstance(), url, user, password);
    }

    private DataSource createDatasource(Driver driver, String url, String user, String password) {
        SimpleDriverDataSource ds = new SimpleDriverDataSource();
        ds.setDriver(driver);
        ds.setUrl(url);
        ds.setUsername(user);
        ds.setPassword(password);

        return ds;
    }

}
