package org.gusdb.sitesearch.service.server;

import java.net.URI;

import org.glassfish.jersey.server.ResourceConfig;
import org.gusdb.fgputil.server.RESTServer;
import org.gusdb.fgputil.web.ApplicationContext;
import org.gusdb.sitesearch.service.Service;
import org.gusdb.sitesearch.service.SiteSearchExceptionMapper;
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
    return new ResourceConfig().registerClasses(
        Service.class,
        SiteSearchExceptionMapper.class
    );
  }

  @Override
  protected ApplicationContext createApplicationContext(URI serviceUri, JSONObject config) {
    return new Context(config);
  }

}
