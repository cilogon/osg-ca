package org.cilogon.osg.servlet;

import edu.uiuc.ncsa.myproxy.MyProxyLogon;
import edu.uiuc.ncsa.myproxy.MyProxyServiceFacade;
import edu.uiuc.ncsa.myproxy.NoUsableMyProxyServerFoundException;
import edu.uiuc.ncsa.myproxy.oa4mp.server.servlet.MyProxyDelegationServlet;
import edu.uiuc.ncsa.security.servlet.AbstractServlet;
import org.cilogon.osg.OsgEnvironment;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.List;

/**
 * This is used for internal monitoring and is kind of described
 * in CIL-119.
 * This is deployed at the endpoint named "ping". If this is successful a response with
 * HTTP return code of 204 (no content is returned).
 * This should also accept an optional parameter of
 * "myproxy=1" to indicate that all the servers are to be tried in turn. in this case, a 200 is returned in case
 * of success and in case of failure a code of 500 is returned with a list of every server that failed.<br/>
 * For parsing, the list of failed servers is comma separated of the form
 * <pre>
 *     Unable to connect to any configured MyProxy server: server1, server2.
 * </pre>
 * Note that this does <b>not</b> follow ping as is found in the rest of OA4MP, but has
 * its own mini-protocol.<br/>
 * Created by wedwards on 3/26/15.
 */
public class OSGPingServlet extends AbstractServlet {

    public static final String PARAM_MYPROXY = "myproxy";
    public static final String ERROR_MESSAGE = "Unable to connect to any configured MyProxy server: ";
    public static final String MYPROXY_CODE = "1";

    @Override
    public void init() throws ServletException {
        super.init();
        this.setExceptionHandler(new PingExceptionHandler(getMyLogger()));
    }

    @Override
    public void loadEnvironment() throws IOException {
        if (environment == null) {
            environment = getConfigurationLoader().load();
        }
    }

    protected OsgEnvironment getGE() {
        return (OsgEnvironment) getEnvironment();
    }

    /**
     * Puts failures in the passed in list and returns the name of a successful server if connected.
     * If there is no successful connection, a null is returned.
     *
     * @param failedServers
     * @return
     * @throws Throwable
     */
    protected String checkServers(ArrayList<String> failedServers) {

        List<MyProxyServiceFacade> facades = MyProxyDelegationServlet.getMyproxyServices();

        for (int i = 0; i < facades.size(); i++) {
            MyProxyServiceFacade facade = facades.get(i);
            String myProxyHost = facade.getFacadeConfiguration().getHostname();
            int myProxyPort = facade.getFacadeConfiguration().getPort();
            MyProxyLogon testLogon = new MyProxyLogon(getGE().getMyLogger(), facade.getFacadeConfiguration().getServerDN());
            testLogon.setHost(myProxyHost);
            testLogon.setPort(myProxyPort);
            testLogon.setSocketTimeout(100);
            try {
                testLogon.connect();
                return myProxyHost;
            } catch (Throwable e) {
                info("Ping failed to connect to " + myProxyHost);
                failedServers.add(myProxyHost+ ":" + myProxyPort);
                // do nothing -- default response is no host found
            }
        }
        return null;
    }

    protected void doIt(HttpServletRequest req, HttpServletResponse resp) throws Throwable {
        resp.setContentType("text/plain");
        // See if the MyProxy server parameter was passed in
        String myProxy = req.getParameter(PARAM_MYPROXY);
        ArrayList<String> failedServers = new ArrayList<>();

        if (myProxy != null) {
            if (myProxy.equals(MYPROXY_CODE)) {
                String successfulServer = checkServers(failedServers);
                if (successfulServer == null) {
                    // so this failed and we return an error message.
                    resp.setStatus(500);
                    String response = ERROR_MESSAGE + getCSVString(failedServers);
                    req.setAttribute("msg", response);
                    throw new NoUsableMyProxyServerFoundException(response);
                } else {
                    OutputStream out = resp.getOutputStream();
                    OutputStreamWriter osw = new OutputStreamWriter(out);
                    osw.flush();
                    resp.setStatus(200);
                    out.write(successfulServer.getBytes());
                    out.flush();
                    out.close();
                }
            }
        } else {
            resp.setStatus(204);
        }

    }

    /**
     * Changes a {@link List} of server names to a comma separated string.
     *
     * @param failedServers
     * @return
     */
    protected String getCSVString(List<String> failedServers) {
        String failureList = "";
        boolean firstPass = true;
        for (String x : failedServers) {
            if (firstPass) {
                failureList = x;
                firstPass = false;
            } else {
                failureList = failureList + "," + x;
            }
        }
        return failureList;
    }
}
