package hu.blackbelt.solr.osgi.http;

import org.apache.commons.io.IOUtils;
import org.apache.commons.io.output.CloseShieldOutputStream;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class SolrContentServlet extends HttpServlet {

    @Override
    public void doGet(HttpServletRequest request,
                      HttpServletResponse response)
            throws IOException {

        response.addHeader("X-Frame-Options", "DENY"); // security: SOLR-7966 - avoid clickjacking for admin interface

        // This attribute is set by the SolrDispatchFilter
        String admin = request.getRequestURI().substring(request.getContextPath().length());

        InputStream in = getServletContext().getResourceAsStream(admin);
        OutputStream out = new CloseShieldOutputStream(response.getOutputStream());

        if (in != null) {
            try {
                    response.setCharacterEncoding("UTF-8");
                    if (admin.endsWith(".css")) {
                        response.setContentType("text/css");
                    } else if (admin.endsWith(".html")) {
                        response.setContentType("text/html");
                    } else if (admin.endsWith(".png")) {
                        response.setContentType("image/png");
                    } else if (admin.endsWith(".gif")) {
                        response.setContentType("image/gif");
                    } else if (admin.endsWith(".swf")) {
                        response.setContentType("application/x-shockwave-flash");
                    } else if (admin.endsWith(".swf")) {
                        response.setContentType("application/javascript");
                    } else {
                        response.setContentType("application/octet-stream");
                    }

                    IOUtils.copy(in, out);
            } finally {
                IOUtils.closeQuietly(in);
                IOUtils.closeQuietly(out);
            }
        } else {
            response.sendError(404);
        }
    }
}
