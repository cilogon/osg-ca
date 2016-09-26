package org.cilogon.osg;

/**
 * Centralizes a few JSON operations for servlet error handling
 * <p>Created by Jeff Gaynor<br>
 * on 6/23/16 at  1:21 PM
 */
public class JSONFormatter {

    public static final String RETRY_LATER = "retry_later";
    public static final String FAILED_PERMANENTLY = "failed_permanently";
    // The next two string are for identifying error messages from standard Java exceptions.
    public static final String NO_USABLE_MY_PROXY_SERVER_FOUND_EXCEPTION = "NoUsableMyProxyServerFoundException";
    public static final String ILLEGAL_ARGUMENT_EXCEPTION = "IllegalArgumentException";

    public static String exceptionToJSON(Object x) {
        if(x instanceof Exception){
            return exceptionToJSON((Exception)x);
        }
        return "{}"; // default empty JSON object
    }
    public static String exceptionToJSON(Exception x) {
        String exceptionString = x.toString();
        String[] exceptionVal = exceptionString.toString().split(":");
        String jsonException = exceptionVal[0];
        String message = x.getMessage();

        String nextAction = null;
        if (jsonException.contains(NO_USABLE_MY_PROXY_SERVER_FOUND_EXCEPTION)) {
            nextAction = RETRY_LATER;
        }
        if (jsonException.contains(ILLEGAL_ARGUMENT_EXCEPTION)) {
            nextAction = FAILED_PERMANENTLY;
        }

        // Fixes CIL-306: JSON response did not have required commas.
        String out = "{\n";
        out = out + "\"message\": " + "\"" + message + "\",\n";
        out = out + "\"exception\": " + "\"" + jsonException + "\",\n";
        out = out + "\"next_action\": " + "\"" + nextAction + "\"\n";
        out = out + "}\n";


        return out;

    }
}
