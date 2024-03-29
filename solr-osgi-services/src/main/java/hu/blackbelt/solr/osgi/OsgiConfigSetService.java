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

import org.apache.solr.common.SolrException;
import org.apache.solr.core.ConfigSetService;
import org.apache.solr.core.CoreDescriptor;
import org.apache.solr.core.SolrResourceLoader;

import java.nio.file.Files;
import java.nio.file.Path;

/**
 * The OSGi ConfigSetService.
 *
 * Loads a ConfigSet defined by the core's configSet property,
 * looking for a directory named for the configSet property value underneath
 * a base directory.  If no configSet property is set, loads the ConfigSet
 * instead from the core's instance directory.
 */
public class OsgiConfigSetService extends ConfigSetService {

    private final Path instancePath;

    /**
     * Create a new ConfigSetService.Default
     * @param loader the CoreContainer's resource loader
     * @param instancePath the base directory of core
     */
    public OsgiConfigSetService(SolrResourceLoader loader, Path instancePath) {
        super(loader);
        this.instancePath = instancePath;
    }

    @Override
    public SolrResourceLoader createCoreResourceLoader(CoreDescriptor cd) {
        Path instanceDir = locateInstanceDir(cd);
        return new SolrResourceLoader(instanceDir, parentLoader.getClassLoader(), cd.getSubstitutableProperties());
    }

    @Override
    public String configName(CoreDescriptor cd) {
        return (cd.getConfigSet() == null ? "instancedir " : "configset ") + locateInstanceDir(cd);
    }

    protected Path locateInstanceDir(CoreDescriptor cd) {
        String configSet = cd.getConfigSet();
        if (configSet == null)
            return cd.getInstanceDir();
        Path configSetDirectory = instancePath.resolve(configSet);
        if (!Files.isDirectory(configSetDirectory))
            throw new SolrException(SolrException.ErrorCode.SERVER_ERROR,
                    "Could not load configuration from directory " + configSetDirectory);
        return configSetDirectory;
    }

}
