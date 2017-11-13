package hu.blackbelt.solr.osgi.http;

import org.apache.solr.core.CoreContainer;
import org.apache.solr.servlet.SolrDispatchFilter;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SolrOsgiDispatchFilter extends SolrDispatchFilter implements Filter {

    final CoreContainer coreContainer;
    private ArrayList<Pattern> excludePatterns;

    public SolrOsgiDispatchFilter(CoreContainer coreContainer, String exclude) {
        this.coreContainer = coreContainer;
        super.cores = coreContainer;

        if(exclude != null) {
            String[] excludeArray = exclude.split(",");
            excludePatterns = new ArrayList<>();
            for (String element : excludeArray) {
                excludePatterns.add(Pattern.compile(element));
            }
        }
        super.init.countDown();
    }

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {

    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        // No need to even create the HttpSolrCall object if this path is excluded.
        if (excludePatterns != null) {
            String requestPath = ((HttpServletRequest) request).getServletPath();
            String extraPath = ((HttpServletRequest) request).getPathInfo();
            if (extraPath != null) { // In embedded mode, servlet path is empty - include all post-context path here for
                // testing
                requestPath += extraPath;
            }
            for (Pattern p : excludePatterns) {
                Matcher matcher = p.matcher(requestPath);
                if (matcher.lookingAt()) {
                    chain.doFilter(request, response);
                    return;
                }
            }
        }

        super.doFilter(request, response, chain);
    }

    @Override
    public void destroy() {

    }
}
