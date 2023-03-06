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
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Service;
import searchEngine.model.SearchResult;
import searchEngine.model.Site;
import searchEngine.repository.DBConnection;

@Service
public class SearchingService implements ApplicationContextAware {

  private final DBConnection connection;
  private final Map<String, List<SearchResult>> foundResults = new HashMap<>();
  private final ExecutorService executor;
  private ApplicationContext context;

  @Autowired
  public SearchingService(DBConnection connection, ExecutorService executor) {
    this.connection = connection;
    this.executor = executor;
  }

  public Map<String, Object> executeSearchQuery(String query, String site, int offset, int limit) {
    List<SearchResult> searchResults;
    String searchTask = query + site;
    if (foundResults.containsKey(searchTask)) {
      searchResults = foundResults.get(searchTask);
      return getResult(searchResults, offset, limit);
    }
    foundResults.clear();
    searchResults = site == null ? allSitesSearch(query) : oneSiteSearch(query, site);
    foundResults.put(searchTask, searchResults);
    return getResult(searchResults, offset, limit);
  }

  private List<SearchResult> allSitesSearch(String query) {
    List<Site> sites = connection.getSites();
    @SuppressWarnings("unchecked")
    CompletableFuture<List<SearchResult>>[] futures = sites.stream()
        .map(Site::getId)
        .map(id -> CompletableFuture.supplyAsync(() -> executeSiteSearch(query, id), executor))
        .toArray(CompletableFuture[]::new);

    return Stream.of(futures)
        .map(CompletableFuture::join)
        .flatMap(Collection::stream)
        .sorted(Comparator.reverseOrder())
        .collect(Collectors.toList());
  }

  private List<SearchResult> oneSiteSearch(String query, String site) {
    int siteId = connection.defineSiteId(site);
    List<SearchResult> results = executeSiteSearch(query, siteId);
    if (results.size() > 1) {
      results.sort(Comparator.reverseOrder());
    }
    return results;
  }

  private List<SearchResult> executeSiteSearch(String query, int siteId) {
    SearchProcessor searcher = context.getBean(SearchProcessor.class, query, siteId);
    return searcher.call();
  }

  private Map<String, Object> getResult(List<SearchResult> searchResults, int offset, int limit) {
    Map<String, Object> result = new HashMap<>();
    result.put("result", true);
    result.put("count", searchResults.size());
    result.put("data",
        searchResults.subList(offset, Math.min(offset + limit, searchResults.size())));
    return result;
  }

  @Override
  public void setApplicationContext(ApplicationContext context) throws BeansException {
    this.context = context;
  }
}
