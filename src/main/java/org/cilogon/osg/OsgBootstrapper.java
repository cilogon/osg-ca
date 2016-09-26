package org.cilogon.osg;

import edu.uiuc.ncsa.security.core.exceptions.GeneralException;
import edu.uiuc.ncsa.security.core.util.ConfigurationLoader;
import edu.uiuc.ncsa.security.servlet.Bootstrapper;
import edu.uiuc.ncsa.security.servlet.Initialization;
import edu.uiuc.ncsa.security.servlet.ServletConfigUtil;
import org.apache.commons.configuration.tree.ConfigurationNode;

import javax.servlet.ServletContext;

/**
 * <p>Created by Jeff Gaynor<br>
 * on 10/15/12 at  3:18 PM
 */
public class OsgBootstrapper extends Bootstrapper {
    public static final String OSG_NODE_NAME = "osg";
    public static final String OSG_CONFIG_FILE_KEY = "osg:server.config.file";
    public static final String OSG_CONFIG_NAME_KEY = "osg:server.config.name";


    protected ConfigurationNode getNode(ServletContext servletContext) throws Exception {
        return ServletConfigUtil.findConfigurationNode(servletContext, OSG_CONFIG_FILE_KEY, OSG_CONFIG_NAME_KEY, OSG_NODE_NAME);
    }


    @Override
    public ConfigurationLoader getConfigurationLoader(ServletContext servletContext) throws Exception {
        if (servletContext.getInitParameter(OSG_CONFIG_FILE_KEY) == null) {
            throw new GeneralException("Error: No configuration found. Cannot configure the server.");
        }
        return getConfigurationLoader(getNode(servletContext));
    }

    @Override
    public ConfigurationLoader getConfigurationLoader(ConfigurationNode node) throws GeneralException {
       return new OsgConfigLoader(node);
    }

    @Override
    public Initialization getInitialization() {
        return null;
    }
}
