package org.cilogon.osg.servlet;

import edu.uiuc.ncsa.myproxy.MPConnectionProvider;
import edu.uiuc.ncsa.myproxy.MyProxyConnectable;
import edu.uiuc.ncsa.myproxy.oa4mp.server.servlet.MyProxyDelegationServlet;
import edu.uiuc.ncsa.security.core.util.DebugUtil;
import edu.uiuc.ncsa.security.delegation.token.MyX509Certificates;
import edu.uiuc.ncsa.security.servlet.AbstractServlet;
import edu.uiuc.ncsa.security.util.pkcs.CertUtil;
import org.apache.commons.codec.binary.Base64;
import org.cilogon.osg.OsgEnvironment;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.StringTokenizer;

/**
 * This servlet class handles host, user, and server certificate requests.
 * Parameters depend on the type of request.  If everything passed in is
 * correct, it should return a certificate
 */
public class OsgCAServlet extends AbstractServlet {

    public static final String PARAM_USERNAME = "username";
    public static final String PARAM_HOSTNAME = "hostname";
    public static final String PARAM_EMAIL = "email";
    public static final String PARAM_CERT_REQ = "cert_request";
    public static final String PARAM_CERT_LIFETIME = "cert_lifetime";
    public static final String PARAM_SRVNAME = "srvname";
    public static final String PARAM_ALT_HOSTNAME = "alt_hostnames";
    /**
     * Delimiter between alt hostnames.
     */
    public static final String ALT_HOSTNAME_DELIM = ",";
    public static final int EMAIL_MAX_LENGTH = 254;
    public static final int MAX_HOSTNAME_LENGTH = 255;
    public static final int MAX_SERVERNAME_LENGTH = 255;
    public static final int MAX_ARG_LENGTH = 100000; // 10^6

    // Very specific debugging to drop everything in standard err rather than in system logging.
    static boolean debugOn = false;

    @Override
    public void debug(String x) {
        if (debugOn) {
            System.err.println(getClass().getSimpleName() + ": " + x);
        }
    }

    protected OsgEnvironment getGE() {
        return (OsgEnvironment) getEnvironment();
    }

    @Override
    public void init()
            throws ServletException {
        super.init();
        this.setExceptionHandler(new OSGExceptionHandler(getMyLogger()));
    }

