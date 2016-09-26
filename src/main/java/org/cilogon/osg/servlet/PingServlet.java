package org.cilogon.osg.servlet;

import edu.uiuc.ncsa.myproxy.MPConnectionProvider;
import edu.uiuc.ncsa.myproxy.NoUsableMyProxyServerFoundException;
import edu.uiuc.ncsa.myproxy.oa4mp.server.MyProxyServiceEnvironment;
import edu.uiuc.ncsa.security.servlet.AbstractServlet;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.List;

/**
 * <p>Created by Jeff Gaynor<br>
 * on 11/19/15 at  2:52 PM
 */
public class PingServlet extends AbstractServlet {
    public static final String PARAM_MYPROXY = "myproxy";
    public static final String ERROR_MESSAGE = "Unable to connect to any configured MyProxy server:";

    @Override
     public void init() throws ServletException {
         super.init();
         this.setExceptionHandler(new PingExceptionHandler(getMyLogger()));
     }

    @Override
    protected void doIt(HttpServletRequest req, HttpServletResponse resp) throws Throwable {
        resp.setContentType("text/plain");
        // See if the MyProxy server parameter was passed in
        String myProxy = req.getParameter(PARAM_MYPROXY);
        ArrayList<String> failedServers = new ArrayList<>();

        if (myProxy != null) {
            if (myProxy.equals("1")) {
                String successfulServer = null; //checkServers(failedServers);
                MPConnectionProvider mpc = new MPConnectionProvider(getMyLogger(), getMPE().getMyProxyServices());
                /*try{
                    mpc.findConnection()
                }*/
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
        }

        else {
            resp.setStatus(204);
        }
    }

    @Override
    public void loadEnvironment() throws IOException {
        if (environment == null) {
            environment = getConfigurationLoader().load();
        }
    }

    public MyProxyServiceEnvironment getMPE(){
             return (MyProxyServiceEnvironment) getEnvironment();
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
