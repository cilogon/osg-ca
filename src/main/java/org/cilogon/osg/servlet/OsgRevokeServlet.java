package org.cilogon.osg.servlet;

import edu.uiuc.ncsa.security.servlet.AbstractServlet;
import edu.uiuc.ncsa.security.util.mail.MailUtil;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;


/**
 * Created with IntelliJ IDEA.
 * User: wedwards
 * Date: 5/15/14
 * Time: 5:07 PM
 * This class handles certificate revocation requests by creating a new file in
 * the designated directory and sending an email.  Email parameters are set in
 * the web.xml file
 */
public class OsgRevokeServlet extends AbstractServlet {
    public static final String PARAM_SERIAL = "serial";

    @Override
    public void init() throws ServletException {

        super.init();
        this.setExceptionHandler(new OSGExceptionHandler(getMyLogger()));
    }

    @Override
    public void loadEnvironment() throws IOException {
        if(environment == null){
            environment =  getConfigurationLoader().load();
        }
    }

    /**
     *
     * @return  Directory to store revocation requests
     */
    private String getRevokeDir() {
        return getServletContext().getInitParameter("revoke.directory");
    }

    // Get parameters from XML file related to sending email

    private boolean getMailEnabled() {
        return Boolean.parseBoolean(getServletContext().getInitParameter("mail.enabled")) ;
    }

    private String getMailServer() {
        return getServletContext().getInitParameter("mail.server");
    }

    private int getPort() {
        return Integer.parseInt(getServletContext().getInitParameter("port"));
        //return 25;
    }

    private String getPassword() {
        return getServletContext().getInitParameter("password");
    }

    private String  getFrom() {
        return getServletContext().getInitParameter("from");
    }

    private String getRecipients() {
        return getServletContext().getInitParameter("recipients");
    }

    private String getMessageTemplate() {
        return getServletContext().getInitParameter("message.template");
    }

    private String getSubjectTemplate() {
        return getServletContext().getInitParameter("subject.template");
    }

    private boolean getUseSSL () {
        return Boolean.parseBoolean(getServletContext().getInitParameter("use.ssl")) ;
    }

    private boolean getStartTLS() {
        return Boolean.parseBoolean(getServletContext().getInitParameter("start.TLS")) ;
    }


    // Handle revocation request here
    @Override
    protected void doIt(HttpServletRequest req, HttpServletResponse resp) throws Throwable {
        String serialNumber = req.getParameter(PARAM_SERIAL);
        if (serialNumber == null) {
            throw new IllegalArgumentException("Error: Missing serial number");
        }
        // Get the directory to use
        String revokeDir = getRevokeDir();
        String sanitizedSerial = sanitize(serialNumber);
        // Create a file using the serial number passed in as the ID
        FileWriter fileWriter = null;
        try {
            String content = "";
            String fileName  = revokeDir + "/" + sanitizedSerial;
            File newTextFile = new File(fileName);
            fileWriter = new FileWriter(newTextFile);
            fileWriter.write(content);
            fileWriter.close();
        } catch (IOException ex) {
            // If there's a problem, write it to the log
            info("Problem creating file in " + revokeDir + " with name " + serialNumber);
        }
        // Send the email
        sendNotification(sanitizedSerial);
    }

    /**
     * Send email notification using MailUtil from OA4MP
     * @param revoke_id Serial number of cert to be revoked
     */
    private void sendNotification(String revoke_id) {


        try {
            HashMap<String, String> replacements = new HashMap<String, String>();
            replacements.put("revoke_id", revoke_id);
            replacements.put("revoke_directory", getRevokeDir());

            MailUtil.MailEnvironment mailEnv = new MailUtil.MailEnvironment(getMailEnabled(),
                    getMailServer(),
                    getPort(),getPassword(),
                    getFrom(),getRecipients(),
                    getMessageTemplate(),
                    getSubjectTemplate(),
                    getUseSSL(), getStartTLS());

            MailUtil mailUtil = new MailUtil(mailEnv);

            mailUtil.sendMessage(replacements);
        } catch (Exception e) {
            info ("Problem sending email about ID " + revoke_id);
        }
    }

    /*
    Check to make sure that revoke parameter contains only numbers or letters
     */
    private String sanitize(String input) {

        String output = "";
        for (int i = 0; i < input.length(); i++ ) {
            char ch = input.charAt(i);
            if (Character.isLetterOrDigit(ch)) {
                  output = output + ch;
            }
        }
        return output;
    }

}
