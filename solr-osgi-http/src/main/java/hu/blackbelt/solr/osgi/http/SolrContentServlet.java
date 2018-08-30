package hu.blackbelt.solr.osgi.http;

import org.apache.commons.io.IOUtils;
import org.apache.commons.io.output.CloseShieldOutputStream;

import javax.activation.MimetypesFileTypeMap;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class SolrContentServlet extends HttpServlet {

    private MimetypesFileTypeMap mimetypesFileTypeMap;

    public SolrContentServlet() {
        ClassLoader old = Thread.currentThread().getContextClassLoader();
        Thread.currentThread().setContextClassLoader(this.getClass().getClassLoader());
        mimetypesFileTypeMap = new MimetypesFileTypeMap();
        Thread.currentThread().setContextClassLoader(old);
    }

    @Override
    public void doGet(HttpServletRequest request,
                      HttpServletResponse response)
            throws IOException {

        response.addHeader("X-Frame-Options", "DENY"); // security: SOLR-7966 - avoid clickjacking for admin interface

        // This attribute is set by the SolrDispatchFilter
        String admin = request.getRequestURI().substring(request.getContextPath().length());
        if (admin == null || admin.equalsIgnoreCase("") || admin.equals("/")) {
            admin = "/index.html";
        }

        InputStream in = getServletContext().getResourceAsStream(admin);
        OutputStream out = new CloseShieldOutputStream(response.getOutputStream());

        if (in != null) {
            try {
                response.setCharacterEncoding("UTF-8");
                // response.setContentType(mimetypesFileTypeMap.getContentType(admin));
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
                } else if (admin.endsWith(".js")) {
                    response.setContentType("application/javascript");
                } else if (admin.endsWith(".svg")) {
                    response.setContentType("image/svg+xml");
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