    // Handle servlet request
    @Override
    protected void doIt(HttpServletRequest req, HttpServletResponse resp) throws Throwable {
        // First check the parameters
        String userName = req.getParameter(PARAM_USERNAME);

        String hostName = req.getParameter(PARAM_HOSTNAME);

        String srvName = req.getParameter(PARAM_SRVNAME);

        String altHostnames = req.getParameter(PARAM_ALT_HOSTNAME);

        debug("got " + PARAM_ALT_HOSTNAME + "=" + altHostnames);

        // Parameter sanity checks
        info("starting request for username=\"" + userName + "\" and hostname=\"" + hostName + "\"");
        // For any of the requests, you need to have either a hostname or a username.
        if ((hostName == null) && (userName == null)) {
            warn("both user name and host name missing, aborting.");
            throw new IllegalArgumentException("Error: Missing hostname and username");
        }

        if (userName != null) {
            if (hostName != null) {
                warn("both username and host name specified, aborting.");
                throw new IllegalArgumentException("Error: Please specify either hostname or username");
            }
            if (altHostnames != null) {
                warn("both username and alt host names specified, aborting.");
                throw new IllegalArgumentException("Error: Please specify a username without alternate hostnames.");

            }
        }

        // You need an email for all of the requests
        String email = req.getParameter(PARAM_EMAIL);
        if (email == null) {
            warn("missing email address, aborting");
            throw new IllegalArgumentException("Error: Missing email address");
        }

        // Email length limit -- more than that is invalid and possibly buffer overflow
        if (email.length() > MAX_ARG_LENGTH) {
            warn("email address length is " + email.length() + ", which exceeds limit of " + MAX_ARG_LENGTH + ", aborting.");
            throw new IllegalArgumentException("Email too long");
        }

        // You need a cert request for all of the requests
        String certReq = req.getParameter(PARAM_CERT_REQ);
        if (certReq == null) {
            warn("No cert request, aborting.");
            throw new IllegalArgumentException("Error: Missing certificate request");
        }

        // Cert lifetime
        long certLifetime = 34257600000L;
        String cl = req.getParameter(PARAM_CERT_LIFETIME);
        if (cl != null) {
            try {
                certLifetime = Long.parseLong(cl);
            } catch (Throwable tt) {
                // ignore a bad cert lifetime, but log it
                info("Could not parse cert lifetime parameter \"" + cl + "\", ignoring it.");
            }
        }

        // Decode cert request (which should be base64 encoded)
        //    byte[] derCertRequest = Base64.decodeBase64(certReq);

        String myProxyUsername = "";

        // Either a host cert or service cert request because hostname is not null
        if (hostName != null) {
            // Enforce length limit on hostname
            if (hostName.length() > MAX_ARG_LENGTH) {
                warn("Hostname length of " + hostName.length() + " exceeds max of " + MAX_ARG_LENGTH + ", aborting.");
                throw new IllegalArgumentException("Hostname too long");
            }
            // Should look like "hostname=test.example.edu email=test@example.edu"


            myProxyUsername = PARAM_HOSTNAME + "=" + hostName + " "
                    + PARAM_EMAIL + "=" + email;
            debug("basic username=" + myProxyUsername);
            // If srvname is null, then it's a host cert request.
            if (srvName != null) {
                // If srvname is not null, then it's a service cert request
                if (srvName.length() > MAX_ARG_LENGTH) {
                    // Enforce length limit on service name
                    warn("Server name has length " + srvName.length() + ", which exceeds limit of " + MAX_ARG_LENGTH + ", aborting.");
                    throw new IllegalArgumentException("Service name too long");
                }
                // Should look like "hostname=text.example.edu srvname=rsv email=test@example.edu"
                myProxyUsername = myProxyUsername + " "
                        + PARAM_SRVNAME + "=" + srvName;
            }
            if (altHostnames != null && altHostnames.length() != 0) {

                if (altHostnames.length() > MAX_ARG_LENGTH) {
                    // Enforce length limit on the alt name list
                    warn("Alt names has length " + altHostnames.length() + ", which exceeds limit of " + MAX_ARG_LENGTH + ", aborting.");
                    throw new IllegalArgumentException("List of alt names too long");
                }
                altHostnames = getUniqueSAN(hostName, altHostnames);

                if(altHostnames!= null && altHostnames.length()!= 0) {
                    // CIL-375 It is still possible that alt hostnames is empty after all that. Do not pass it to MyProxy
                    // since it will result in an empty entry in the cert, which can cause issues for consumers of the cert.
                    myProxyUsername = myProxyUsername + " "
                            + PARAM_ALT_HOSTNAME + "=" + altHostnames;
                }
                debug("username=" + myProxyUsername);

            }
        }

        // This is a user cert request
        if (userName != null) {
            // Enforce length limit on username
            if (userName.length() > MAX_ARG_LENGTH) {
                warn("User name has length " + userName.length() + ", which exceeds limit of " + MAX_ARG_LENGTH + ", aborting.");
                throw new IllegalArgumentException("Username too long");
            }
            // Should look like "username=johndoe email=test@example.edu"
            myProxyUsername = PARAM_USERNAME + "=" + userName + " "
                    + PARAM_EMAIL + "=" + email;
        }
        info("Starting to get certs");
        MPConnectionProvider facades = new MPConnectionProvider(getMyLogger(), MyProxyDelegationServlet.getMyproxyServices());
        MyProxyConnectable mpc = facades.findConnection(myProxyUsername, null, certLifetime);
        mpc.open();
        byte[] pkcs10CR = Base64.decodeBase64(certReq);
        // Fixes CIL-258 (also required a change to the MyProxyConnectable interface.
        MyX509Certificates myCerts = new MyX509Certificates(mpc.getCerts(pkcs10CR));
        info("Writing certs");
        CertUtil.toPKCS7(myCerts.getX509Certificates(), resp.getOutputStream());
        mpc.close();
        info("Done writing certs. Finished");

    }

    // Fixes CIL-291: scan for duplicate alt hostnames. Only use unique ones.
    private String getUniqueSAN(String hostname, String altHostnames) {
        DebugUtil.dbg(this,"alt hostnames=" + altHostnames);
        DebugUtil.dbg(this,"hostname=" + hostname + ", alt hostnames=" + altHostnames);
        // To pre-pend this correctly, we split up the alt host name
        StringTokenizer st = new StringTokenizer(altHostnames, ",");
        ArrayList<String> arrayList = new ArrayList<>();
        while (st.hasMoreTokens()) {
            // This makes the entries unique. Note that we must control for white space or we might get duplicates.
            String token = st.nextToken().trim();
            if (!token.equals(hostname) && !arrayList.contains(token)) {
                arrayList.add(token);
            }
        }
        // Now turn it back in to a string
        String rc = "";
        for (String x : arrayList) {
            rc = rc + (rc.length() == 0 ? "" : ",") + x; // prepend comma after first iteration.
        }
        DebugUtil.dbg(this, "returned alt hostname list = " + rc);
        return rc;
    }


    @Override
    public void loadEnvironment() throws IOException {
        if (environment == null) {
            environment = getConfigurationLoader().load();
        }
    }
}
