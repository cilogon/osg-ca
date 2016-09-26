package org.cilogon.osg.servlet;

import edu.uiuc.ncsa.security.core.util.MyLoggingFacade;
import edu.uiuc.ncsa.security.servlet.ExceptionHandler;
import edu.uiuc.ncsa.security.servlet.JSPUtil;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * Created by wedwards on 4/5/15.
 * This class forwards ping exception (no usable MyProxy server found) to custom JSP.
 */
public class PingExceptionHandler implements ExceptionHandler {
    public PingExceptionHandler(MyLoggingFacade logger) {
        this.logger = logger;
    }

    MyLoggingFacade logger;

    @Override
    public MyLoggingFacade getLogger() {
        return logger;
    }

    // The request should include the "msg" attribute that will be displayed on the JSP page
    public void handleException(Throwable t, HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        request.setAttribute("exception", t);
        request.setAttribute("servletPath", request.getServletPath());
        request.setAttribute("requestURI", request.getRequestURI());
        JSPUtil.fwd(request, response, "/pingerror.jsp");
    }
}
