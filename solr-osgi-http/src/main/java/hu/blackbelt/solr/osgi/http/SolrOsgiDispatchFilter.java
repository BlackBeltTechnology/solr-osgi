package hu.blackbelt.solr.osgi.http;

/*-
 * #%L
 * Solr OSGi HTTP
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

    private ArrayList<Pattern> excludePatterns;

    public SolrOsgiDispatchFilter(CoreContainer coreContainer, String exclude) {
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
