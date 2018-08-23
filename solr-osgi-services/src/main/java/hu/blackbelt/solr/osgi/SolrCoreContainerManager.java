package hu.blackbelt.solr.osgi;

import com.google.common.base.Strings;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.common.io.ByteStreams;
import com.google.common.jimfs.Jimfs;
import hu.blackbelt.osgi.utils.osgi.api.BundleCallback;
import hu.blackbelt.osgi.utils.osgi.api.BundleTrackerManager;
import hu.blackbelt.osgi.utils.osgi.api.ConfigurationCallback;
import hu.blackbelt.osgi.utils.osgi.api.ConfigurationInfo;
import hu.blackbelt.osgi.utils.osgi.api.ConfigurationTrackerManager;
import hu.blackbelt.osgi.utils.osgi.api.PropertiesUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileCleaningTracker;
import org.apache.lucene.util.Version;
import org.apache.solr.common.util.ExecutorUtil;
import org.apache.solr.core.CoreContainer;
import org.apache.solr.core.SolrCore;
import org.apache.solr.request.SolrRequestInfo;
import org.apache.solr.servlet.SolrRequestParsers;
import org.apache.solr.util.SolrFileCleaningTracker;
import org.apache.solr.util.configuration.SSLConfigurationsFactory;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.metatype.annotations.Designate;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Comparator;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.function.Consumer;
import java.util.function.Predicate;

import static com.google.common.collect.Iterators.forEnumeration;


/**
 * Manages solr core solrCoreContainerManager. CoreContainer is a class, to be able to reference it have to register
 * direcly with the implementation class. The solrHome is centralized, so it is uses the solrHome defined here
 */
@Component(name = "solr.corecontainer", immediate = true, configurationPolicy = ConfigurationPolicy.REQUIRE)
@Designate(ocd = SolrCoreContainerConfig.class)
@Slf4j
public class SolrCoreContainerManager {

    static final String SOLR_CONIGSET_TAG = "Solr-Configset";
    static final String SOLR_CORE_CONFIGURATION_PID = "solr.core";
    static final String CONFIGURATION_CONFIGSET = "configSet";
    static final String CONFIGURATION_NAME = "name";
    static final String CONFIGSETS = "configsets";
    static final String SLASH = "/";

    private static final Predicate<Bundle> IS_SOLRCCONFIGSET = new Predicate<Bundle>() {
        @Override
        public boolean test(Bundle bundle) {
            return bundle.getHeaders().get(SOLR_CONIGSET_TAG) != null;
        }
    };

    @Reference
    BundleTrackerManager bundleTrackerManager;

    @Reference
    ConfigurationTrackerManager configurationTrackerManager;

    private SolrCoreContainerConfig config;
    private CoreContainer coreContainer;
    private FileSystem coreFileSystem;
    private ServiceRegistration coreContainerRegistration;

    private Set<String> loadedConfigSet = Sets.newConcurrentHashSet();
    private Map<String, ConfigurationInfo> allCoreConfigurations = Maps.newConcurrentMap();
    private Map<String, ConfigurationInfo> startedCoreConfigurations = Maps.newConcurrentMap();
    protected final CountDownLatch init = new CountDownLatch(1);



    @SuppressWarnings({"checkstyle:illegalcatch", "checkstyle:executablestatementcount", "checkstyle:methodlength",
            "checkstyle:avoidinlineconditionals"})
    public void init(SolrCoreContainerConfig config) throws Exception {
        SSLConfigurationsFactory.current().init();
        log.trace("Solr.init(): {}", this.getClass().getClassLoader());
        try {
            SolrRequestParsers.fileCleaningTracker = new SolrFileCleaningTracker();
            logWelcomeBanner();
            try {
                coreFileSystem = Jimfs.newFileSystem(com.google.common.jimfs.Configuration.unix());
                coreContainer = OsgiSolrFactory.createCoreContainer(config, coreFileSystem.getPath("/"));
                ExecutorUtil.addThreadLocalProvider(SolrRequestInfo.getInheritableThreadLocalProvider());
                log.debug("user.dir=" + System.getProperty("user.dir"));
            } catch (Exception e) {
                log.error("Could not start Solr. Check solr/home property and the logs");
                SolrCore.log(e);
                throw e;
            }

        } finally {
            log.trace("Solr.init() done");
            init.countDown();
        }
    }


