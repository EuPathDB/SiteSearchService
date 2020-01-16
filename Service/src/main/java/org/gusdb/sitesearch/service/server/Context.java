package org.gusdb.sitesearch.service.server;

import org.apache.log4j.Logger;
import org.gusdb.fgputil.server.BasicApplicationContext;
import org.json.JSONObject;

public class Context extends BasicApplicationContext {

  private static final Logger LOG = Logger.getLogger(Context.class);

  public static final String SOLR_URL = "solrUrl";

  public Context(JSONObject config) {
    put(SOLR_URL, config.getString(SOLR_URL));
  }

  @Override
  public void close() {
    // close any resources here
    LOG.info("SOLR Resources closing... closed!");
  }
}
