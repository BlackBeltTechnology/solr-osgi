package hu.blackbelt.solr.osgi;

/*-
 * #%L
 * Solr OSGi services
 * %%
 * Copyright (C) 2018 - 2023 BlackBelt Technology
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */


import com.google.common.jimfs.Configuration;
import com.google.common.jimfs.Jimfs;
import io.leangen.geantyref.TypeFactory;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocumentList;
import org.apache.solr.common.SolrInputDocument;
import org.apache.solr.common.params.CoreAdminParams;
import org.apache.solr.core.CoreContainer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;

import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.fail;

@Slf4j
public class OsgiSolrFactoryTest {

    static final String CONFIGSET_DIR = "target/configsets";

    static SolrClient solrClient1;
    static SolrClient solrClient2;

    public static void copyFolder(Path src, Path dest) throws IOException {
        Files.walk(src).forEach(source -> {
            if (!src.relativize(source).toString().equals("")) {
                copy(source, dest.resolve(src.relativize(source).toString()));
                log.info(source.toString() + " -> " + src.relativize(source));
            }
        });
    }

    private static void copy(Path source, Path dest) {
        try {
            Files.copy(source, dest, REPLACE_EXISTING);
        } catch (Exception e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    @BeforeAll
    public static void setupClass() {
        try {
            String targetLocation = OsgiSolrFactory.class
                    .getProtectionDomain().getCodeSource().getLocation().getFile() + "/..";

            log.info(targetLocation.toString());

            String solrHome = targetLocation + "/solr";

            final File solrHomeDir = new File(solrHome);
            if (solrHomeDir.exists()) {
                FileUtils.deleteDirectory(solrHomeDir);
                solrHomeDir.mkdirs();
            } else {
                solrHomeDir.mkdirs();
            }

            FileSystem containerInstancecFileSystem = Jimfs.newFileSystem(Configuration.unix());
            Map<String, Object> annotationParameters = new HashMap<>();
            annotationParameters.put("configSetBaseDir", "configsets");
            annotationParameters.put("solrHome", solrHome);

            SolrCoreContainerConfig solrCoreContainerConfig = TypeFactory.annotation(SolrCoreContainerConfig.class, annotationParameters);


            // final Path configSetPath = Paths.get(CONFIGSET_DIR).toAbsolutePath();
            final Path configSetPath = containerInstancecFileSystem.getPath(solrCoreContainerConfig.configSetBaseDir());
            Files.createDirectory(configSetPath);

            copyFolder(Paths.get(CONFIGSET_DIR).toAbsolutePath(), configSetPath);

            // solrClient = SolrUtil.create(solrCoreContainerConfig, solrHomeDir.toPath(), configSetPath, "exampleCollection", "test-1");
            // return createServer(createCoreContainer(solrCoreContainerConfig, solrPath, configSetPath), configSetName, coreName);
            CoreContainer coreContainer = OsgiSolrFactory.createCoreContainer(solrCoreContainerConfig, containerInstancecFileSystem.getPath(""));
            coreContainer.waitForLoadingCoresToFinish(5000);

            Dictionary<String, Object> params = new Hashtable<>();
            params.put(CoreAdminParams.PROPERTY_PREFIX + "test_par_1", "test1");

            solrClient1 = OsgiSolrFactory.createServer(coreContainer, "exampleCollection", "test-1", params);
            solrClient2 = OsgiSolrFactory.createServer(coreContainer, "exampleCollection", "test-2", params);

            // create some test documents
            SolrInputDocument doc1 = new SolrInputDocument();
            doc1.addField("id", "1");

            SolrInputDocument doc2 = new SolrInputDocument();
            doc2.addField("id", "2");

            SolrInputDocument doc3 = new SolrInputDocument();
            doc3.addField("id", "3");

            SolrInputDocument doc4 = new SolrInputDocument();
            doc4.addField("id", "4");

            SolrInputDocument doc5 = new SolrInputDocument();
            doc5.addField("id", "5");

            // add the test data to the index
            solrClient1.add(Arrays.asList(doc1, doc2, doc3, doc4, doc5));
            solrClient1.commit();
            solrClient2.add(Arrays.asList(doc1, doc2));
            solrClient2.commit();
        } catch (Exception e) {
            log.error("Error", e);
            fail(e.getMessage());
        }
    }

    @AfterAll
    public static void teardownClass() {
        try {
            solrClient1.close();
            solrClient2.close();
        } catch (Exception e) {
        }
    }

    @Test
    public void testEmbeddedSolrServerFactory() throws IOException, SolrServerException {
        SolrQuery solrQuery = new SolrQuery("*:*");
        QueryResponse response = solrClient1.query(solrQuery);
        assertNotNull(response);

        SolrDocumentList solrDocuments = response.getResults();
        assertNotNull(solrDocuments);
        assertEquals(5, solrDocuments.getNumFound());


        response = solrClient2.query(solrQuery);
        assertNotNull(response);

        solrDocuments = response.getResults();
        assertNotNull(solrDocuments);
        assertEquals(2, solrDocuments.getNumFound());

    }
}
