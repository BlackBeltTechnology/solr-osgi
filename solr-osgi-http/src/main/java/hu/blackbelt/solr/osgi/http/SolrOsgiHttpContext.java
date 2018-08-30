package hu.blackbelt.solr.osgi.http;


import lombok.extern.slf4j.Slf4j;
import org.osgi.framework.BundleContext;
import org.osgi.service.http.HttpContext;
import org.osgi.service.http.context.ServletContextHelper;

import javax.activation.MimetypesFileTypeMap;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URL;


@Slf4j
public class SolrOsgiHttpContext extends ServletContextHelper implements HttpContext {

    public final static String NAME="solr";

    private MimetypesFileTypeMap mimetypesFileTypeMap;

    private BundleContext bundleContext;

    public SolrOsgiHttpContext(BundleContext bundleContext) {
        this.bundleContext = bundleContext;
        ClassLoader old = Thread.currentThread().getContextClassLoader();
        Thread.currentThread().setContextClassLoader(this.getClass().getClassLoader());
        mimetypesFileTypeMap = new MimetypesFileTypeMap();
        Thread.currentThread().setContextClassLoader(old);
    }


    @Override
    public boolean handleSecurity(HttpServletRequest request,
                                  HttpServletResponse response) throws IOException {
        return true; // TODO support security
    }

    @Override
    public URL getResource(String name) {
        String actualName = name;
        if (actualName == null || actualName.equalsIgnoreCase("") || actualName.equals("/")) {
            actualName = "/index.html";
        }
        return bundleContext.getBundle().getResource(actualName);
    }

    @Override
    public String getMimeType(String name) {
        /* bunlde: resulution have to be supported by nio to work
        try {
            return Files.probeContentType(Paths.get(getResource(name).toURI()));
        } catch (IOException | URISyntaxException e) {
            log.error("Could not determinate mime type for: " + name, e);
        } */
        String actualName = name;
        if (actualName.equals("/")) {
            actualName = "/index.html";
        }

        return mimetypesFileTypeMap.getContentType(actualName);

    }
}
