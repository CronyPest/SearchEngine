package searchEngine.service;

import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ForkJoinPool;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.annotation.PostConstruct;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import searchEngine.model.Page;
import searchEngine.model.Site;
import searchEngine.model.SiteStatus;
import searchEngine.repository.DBConnection;

@Component
@Scope("prototype")
public class SiteIndexer implements ApplicationContextAware, Runnable {

  private final Site site;
  private ApplicationContext context;
  private boolean isIndexing = false;
  private DBConnection connection;

  private int siteId;

  public SiteIndexer(Site site) {
    this.site = site;
  }

  @PostConstruct
  private void postConstruct() {
    connection = context.getBean(DBConnection.class);
  }

  public void run() {
    if (IndexationStatus.isNotCanceled()) {
      isIndexing = true;
      IndexationStatus.startIndexingThread();
      String url = site.getUrl();
      String mySite = defineMySite(url);
      siteId = connection.addSite(site);

      new Thread(new SiteIndexationTimeUpdater(connection, this)).start();

      IndexationProvider indexator = context.getBean(IndexationProvider.class);
      PageExtractor.clearUniqueUrl();
      List<Page> pages = new ForkJoinPool().invoke(
          context.getBean(PageExtractor.class, url, mySite));
      try {
        indexator.createIndexation(pages, siteId);
      } catch (ExecutionException | InterruptedException e) {
        e.printStackTrace();
      }
      if (isIndexing) {
        isIndexing = false;
        connection.updateSiteStatus(siteId, SiteStatus.INDEXED.getStatus());
      }
      IndexationStatus.endIndexingThread();
    }
  }

  public int getSiteId() {
    return siteId;
  }

  public void cancel() {
    if (isIndexing) {
      isIndexing = false;
      connection.updateSiteStatus(siteId, SiteStatus.FAILED.getStatus(), "Прервано пользователем");
    }
  }

  public boolean indexPage() {
    String url = site.getUrl();
    String mySite = defineMySite(url);
    int siteId = connection.defineSiteId(mySite);
    if (siteId < 0) {
      return false;
    }
    IndexationProvider indexator = context.getBean(IndexationProvider.class);
    Page page = context.getBean(PageExtractor.class, url, mySite).indexPage();
    try {
      indexator.createIndexation(page, siteId);
    } catch (ExecutionException | InterruptedException e) {
      e.printStackTrace();
    }
    return true;
  }

  private String defineMySite(String url) {
    Pattern p = Pattern.compile(".+//(www\\.)?([^/]+)");
    Matcher m = p.matcher(url);
    if (m.find()) {
      return m.group(2);
    }
    throw new IllegalArgumentException(
        "Site " + site.getName() + " has wrong url format: " + site.getUrl());
  }

  public boolean isIndexing() {
    return isIndexing;
  }

  @Override
  public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
    this.context = applicationContext;
  }
}