    @Activate
    protected void activate(SolrCoreContainerConfig config, BundleContext bundleContext) throws Exception {
        this.config = config;
        try {
            init(config);
            coreContainerRegistration = registerSolrServiceReference(CoreContainer.class, bundleContext, coreContainer);

            bundleTrackerManager.registerBundleCallback("solrConfigSetTracker-" + this.toString(),
                    registerBundleCallback(),
                    unregisterBundleCallback(),
                    IS_SOLRCCONFIGSET);

            configurationTrackerManager.registerConfigurationCallback("solrConfigSetTracker-" + this.toString(),
                    createConfigurationCallback(),
                    updateConfigurationCallback(),
                    deleteConfigurationCallback(),
                    filterSolrCoreConfiguration()
            );
        } catch (Exception e) {
            throw e;
        }

    }

    @Deactivate
    @SuppressWarnings("checkstyle:illegalcatch")
    protected void deactivate() {
        bundleTrackerManager.unregisterBundleCallback("solrConfigSetTracker-" + this.toString());
        configurationTrackerManager.unregisterConfigurationCallback("solrConfigSetTracker-" + this.toString());

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

        if (coreContainer != null) {
            try {
                coreContainer.shutdown();
            } finally {
                coreContainer = null;
            }
        }

        try {
            coreFileSystem.close();
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

    private BundleCallback registerBundleCallback() {
        return new BundleCallback() {
            @Override
            public void accept(Bundle bundle) {
                String configSetName = bundle.getHeaders().get(SOLR_CONIGSET_TAG).toString();
                log.info("Solr content found in bundle " + bundle.getSymbolicName() + " Registering");
                try {
                    // Copy content from path
                    forEnumeration(bundle.findEntries(CONFIGSETS + SLASH + configSetName, "*", true)).forEachRemaining(url -> {
                        if (!url.toString().endsWith(SLASH)) {
                            java.net.URL url1;
                            try {
                                String relPath = SLASH + makePathRelative(configSetName, url.getPath());
                                log.info("Copy file " + url.toString() + " to " + relPath);
                                Path to = coreFileSystem.getPath(relPath);
                                Files.createDirectories(to.getParent());
                                ByteStreams.copy(url.openStream(), Files.newOutputStream(to));
                            } catch (Exception e) {
                                log.info("Could not copy file: " + url.toString());
                            }
                        }
                    });
                    loadedConfigSet.add(configSetName);
                    refreshCores();
                } catch (Exception e) {
                    log.warn(String.format("Could not load content from bundle: %s in path: %s ",
                            bundle.getSymbolicName(), configSetName), e);
                }
            }

            @Override
            public Thread process(Bundle bundle) {
                return null;
            }
        };
    }

    private BundleCallback unregisterBundleCallback() {
        return new BundleCallback() {
            @Override
            public void accept(Bundle bundle) {
                String configSetName = bundle.getHeaders().get(SOLR_CONIGSET_TAG).toString();
                log.info("Solr content found in bundle " + bundle.getSymbolicName() + " Unregistering");
                try {
                    // Remove from path
                    loadedConfigSet.remove(configSetName);
                    Files.walk(coreFileSystem.getPath(CONFIGSETS + SLASH + configSetName))
                            .sorted(Comparator.reverseOrder())
                            .map(Path::toFile)
                            .peek(f -> log.info("Deleting file: " + f.toString()))
                            .forEach(File::delete);

                    refreshCores();
                } catch (Exception e) {
                    log.warn(String.format("Could not load content from bundle: %s in path: %s ",
                            bundle.getSymbolicName(), configSetName), e);
                }
            }

            @Override
            public Thread process(Bundle bundle) {
                return null;
            }
        };
    }


    private ConfigurationCallback createConfigurationCallback() {
        return new ConfigurationCallback() {
            @Override
            public void accept(ConfigurationInfo configurationInfo) {
                log.info("Creating config: " + configurationInfo.toString());
                allCoreConfigurations.put(configurationInfo.getConfigurationPid(), configurationInfo);
                refreshCores();
            }

            @Override
            public Thread process(ConfigurationInfo configurationInfo) {
                return null;
            }
        };
    }

    private ConfigurationCallback updateConfigurationCallback() {
        return new ConfigurationCallback() {
            @Override
            public void accept(ConfigurationInfo configurationInfo) {
                log.info("Update of configuration is ignored: " + configurationInfo.getConfigurationFactoryPid());
            }

            @Override
            public Thread process(ConfigurationInfo configurationInfo) {
                return null;
            }
        };
    }

    private ConfigurationCallback deleteConfigurationCallback() {
        return new ConfigurationCallback() {
            @Override
            public void accept(ConfigurationInfo configurationInfo) {
                log.info("Removing config: " + configurationInfo.toString());
                allCoreConfigurations.remove(configurationInfo.getConfigurationPid());
                refreshCores();
            }

            @Override
            public Thread process(ConfigurationInfo configurationInfo) {
                return null;
            }
        };
    }

    private Predicate<ConfigurationInfo> filterSolrCoreConfiguration() {
        return new Predicate<ConfigurationInfo>() {
            @Override
            public boolean test(ConfigurationInfo configurationInfo) {
                return configurationInfo.getConfigurationFactoryPid().equals(SOLR_CORE_CONFIGURATION_PID)
                        && configurationInfo.getProperties().get(CONFIGURATION_CONFIGSET) != null
                        && configurationInfo.getProperties().get(CONFIGURATION_NAME) != null;
            }
        };
    }

    private Consumer<ConfigurationInfo> startCore() {
        return new Consumer<ConfigurationInfo>() {
            @Override
            public void accept(ConfigurationInfo configurationInfo) {
                startedCoreConfigurations.put(configurationInfo.getConfigurationPid(), configurationInfo);
                log.info("Starting core: " + configurationInfo.toString());
                try {
                    OsgiSolrFactory.createServer(coreContainer,
                            PropertiesUtil.toString(configurationInfo.getProperties().get(CONFIGURATION_CONFIGSET), null),
                            PropertiesUtil.toString(configurationInfo.getProperties().get(CONFIGURATION_NAME), null),
                            configurationInfo.getProperties());
                } catch (Exception e) {
                    log.error("Coukd not start core: " + configurationInfo.toString());
                }
            }
        };
    }

    private Consumer<ConfigurationInfo> stopCore() {
        return new Consumer<ConfigurationInfo>() {
            @Override
            public void accept(ConfigurationInfo configurationInfo) {
                startedCoreConfigurations.remove(configurationInfo.getConfigurationPid());
                log.info("Stopping core: " + configurationInfo.toString());
                try {
                    coreContainer.getCore(PropertiesUtil.toString(configurationInfo.getProperties().get(CONFIGURATION_NAME), null)).close();
                } catch (Exception e) {
                    log.error("Coukd not start core: " + configurationInfo.toString());
                }
            }
        };
    }

    private synchronized void refreshCores() {

        // All configurations which is not started but have configSet start
        allCoreConfigurations.entrySet().stream()
                .filter(c -> !startedCoreConfigurations.containsKey(c.getKey()))
                .filter(c -> loadedConfigSet.contains(c.getValue().getProperties().get(CONFIGURATION_CONFIGSET)))
                .map(c -> c.getValue())
                .forEach(startCore());

        // All configurations which is started but dont have configSet stops
        allCoreConfigurations.entrySet().stream()
                .filter(c -> startedCoreConfigurations.containsKey(c.getKey()))
                .filter(c -> !loadedConfigSet.contains(c.getValue().getProperties().get(CONFIGURATION_CONFIGSET)))
                .map(c -> c.getValue())
                .forEach(stopCore());

        // All started which configuration is removed from all confugurations
        startedCoreConfigurations.entrySet().stream()
                .filter(c -> !allCoreConfigurations.containsKey(c.getKey()))
                .map(c -> c.getValue())
                .forEach(stopCore());

        // All configurations which is not started but have configSet start
        allCoreConfigurations.entrySet().stream()
                .filter(c -> !startedCoreConfigurations.containsKey(c.getKey()))
                .filter(c -> !loadedConfigSet.contains(c.getValue().getProperties().get(CONFIGURATION_CONFIGSET)))
                .forEach(c -> log.warn("Could not start core, because configSet is not loaded: " + c.getValue().toString()));
    }


    /**
     * Making path relative
     * @param relativePath
     * @param path
     * @return
     */
    private String makePathRelative(String relativePath, String path) {
        path = path.replaceAll("//", SLASH);
        if (path.startsWith(SLASH))
            path =  path.substring(1);

        if (path.startsWith(relativePath)) {
            path = path.substring(relativePath.length());
        }
        return path;
    }

}
