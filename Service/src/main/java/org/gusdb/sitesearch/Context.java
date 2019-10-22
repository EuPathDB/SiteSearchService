package org.gusdb.sitesearch;

import org.apache.log4j.Logger;
import org.gusdb.fgputil.server.BasicApplicationContext;
import org.json.JSONObject;

public class Context extends BasicApplicationContext {

  private static final Logger LOG = Logger.getLogger(Context.class);

  public static final String SOLR_CONNECTION_STRING = "solrConnectionString";

  public Context(JSONObject config) {
    // 1. read config object and throw IllegalArgumentException if invalid
    put(SOLR_CONNECTION_STRING, config.getString(SOLR_CONNECTION_STRING));
    // 2. do any application setup here, e.g. initialize connection pool
    // 3. TODO: provide getters for anything the service might need
  }

  public String getSolrConnectionString() { return (String)get(SOLR_CONNECTION_STRING); }

  @Override
  public void close() {
    // close any resources here
    LOG.info("SOLR Resources closing... closed!");
  }
}
