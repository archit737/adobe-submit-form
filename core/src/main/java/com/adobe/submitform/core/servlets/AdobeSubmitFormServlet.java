package com.adobe.submitform.core.servlets;

import org.apache.http.client.utils.URIBuilder;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.PersistenceException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.servlets.HttpConstants;
import org.apache.sling.api.servlets.SlingSafeMethodsServlet;
import org.osgi.framework.Constants;
import org.osgi.service.component.annotations.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.ItemExistsException;
import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.lock.LockException;
import javax.jcr.nodetype.ConstraintViolationException;
import javax.jcr.nodetype.NoSuchNodeTypeException;
import javax.jcr.version.VersionException;
import javax.servlet.Servlet;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Enumeration;

/**
 * @author Archit Arora
 *
 * This servlet uses the HTTP GET method to read a data from the RESTful webservice
 */
@Component(service = Servlet.class, property = {
        Constants.SERVICE_DESCRIPTION + "=JSON Servlet to read the data from the external webservice",
        "sling.servlet.methods=" + HttpConstants.METHOD_GET, "sling.servlet.paths=" + "/bin/adobeSubmit" })
public class AdobeSubmitFormServlet extends SlingSafeMethodsServlet {

    /**
     * Generated serialVersionUID
     */
    private static final long serialVersionUID = 4438376868274173005L;
    /**
     * Logger
     */
    private static final Logger log = LoggerFactory.getLogger(AdobeSubmitFormServlet.class);

    @Override
    protected void doGet(SlingHttpServletRequest request, SlingHttpServletResponse response) {

        try {
            log.info("Reading the data from the webservice");
            PrintWriter out = response.getWriter();
            response.setContentType("text/plain");
            /**
             * Build base URI
             */
            URIBuilder builder = new URIBuilder();
            builder.setScheme("https").setHost("gorest.co.in").setPath("/public/v2/users");
            /**
             * Get query parameters and inject into the URI
             */
            Enumeration<String> parameterNames = request.getParameterNames();
            while (parameterNames.hasMoreElements()) {
                String paramName = parameterNames.nextElement();
                String[] paramValues = request.getParameterValues(paramName);
                for (int i = 0; i < paramValues.length; i++) {
                    String paramValue = paramValues[i];
                    builder.setParameter(paramName,paramValue);
                }
            }
            URI uri = builder.build();
            /**
             * Create final URL with appended query parameters
             */
            URL url = new URL(uri.toURL().toString());
            HttpURLConnection con = (HttpURLConnection) url.openConnection();
            con.setRequestMethod("GET");
            con.setRequestProperty("Content-Type", "application/json");
            /**
             * Set timeouts
             */
            con.setConnectTimeout(5000);
            con.setReadTimeout(5000);
            /**
             * Trigger the request and get a reponse
             */
            int status = con.getResponseCode();
            /**
             * Read the response
             */
            if(status ==200) {
                BufferedReader in = new BufferedReader(
                        new InputStreamReader(con.getInputStream()));
                String inputLine;
                StringBuffer content = new StringBuffer();
                while ((inputLine = in.readLine()) != null) {
                    content.append(inputLine);
                }
                in.close();
                /**
                 * Output the response on the page
                 */
                out.println("STATUS IS "+status);
                out.println("\n");
                out.println("URL is "+url);
                out.println("\n");
                out.println("Content returned is "+content.toString());
                out.println("\n");
                /**
                 * Save timestamp to JCR
                 */
                saveTimestampInJRC(request);
                out.println("Timestamp saved in JCR");
            }
            else {
                out.println("Something went wrong. Please try again");
            }
            out.close();
            con.disconnect();
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
    }
    /**
     * Method to persist metadata into JCR
     */
    private void saveTimestampInJRC(SlingHttpServletRequest request){

        try {
            /**
             * Getting the instance of resource resolver from the request
             */
            ResourceResolver resourceResolver = request.getResourceResolver();
            /**
             * Getting the resource object via path
             */
            Resource resource = resourceResolver.getResource("/content/submit-form/en");
            log.info("Resource is at path {}", resource.getPath());
            /**
             * Adapt the resource to javax.jcr.Node type
             */
            Node node = resource.adaptTo(Node.class);
            /**
             * Create a new node with name and primary type and add it below the path specified by the resource
             */
            Node newNode = node.addNode(getLocalDateTime(), "nt:unstructured");
            /**
             * Setting a name property for this node
             */
            newNode.setProperty("timestamp", getLocalDateTime());
            /**
             * Commit the changes to JCR
             */
            resourceResolver.commit();
        }
        catch (LockException e) {
            e.printStackTrace();
        }
        catch (ItemExistsException e) {
            e.printStackTrace();
        }
        catch (ConstraintViolationException e) {
            e.printStackTrace();
        }
        catch (PathNotFoundException e) {
            e.printStackTrace();
        }
        catch (NoSuchNodeTypeException e) {
            e.printStackTrace();
        }
        catch (VersionException e) {
            e.printStackTrace();
        }
        catch (RepositoryException e) {
            e.printStackTrace();
        } catch (PersistenceException e) {
            e.printStackTrace();
        }
    }
    /**
     * Method to get current time
     */
    private String getLocalDateTime() {
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
        LocalDateTime now = LocalDateTime.now();
        return dtf.format(now);
    }
}
