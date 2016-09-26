package org.cilogon.osg;

import edu.uiuc.ncsa.myproxy.oa4mp.server.MyProxyFacadeProvider;
import edu.uiuc.ncsa.myproxy.oa4mp.server.MyProxyServiceEnvironment;
import edu.uiuc.ncsa.security.core.util.MyLoggingFacade;

import java.util.List;
import java.util.Map;

/**
 * <p>Created by Jeff Gaynor<br>
 * on 10/15/12 at  11:34 AM
 */
public class OsgEnvironment extends MyProxyServiceEnvironment {
    public OsgEnvironment() {
    }

    public OsgEnvironment(MyLoggingFacade myLogger, List<MyProxyFacadeProvider> mfp) {
        super(myLogger, mfp);
    }

    public OsgEnvironment(MyLoggingFacade myLogger, List<MyProxyFacadeProvider> mfp, Map<String, String> constants) {
        super(myLogger, mfp, constants);
    }

}
