package org.cilogon.osg;

import edu.uiuc.ncsa.myproxy.oa4mp.server.servlet.MyProxyConfigurationLoader;
import edu.uiuc.ncsa.security.core.util.MyLoggingFacade;
import org.apache.commons.configuration.tree.ConfigurationNode;

/**  Mostly extended to get a better startup message
 * <p>Created by Jeff Gaynor<br>
 * on 10/15/12 at  3:20 PM
 */
public class OsgConfigLoader extends MyProxyConfigurationLoader<OsgEnvironment> {

    public OsgConfigLoader(ConfigurationNode node) {
        super(node);
    }

    @Override
    public String getVersionString() {
        return "Osg-CA server configuration loader, version " + VERSION_NUMBER;
    }

    public OsgConfigLoader(ConfigurationNode node, MyLoggingFacade logger) {
        super(node, logger);
    }

    @Override
    public OsgEnvironment load() {
        return createInstance();
    }


    @Override
    public OsgEnvironment createInstance() {
        // Might this be better served by having a list of these?
        return new OsgEnvironment(myLogger, getMyProxyFacadeProvider());
    }


}
