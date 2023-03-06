package searchEngine.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.core.task.TaskExecutor;
import org.springframework.stereotype.Service;
import searchEngine.config.AvailableSitesConfiguration;
import searchEngine.model.Site;

@Service
public class IndexingService implements ApplicationContextAware {

  private final TaskExecutor taskExecutor;
  private final List<SiteIndexer> taskList;
  private ApplicationContext context;

  @Autowired
  public IndexingService(TaskExecutor taskExecutor) {
    this.taskExecutor = taskExecutor;
    taskList = Collections.synchronizedList(new ArrayList<>());
  }

  public void beginIndexation() {
    taskList.clear();
    IndexationStatus.startIndexing();
    AvailableSitesConfiguration sitesConfig = context.getBean(AvailableSitesConfiguration.class);
    List<Site> sites = sitesConfig.getSites();
    CompletableFuture<?>[] futures = sites.stream()
        .map(s -> CompletableFuture.runAsync(() -> indexSite(s), taskExecutor))
        .toArray(CompletableFuture[]::new);
    CompletableFuture<Void> run = CompletableFuture.allOf(futures);
    try {
      run.get();
    } catch (InterruptedException | ExecutionException e) {
      e.printStackTrace();
    }
  }

  public void cancelIndexation() {
    IndexationStatus.cancelIndexing();

    taskList.forEach(SiteIndexer::cancel);
  }

  private void indexSite(Site site) {
    SiteIndexer siteIndexer = context.getBean(SiteIndexer.class, site);
    taskList.add(siteIndexer);
    siteIndexer.run();
  }


  public boolean indexPage(String url) {
    Site tempSite = Site.builder().url(url).name("").build();
    SiteIndexer siteIndexer = context.getBean(SiteIndexer.class, tempSite);
    return siteIndexer.indexPage();
  }

  @Override
  public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
    this.context = applicationContext;
  }
}
