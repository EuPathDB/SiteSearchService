package org.gusdb.sitesearch.service.server;

import org.gusdb.fgputil.server.BasicApplicationContext;
import org.json.JSONObject;

public class Context extends BasicApplicationContext {

  public static final String SOLR_URL = "solrUrl";

  public Context(JSONObject config) {
    put(SOLR_URL, config.getString(SOLR_URL));
  }

  @Override
  public void close() {
    // nothing to do here
  }
}
