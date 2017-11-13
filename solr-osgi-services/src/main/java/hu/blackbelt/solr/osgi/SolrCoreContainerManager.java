package hu.blackbelt.solr.osgi;

import com.codahale.metrics.jvm.ClassLoadingGaugeSet;
import com.codahale.metrics.jvm.GarbageCollectorMetricSet;
import com.codahale.metrics.jvm.MemoryUsageGaugeSet;
import com.codahale.metrics.jvm.ThreadStatesGaugeSet;
import com.google.common.base.Charsets;
import com.google.common.base.Strings;
import com.google.common.jimfs.Configuration;
import com.google.common.jimfs.Jimfs;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileCleaningTracker;
import org.apache.http.client.HttpClient;
import org.apache.lucene.util.Version;
import org.apache.solr.common.SolrException;
import org.apache.solr.common.cloud.SolrZkClient;
import org.apache.solr.common.util.ExecutorUtil;
import org.apache.solr.core.CoreContainer;
import org.apache.solr.core.NodeConfig;
import org.apache.solr.core.SolrCore;
import org.apache.solr.core.SolrInfoBean;
import org.apache.solr.core.SolrResourceLoader;
import org.apache.solr.core.SolrXmlConfig;
import org.apache.solr.metrics.AltBufferPoolMetricSet;
import org.apache.solr.metrics.MetricsMap;
import org.apache.solr.metrics.OperatingSystemMetricSet;
import org.apache.solr.metrics.SolrMetricManager;
import org.apache.solr.request.SolrRequestInfo;
import org.apache.solr.servlet.SolrRequestParsers;
import org.apache.solr.util.SolrFileCleaningTracker;
import org.apache.solr.util.configuration.SSLConfigurationsFactory;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.metatype.annotations.Designate;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.CountDownLatch;


/**
 * Manages solr core solrCoreContainerManager. CoreContainer is a class, to be able to reference it have to register
 * direcly with the implementation class. The solrHome is centralized, so it is uses the solrHome defined here
 */
@Component(name = "SolrCoreContainer", immediate = true, configurationPolicy = ConfigurationPolicy.REQUIRE )
@Designate(ocd = SolrCoreContainerConfig.class)
@Slf4j
public class SolrCoreContainerManager {

    public static final String SOLR_XML = "/solr.xml";

    private SolrCoreContainerConfig config;
    private CoreContainer cores;

    FileSystem fs;

    private ServiceRegistration coreContainerRegistration;

    private HttpClient httpClient;
    protected final CountDownLatch init = new CountDownLatch(1);


    @SuppressWarnings({"checkstyle:illegalcatch", "checkstyle:executablestatementcount", "checkstyle:methodlength",
            "checkstyle:avoidinlineconditionals"})
    public void init() {
        // TODO: Define in OWGi context
        SSLConfigurationsFactory.current().init();
        log.trace("Solr.init(): {}", this.getClass().getClassLoader());
        CoreContainer coresInit = null;
        try {
            SolrRequestParsers.fileCleaningTracker = new SolrFileCleaningTracker();

            logWelcomeBanner();
            try {
                Properties extraProperties = new java.util.Properties();

                ExecutorUtil.addThreadLocalProvider(SolrRequestInfo.getInheritableThreadLocalProvider());

                coresInit = createCoreContainer(Strings.isNullOrEmpty(config.solrHome()) ? fs.getPath("") : Paths.get(config.solrHome()),
                        extraProperties);
                this.httpClient = coresInit.getUpdateShardHandler().getHttpClient();
                setupJvmMetrics(coresInit);
                log.debug("user.dir=" + System.getProperty("user.dir"));
            } catch (Throwable t) {
                log.error("Could not start Solr. Check solr/home property and the logs");
                SolrCore.log(t);
                if (t instanceof Error) {
                    throw (Error) t;
                }
            }

        } finally {
            log.trace("Solr.init() done");
            this.cores = coresInit;
            init.countDown();
        }
    }


    @Activate
    protected void activate(SolrCoreContainerConfig config, BundleContext bundleContext) throws IOException {
        fs = Jimfs.newFileSystem(Configuration.unix());
        this.config = config;
        init();
        registerSolrServiceReference(CoreContainer.class, bundleContext, cores);
    }

