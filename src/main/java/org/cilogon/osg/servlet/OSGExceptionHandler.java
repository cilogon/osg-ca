package org.cilogon.osg.servlet;

import edu.uiuc.ncsa.security.core.util.MyLoggingFacade;
import edu.uiuc.ncsa.security.servlet.ExceptionHandler;
import edu.uiuc.ncsa.security.servlet.JSPUtil;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * Created by wedwards on 4/1/15.
 * Forward to osgerror JSP along with path and exception information
 */

public class OSGExceptionHandler implements ExceptionHandler {
    public OSGExceptionHandler(MyLoggingFacade logger) {
        this.logger = logger;
    }

    MyLoggingFacade logger;

    @Override
    public MyLoggingFacade getLogger() {
        return logger;
    }

    public void handleException(Throwable t, HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        // Just forward exception itself to JSP
        request.setAttribute("exception", t);
        request.setAttribute("servletPath", request.getServletPath())    ;
        request.setAttribute("requestURI", request.getRequestURI());
        JSPUtil.fwd(request, response, "/osgerror.jsp");
    }
}
