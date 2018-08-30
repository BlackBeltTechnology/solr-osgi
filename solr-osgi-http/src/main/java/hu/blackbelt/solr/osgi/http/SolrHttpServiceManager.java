package hu.blackbelt.solr.osgi.http;

import lombok.extern.slf4j.Slf4j;
import org.apache.solr.core.CoreContainer;
import org.apache.solr.servlet.LoadAdminUiServlet;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.http.HttpService;
import org.osgi.service.http.context.ServletContextHelper;
import org.osgi.service.http.whiteboard.HttpWhiteboardConstants;
import org.osgi.service.metatype.annotations.Designate;
import org.restlet.ext.servlet.ServerServlet;

import javax.servlet.Filter;
import javax.servlet.Servlet;
import java.util.Dictionary;
import java.util.Hashtable;

@Component(name = "solr.http", immediate = true, configurationPolicy = ConfigurationPolicy.REQUIRE )
@Designate(ocd = SolrHttpConfig.class)
@Slf4j
public class SolrHttpServiceManager {

    @Reference
    HttpService httpService;

    @Reference
    CoreContainer coreContainer;

    ServiceRegistration solrHttpContextRegistration;
    ServiceRegistration dispatchFilterRegistration;
    ServiceRegistration loadAdminUiServletRegistration;
    ServiceRegistration solrRestApiServletRegistration;
    ServiceRegistration solrStaticResourceRegistration;

    @Activate
    public void activate(SolrHttpConfig solrHttpConfig, BundleContext bundleContext) {
        try {
            solrHttpContextRegistration = registerHttpContext(solrHttpConfig, bundleContext);
            solrStaticResourceRegistration = registerSolrStaticResource(solrHttpConfig, bundleContext);
            dispatchFilterRegistration = registerDispatchFilter(solrHttpConfig, bundleContext);
            loadAdminUiServletRegistration = registerLoadAdminUiServlet(solrHttpConfig, bundleContext);
            solrRestApiServletRegistration = registerSolrRestApiServlet(solrHttpConfig, bundleContext);

        } catch (Exception ex) {
            log.error("Could not register solr http services", ex);
        }
    }

    @Deactivate
    public void deactivate() {
        if (dispatchFilterRegistration != null) {
            dispatchFilterRegistration.unregister();
        }
        if (loadAdminUiServletRegistration != null) {
            loadAdminUiServletRegistration.unregister();
        }
        if (solrStaticResourceRegistration != null) {
            solrStaticResourceRegistration.unregister();
        }
        if (solrRestApiServletRegistration != null) {
            solrRestApiServletRegistration.unregister();
        }
        if (solrHttpContextRegistration != null) {
            solrHttpContextRegistration.unregister();
        }
    }


    private ServiceRegistration registerSolrStaticResource(SolrHttpConfig solrHttpConfig, BundleContext bundleContext) {
        SolrContentServlet solrContentServlet = new SolrContentServlet();
        Dictionary initParams = new Hashtable<>();
        initParams.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_SELECT,
                "(" + HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_NAME +"=" + SolrOsgiHttpContext.NAME + ")");
        initParams.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_PATTERN, "/");
        initParams.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_FILTER_NAME, "SolrContentServlet");
        return bundleContext.registerService(Servlet.class, solrContentServlet, initParams);
    }

    private ServiceRegistration registerHttpContext(SolrHttpConfig solrHttpConfig, BundleContext bundleContext) {
        ServletContextHelper solrHttpContext = new SolrOsgiHttpContext(bundleContext);

        String contextRoot = solrHttpConfig.contextRoot();

        Dictionary initParams = new Hashtable<>();
        initParams.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_NAME, SolrOsgiHttpContext.NAME );
        initParams.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_PATH, contextRoot);

        return bundleContext.registerService(ServletContextHelper.class, solrHttpContext, initParams);
    }

    private ServiceRegistration registerDispatchFilter(SolrHttpConfig solrHttpConfig, BundleContext bundleContext) {
        Dictionary initParams = new Hashtable<>();
        initParams.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_SELECT,
                "(" + HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_NAME +"=" + SolrOsgiHttpContext.NAME + ")");
        initParams.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_FILTER_PATTERN, "/*");
        initParams.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_FILTER_NAME, "SolrDispatchFilter");

        Filter solrDispatchFilter = new SolrOsgiDispatchFilter(coreContainer, solrHttpConfig.excludePatterns());
        return bundleContext.registerService(Filter.class, solrDispatchFilter, initParams);
    }

    private ServiceRegistration registerLoadAdminUiServlet(SolrHttpConfig solrHttpConfig, BundleContext bundleContext) {
        LoadAdminUiServlet loadAdminUiServlet = new LoadAdminUiServlet();
        Dictionary initParams = new Hashtable<>();
        initParams.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_SELECT,
                "(" + HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_NAME +"=" + SolrOsgiHttpContext.NAME + ")");
        initParams.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_PATTERN, "/index.html");
        initParams.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_FILTER_NAME, "LoadAdminUiServlet");
        return bundleContext.registerService(Servlet.class, loadAdminUiServlet, initParams);
    }

    private ServiceRegistration registerSolrRestApiServlet(SolrHttpConfig solrHttpConfig, BundleContext bundleContext) {
        ServerServlet serverServlet = new ServerServlet();
        Dictionary initParams = new Hashtable<>();
        initParams.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_SELECT,
                "(" + HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_NAME +"=" + SolrOsgiHttpContext.NAME + ")");
        initParams.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_PATTERN, "/schema/*");
        initParams.put("org.restlet.application", "org.apache.solr.rest.SolrSchemaRestApi");
        initParams.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_FILTER_NAME, "SolrSchemaRestApiServlet");
        return bundleContext.registerService(Servlet.class, serverServlet, initParams);
    }

}
