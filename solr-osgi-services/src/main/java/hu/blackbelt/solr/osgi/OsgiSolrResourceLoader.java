package hu.blackbelt.solr.osgi;

import lombok.extern.slf4j.Slf4j;
import org.apache.solr.core.SolrResourceLoader;

import java.nio.file.Path;
import java.util.Properties;

@Slf4j
public class OsgiSolrResourceLoader extends SolrResourceLoader {


    public OsgiSolrResourceLoader(Path instanceDir) {
        super(instanceDir);
    }

    @Override
    public <T> T newInstance(String cName, Class<T> expectedType, String [] subPackages, Class[] params, Object[] args) {
        log.info("NewInstance: " + cName + " Expected tyoe: " + expectedType.getName());
        return super.newInstance(cName, expectedType, subPackages, params, args);
    }

    @Override
    public Path getInstancePath() {
        Path insancePath = super.getInstancePath();
        log.info("Get instance path: " + insancePath);
        return insancePath;
    }

    public String[] listConfigDir() {
        throw new UnsupportedOperationException("Could not list configuration directories");
    }

    public String getConfigDir() {
        String configDir = getInstancePath().resolve("conf").toString();
        log.info("Get confDir: " + configDir);
        return configDir;
    }

    public String getDataDir()    {
        String dataDir  = super.getDataDir();
        log.info("Get dataDir: " + dataDir);
        return dataDir;
    }

    public Properties getCoreProperties() {
        return super.getCoreProperties();
    }

    public ClassLoader getClassLoader() {
        return super.getClassLoader();
    }

}
