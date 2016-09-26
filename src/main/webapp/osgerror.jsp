<%@ page contentType="application/json;charset=UTF-8" language="java" %>
<%@ page isErrorPage="true" %>
<%@ page import="org.cilogon.osg.JSONFormatter" %>
<%@ page import="java.util.Date" %>
<%@ page import="java.util.Properties" %>
<%!
            String mailto      = "alerts@cilogon.org";  // production address
         //   String mailto      = "gaynor@illinois.edu";  // Jeff's address
            String mailsubject = "Error caught by Tomcat on "; // + hostname
            String mailsmtp    = "smtp.ncsa.uiuc.edu";
            String jsonException = "unknown";

            // Function to send email
            void sendEmail(String body, String host) {
                Properties props = System.getProperties();
                props.put("mail.host",mailsmtp);
                props.put("mail.transport.protocol","smtp");
                Session mailSession = Session.getDefaultInstance(props,null);
                mailSession.setDebug(false); // Do not echo debug info

                try {
                    Message msg = new MimeMessage(mailSession);
                    InternetAddress[] address = {new InternetAddress(mailto)};
                    msg.setRecipients(Message.RecipientType.TO,address);
                    msg.setSubject(mailsubject + host);
                    msg.setSentDate(new Date());
                    msg.setText(body);
                    Transport.send(msg);
                } catch (Exception e) {
                    //e.printStackTrace();
                }
            }

%>
<%
            response.setStatus(500);

            Exception excep = null;
            String servletPath = null;
            String requestURI = null;

            // Get information about exception and context
            Object excepObj = request.getAttribute("exception");
            String jsonResp = JSONFormatter.exceptionToJSON(excepObj);

            if (request.getAttribute("servletPath") != null) {
                servletPath = (String)request.getAttribute("servletPath");
            }

            if (request.getAttribute("requestURI") != null) {
                requestURI = (String)request.getAttribute("requestURI");
            }
            %>
<%=jsonResp%>
<%
// put together error report for email
String er = "Error Report - " + application.getServerInfo() + "\n";
er       += "------------\n";
er += "Error  : 500\n";
er += "Host   : " + request.getServerName() + "\n";
er += "Client : " + request.getRemoteAddr() + "\n";
er += "Servlet: " + servletPath + "\n";
er += "URL    : " + requestURI + "\n";
er += "\n";

if (excep != null) {
    er += "Exception\n";
    er += "---------\n";
    er += excep.toString() + "\n";
    StackTraceElement[] st = excep.getStackTrace();
    for (int i = 0; i < st.length; i++) {
        er += "    " + st[i].toString() + "\n";
    }
    er += "\n";

    Throwable cause = excep.getCause();
    if (cause != null) {
        er += "Root Cause\n";
        er += "----------\n";
        er += cause.toString() + "\n";
        st = cause.getStackTrace();
        for (int i = 0; i < st.length; i++) {
            er += "    " + st[i].toString() + "\n";
        }
        er += "\n";
    }
}

Cookie[] cookies = request.getCookies();
if (cookies != null) {
    er += "Cookies\n";
    er += "-------\n";
    for (int i = 0; i < cookies.length; i++) {
        er += cookies[i].getName() +" : "+ cookies[i].getValue() + "\n";
    }
    er += "\n";
}

sendEmail(er,request.getServerName());

%>



