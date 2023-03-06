package searchEngine.controller;

import java.util.HashMap;
import java.util.Map;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import searchEngine.repository.DBConnection;
import searchEngine.service.IndexationStatus;
import searchEngine.service.SearchingService;

@RestController
public class SearchController implements ApplicationContextAware {

  private ApplicationContext context;

  @GetMapping("/search")
  public Map<String, Object> getSearchResult(@RequestParam String query,
      @RequestParam(required = false) String site,
      @RequestParam(defaultValue = "0") int offset,
      @RequestParam(defaultValue = "20") int limit) {

    DBConnection connection = context.getBean(DBConnection.class);
    SearchingService service = context.getBean(SearchingService.class);
    Map<String, Object> result = new HashMap<>();

    if (query.isEmpty()) {
      result.put("result", false);
      result.put("error", "Введите поисковый запрос!");
      return result;
    }

    if (IndexationStatus.isIndexing()) {
      result.put("result", false);
      result.put("error", "Индексация ещё не завершена.");
      return result;
    }

    if (site == null) {
      if (!connection.checkSiteStatus()) {
        result.put("result", false);
        result.put("error", "Не все сайты проиндексированы полностью.");
        return result;
      }
    } else {
      if (!connection.checkSiteStatus(site)) {
        result.put("result", false);
        result.put("error", "Сайт " + site + " не проиндексирован полностью.");
        return result;
      }
    }
    result = service.executeSearchQuery(query, site, offset, limit);
    return result;
  }

  @Override
  public void setApplicationContext(ApplicationContext context) throws BeansException {
    this.context = context;
  }
}
