package org.gusdb.sitesearch;

import org.glassfish.jersey.server.ResourceConfig;
import org.gusdb.fgputil.server.ApplicationContext;
import org.gusdb.fgputil.server.RESTServer;
import org.json.JSONObject;

public class Server extends RESTServer {

  public static void main(String[] args) {
    new Server(args).start();
  }

  private Server(String[] args) {
    super(args);
  }

  @Override
  public ResourceConfig getResourceConfig() {
    // create a Jersey resource config containing our service and provider classes
    return new ResourceConfig().registerClasses(Service.class);
  }

  @Override
  protected ApplicationContext createApplicationContext(JSONObject config) {
    return new Context(config);
  }

}
