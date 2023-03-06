package searchEngine.service;

import searchEngine.model.SiteStatus;
import searchEngine.repository.DBConnection;

public class SiteIndexationTimeUpdater implements Runnable {

  private final DBConnection connection;
  private final SiteIndexer indexer;
  private final int siteId;

  public SiteIndexationTimeUpdater(DBConnection connection, SiteIndexer indexer) {
    this.connection = connection;
    this.indexer = indexer;
    this.siteId = indexer.getSiteId();
  }

  @Override
  public void run() {
    while (indexer.isIndexing()) {
      connection.updateSiteStatus(siteId, SiteStatus.INDEXING.getStatus());
      try {
        Thread.sleep(20000);
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
    }
  }
}
