<%@ page import="org.cilogon.osg.JSONFormatter"%>
<%@ page contentType="application/json;charset=UTF-8" language="java" %>
<%@ page isErrorPage="true" %>
<%
            response.setStatus(500);
            Object excepObj = request.getAttribute("exception");
            String jsonResp = JSONFormatter.exceptionToJSON(excepObj);
            %>
    <%=jsonResp%>



