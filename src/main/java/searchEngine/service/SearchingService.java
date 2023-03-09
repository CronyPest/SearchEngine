package searchEngine.service;

import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.PostConstruct;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Service;
import searchEngine.model.FoundPage;
import searchEngine.model.SearchResult;
import searchEngine.model.Site;
import searchEngine.repository.DBConnection;

@Service
public class SearchingService implements ApplicationContextAware {

  private final DBConnection connection;
  private final Map<String, List<FoundPage>> foundPages = new HashMap<>();
  private final ExecutorService executor;
  private ApplicationContext context;
  private ResultConstructor constructor;

  @Autowired
  public SearchingService(DBConnection connection, ExecutorService executor) {
    this.connection = connection;
    this.executor = executor;
  }

  @PostConstruct
  private void postConstruct() {
    constructor = context.getBean(ResultConstructor.class);
  }

  public Map<String, Object> executeSearchQuery(String query, String site, int offset, int limit) {
    List<FoundPage> searchResults;
    String searchTask = query + site;
    if (foundPages.containsKey(searchTask)) {
      searchResults = foundPages.get(searchTask);
      return getResult(searchResults, offset, limit);
    }
    foundPages.clear();
    searchResults = site == null ? allSitesSearch(query) : oneSiteSearch(query, site);
    foundPages.put(searchTask, searchResults);
    return getResult(searchResults, offset, limit);
  }

  private List<FoundPage> allSitesSearch(String query) {
    List<Site> sites = connection.getSites();
    @SuppressWarnings("unchecked")
    CompletableFuture<List<FoundPage>>[] futures = sites.stream()
        .map(Site::getId)
        .map(id -> CompletableFuture.supplyAsync(() -> executeSiteSearch(query, id), executor))
        .toArray(CompletableFuture[]::new);

    return Stream.of(futures)
        .map(CompletableFuture::join)
        .flatMap(Collection::stream)
        .sorted(Comparator.reverseOrder())
        .collect(Collectors.toList());
  }

  private List<FoundPage> oneSiteSearch(String query, String site) {
    int siteId = connection.defineSiteId(site);
    List<FoundPage> results = executeSiteSearch(query, siteId);
    if (results.size() > 1) {
      results.sort(Comparator.reverseOrder());
    }
    return results;
  }

  private List<FoundPage> executeSiteSearch(String query, int siteId) {
    SearchProcessor searcher = context.getBean(SearchProcessor.class, query, siteId);
    return searcher.call();
  }

  private Map<String, Object> getResult(List<FoundPage> foundPages, int offset, int limit) {
    Map<String, Object> result = new HashMap<>();
    result.put("result", true);
    result.put("count", foundPages.size());
    List<SearchResult> data = constructor.constructResults(
        foundPages.subList(offset, Math.min(offset + limit, foundPages.size())));
    result.put("data", data);
    return result;
  }

  @Override
  public void setApplicationContext(ApplicationContext context) throws BeansException {
    this.context = context;
  }
}
