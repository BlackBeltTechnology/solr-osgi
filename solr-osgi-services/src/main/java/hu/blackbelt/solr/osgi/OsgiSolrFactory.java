package hu.blackbelt.solr.osgi;

import com.codahale.metrics.jvm.ClassLoadingGaugeSet;
import com.codahale.metrics.jvm.GarbageCollectorMetricSet;
import com.codahale.metrics.jvm.MemoryUsageGaugeSet;
import com.codahale.metrics.jvm.ThreadStatesGaugeSet;
import hu.blackbelt.osgi.utils.osgi.api.PropertiesUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.reflect.FieldUtils;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrRequest;
import org.apache.solr.client.solrj.SolrResponse;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.embedded.EmbeddedSolrServer;
import org.apache.solr.client.solrj.request.CoreAdminRequest;
import org.apache.solr.client.solrj.response.CoreAdminResponse;
import org.apache.solr.common.params.ModifiableSolrParams;
import org.apache.solr.common.params.SolrParams;
import org.apache.solr.common.util.ContentStream;
import org.apache.solr.core.ConfigSetService;
import org.apache.solr.core.CoreContainer;
import org.apache.solr.core.CorePropertiesLocator;
import org.apache.solr.core.NodeConfig;
import org.apache.solr.core.SolrInfoBean;
import org.apache.solr.core.SolrResourceLoader;
import org.apache.solr.core.SolrXmlConfig;
import org.apache.solr.metrics.AltBufferPoolMetricSet;
import org.apache.solr.metrics.MetricsMap;
import org.apache.solr.metrics.OperatingSystemMetricSet;
import org.apache.solr.metrics.SolrMetricManager;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.Properties;
import java.util.Set;

@Slf4j
public class OsgiSolrFactory {

    /**
     * Create a core and returns a client to handle it.
     *
     * @param configSetName The configset name which used to template the core
     * @param coreName the name of the core, must have a matching directory in configHome
     * @param properties The core.properties. https://lucene.apache.org/solr/guide/7_0/coreadmin-api.html
     *
     * @return an EmbeddedSolrServer with a core created for the given coreName
     * @throws IOException


     */
    public static SolrClient createServer(final CoreContainer coreContainer, final String configSetName, final String coreName, Dictionary<String, Object> properties)
            throws IOException, SolrServerException {


        EmbeddedSolrServer embeddedSolrServer = new EmbeddedSolrServer(coreContainer, coreName);

        /* The SolrJ Create does not have way to give parameters to core */
        final CoreAdminRequest.Create createRequest = new CoreAdminRequest.Create();
        createRequest.setCoreName(coreName);
        createRequest.setConfigSet(configSetName);
        createRequest.setInstanceDir(new File(coreContainer.getSolrHome()).toPath().resolve(coreName).toAbsolutePath().toString());

        // Extend the request
        SolrRequest req = new SolrRequest(createRequest.getMethod(), createRequest.getPath()) {
            @Override
            public SolrParams getParams() {
                ModifiableSolrParams params = (ModifiableSolrParams) createRequest.getParams();
                Enumeration<String> e = properties.keys();
                while (e.hasMoreElements()) {
                    String key = e.nextElement();
                    params.set(key, PropertiesUtil.toString(properties.get(key), null));
                }
                return params;
            }

            @Override
            public Collection<ContentStream> getContentStreams() throws IOException {
                return createRequest.getContentStreams();
            }

            @Override
            protected SolrResponse createResponse(SolrClient client) {
                return new CoreAdminResponse();
            }
        };

        embeddedSolrServer.request(req);

        return embeddedSolrServer;
    }



    /**
     * @param coreName the name of the core, must have a matching directory in configHome
     *
     * @return an EmbeddedSolrServer with a core created for the given coreName
     * @throws IOException
     */
    public static SolrClient getServer(final CoreContainer coreContainer, final String coreName)
            throws IOException, SolrServerException {


        EmbeddedSolrServer embeddedSolrServer = new EmbeddedSolrServer(coreContainer, coreName);
        return embeddedSolrServer;
    }

    /**
     * @param solrCoreContainerConfig the Solr home directory to use
     * @param solrInstancePath the directory container instance
     *
     * @return an CoreContainer which are prepared to create cores
     * @throws IOException
     */
    public static CoreContainer createCoreContainer(final SolrCoreContainerConfig solrCoreContainerConfig, final Path solrInstancePath)
            throws IOException, SolrServerException {


        final SolrResourceLoader loader = new OsgiSolrResourceLoader(solrInstancePath);
        NodeConfig config = SolrXmlConfig.fromString(loader, SolrXmlGenerator.getSolrXml(solrCoreContainerConfig));
        CoreContainer coreContainer = new CoreContainer(config, new Properties(), new CorePropertiesLocator(config.getCoreRootDirectory()), true);
        coreContainer.load();

        // Hack: To be able to control resourcec loading have to replace core container's configset loader implementation.
        // It is mandatory to be able to use osgi compliant resource loader.
        // TODO: Change the behaviour to work with Zookeper's clustered instances
        ConfigSetService configSetService = new OsgiConfigSetService(new OsgiSolrResourceLoader(solrInstancePath), solrInstancePath.resolve(solrCoreContainerConfig.configSetBaseDir()));
        try {
            FieldUtils.writeField(coreContainer, "coreConfigService", configSetService,  true);
            FieldUtils.writeField(coreContainer, "solrHome", solrCoreContainerConfig.solrHome(),  true);

        } catch (IllegalAccessException e) {
            log.error("Could not set coreConfigService");
        }

        setupJvmMetrics(coreContainer);
        return coreContainer;
    }


    @SuppressWarnings("checkstyle:illegalcatch")
    public static void setupJvmMetrics(CoreContainer coresInit)  {
        SolrMetricManager metricManager = coresInit.getMetricManager();
        final Set<String> hiddenSysProps = coresInit.getConfig().getMetricsConfig().getHiddenSysProps();
        try {
            String registry = SolrMetricManager.getRegistryName(SolrInfoBean.Group.jvm);
            metricManager.registerAll(registry, new AltBufferPoolMetricSet(), true, "buffers");
            metricManager.registerAll(registry, new ClassLoadingGaugeSet(), true, "classes");
            metricManager.registerAll(registry, new OperatingSystemMetricSet(), true, "os");
            metricManager.registerAll(registry, new GarbageCollectorMetricSet(), true, "gc");
            metricManager.registerAll(registry, new MemoryUsageGaugeSet(), true, "memory");
            metricManager.registerAll(registry, new ThreadStatesGaugeSet(), true, "threads");
            MetricsMap sysprops = new MetricsMap((detailed, map) -> {
                System.getProperties().forEach((k, v) -> {
                    if (!hiddenSysProps.contains(k)) {
                        map.put(String.valueOf(k), v);
                    }
                });
            });
            metricManager.registerGauge(null, registry, sysprops, true, "properties", "system");
        } catch (Exception e) {
            log.warn("Error registering JVM metrics", e);
        }
    }

}
