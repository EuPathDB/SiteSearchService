package org.gusdb.sitesearch.service.server;

import java.net.URI;

import org.glassfish.jersey.server.ResourceConfig;
import org.gusdb.fgputil.runtime.Environment;
import org.gusdb.fgputil.server.BasicApplicationContext;
import org.gusdb.fgputil.server.RESTServer;
import org.gusdb.fgputil.web.ApplicationContext;
import org.gusdb.sitesearch.service.Service;
import org.gusdb.sitesearch.service.exception.SiteSearchExceptionMapper;
import org.json.JSONObject;

public class Server extends RESTServer {

  private static final String SERVER_PORT_ENV_VAR = "SERVER_PORT";

  public static void main(String[] args) {
    String port = Environment.getOptionalVar(SERVER_PORT_ENV_VAR, "8080");
    new Server(new String[] { "http://0.0.0.0", port }).start();
  }

  private Server(String[] args) {
    super(args);
  }

  @Override
  protected boolean requiresConfigFile() {
    return false;
  }

  @Override
  public ResourceConfig getResourceConfig() {
    // create a Jersey resource config containing our service and provider classes
    return new ResourceConfig().registerClasses(
        Service.class,
        SiteSearchExceptionMapper.class
    );
  }

  @Override
  protected ApplicationContext createApplicationContext(URI serviceUri, JSONObject config) {
    return new Context(config);
  }

  public static class Context extends BasicApplicationContext {

    public static final String SOLR_URL = "SOLR_URL";
    public static final String SOLR_CORE = "SOLR_CORE";

    /**
     * @param config unused config; now performed by env vars
     */
    public Context(JSONObject config) {
      put(SOLR_URL, Environment.getRequiredVar(SOLR_URL));
      put(SOLR_CORE, Environment.getRequiredVar(SOLR_CORE));
    }

    @Override
    public void close() {
      // nothing to do here
    }
  }
}
