package org.gusdb.sitesearch;

import static org.gusdb.sitesearch.Context.SOLR_CONNECTION_STRING;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.gusdb.fgputil.server.RESTServer;
import org.json.JSONObject;

@Path("/")
public class Service {

  @GET
  @Produces(MediaType.APPLICATION_JSON)
  public Response runSiteSearch() {
    return Response.ok(new JSONObject()
      .put(SOLR_CONNECTION_STRING, RESTServer.getApplicationContext().get(SOLR_CONNECTION_STRING))
      .put("results", new String[] {
        "an", "extensive", "compilation", "of", "resutls"
      }).toString()).build();
  }

}