    @Deactivate
    @SuppressWarnings("checkstyle:illegalcatch")
    protected void deactivate() {
        if (coreContainerRegistration != null) {
            coreContainerRegistration.unregister();
        }

        try {
            FileCleaningTracker fileCleaningTracker = SolrRequestParsers.fileCleaningTracker;
            if (fileCleaningTracker != null) {
                fileCleaningTracker.exitWhenFinished();
            }
        } catch (Exception e) {
            log.warn("Exception closing FileCleaningTracker", e);
        } finally {
            SolrRequestParsers.fileCleaningTracker = null;
        }

        if (cores != null) {
            try {
                cores.shutdown();
            } finally {
                cores = null;
            }
        }

        try {
            fs.close();
        } catch (IOException e) {
            log.warn("Exception closing FileSystem close", e);
        }
    }
    /**
     * Register Solr service related to this config.
     * @param clazz
     * @param bundleContext
     * @param service
     * @return
     */
    private ServiceRegistration registerSolrServiceReference(Class clazz, BundleContext bundleContext, Object service) {
        Dictionary<String, Object> props = new Hashtable<>();
        return bundleContext.registerService(clazz.getName(), service, props);
    }



    @SuppressWarnings("checkstyle:illegalcatch")
    private void setupJvmMetrics(CoreContainer coresInit)  {
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

    @SuppressWarnings("checkstyle:avoidinlineconditionals")
    private void logWelcomeBanner() {
        log.info(" ___      _       Welcome to Apache Solr™ version {}", solrVersion());
        log.info("/ __| ___| |_ _   Starting in {} mode", isCloudMode() ? "cloud" : "standalone");
        log.info("\\__ \\/ _ \\ | '_| ");
        log.info("|___/\\___/_|_|    Start time: {}", Instant.now().toString());
    }


    @SuppressWarnings({"checkstyle:illegalcatch", "checkstyle:avoidinlineconditionals"})
    private String solrVersion() {
        String specVer = Version.LATEST.toString();
        try {
            String implVer = SolrCore.class.getPackage().getImplementationVersion();
            return (specVer.equals(implVer.split(" ")[0])) ? specVer : implVer;
        } catch (Exception e) {
            return specVer;
        }
    }

    /* We are in cloud mode if Java option zkRun exists OR zkHost exists and is non-empty */
    private boolean isCloudMode() {
        return (!Strings.isNullOrEmpty(config.zkHost()) || !Strings.isNullOrEmpty(config.zkRun()));
    }

    /**
     * Override this to change CoreContainer initialization.
     * @return a CoreContainer to hold this server's cores
     */
    protected CoreContainer createCoreContainer(Path solrHome, java.util.Properties extraProperties) {
        NodeConfig nodeConfig = loadNodeConfig(solrHome, extraProperties);
        final CoreContainer coreContainer = new CoreContainer(nodeConfig, extraProperties, true);
        coreContainer.load();
        return coreContainer;
    }

    /**
     * Get the NodeConfig whether stored on disk, in ZooKeeper, etc.
     * This may also be used by custom filters to load relevant configuration.
     * @return the NodeConfig
     */
    @SuppressWarnings("checkstyle:illegalcatch")
    public NodeConfig loadNodeConfig(Path solrHome, java.util.Properties nodeProperties) {

        SolrResourceLoader loader = new SolrResourceLoader(solrHome, SolrCoreContainerManager.class.getClassLoader(), nodeProperties);

        String zkHost = config.zkHost();
        if (!Strings.isNullOrEmpty(zkHost)) {
            try (SolrZkClient zkClient = new SolrZkClient(zkHost, config.zkClientTimeout())) {
                if (zkClient.exists(SOLR_XML, true)) {
                    log.info("solr.xml found in ZooKeeper. Loading...");
                    byte[] data = zkClient.getData(SOLR_XML, null, null, true);
                    return SolrXmlConfig.fromInputStream(loader, new ByteArrayInputStream(data));
                }
            } catch (Exception e) {
                throw new SolrException(SolrException.ErrorCode.SERVER_ERROR, "Error occurred while loading solr.xml from zookeeper", e);
            }
            log.info("Loading solr.xml from SolrHome (not found in ZooKeeper)");
        }
        // Using the templated version
        Path solrXml = loader.getInstancePath().resolve(SolrXmlConfig.SOLR_XML_FILE);
        String solrConfig = SolrXmlGenerator.getSolrXml(config);
        log.info("Solr config used: \n" + solrConfig);
        try {
            Files.createDirectories(solrXml.getParent());
            Files.write(solrXml, solrConfig.getBytes(Charsets.UTF_8), StandardOpenOption.CREATE);
        } catch (IOException e) {
            log.error("Could not create directories: " + solrXml.getParent().toString());
        }
        return SolrXmlConfig.fromSolrHome(loader, loader.getInstancePath());
    }

     public CoreContainer getCores() {
        return cores;
    }
}
