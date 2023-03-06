package searchEngine.controller;

import java.util.HashMap;
import java.util.Map;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import searchEngine.repository.DBConnection;
import searchEngine.service.IndexationStatus;
import searchEngine.service.IndexingService;

@RestController
public class IndexController implements ApplicationContextAware {

  private ApplicationContext context;

  @GetMapping("/statistics")
  public Map<String, Object> getStatistics() {
    DBConnection connection = context.getBean(DBConnection.class);
    return connection.getStatistic();
  }

  @GetMapping("/startIndexing")
  public Map<String, Object> startIndexing() {
    IndexingService service = context.getBean(IndexingService.class);
    Map<String, Object> result = new HashMap<>();

    if (!IndexationStatus.isIndexing()) {
      service.beginIndexation();
      result.put("result", true);
    } else {
      result.put("result", false);
      result.put("error", "Индексация уже запущена");
    }
    return result;
  }

  @GetMapping("/stopIndexing")
  public Map<String, Object> stopIndexing() {
    IndexingService service = context.getBean(IndexingService.class);
    Map<String, Object> result = new HashMap<>();

    if (IndexationStatus.isIndexing()) {
      service.cancelIndexation();
      result.put("result", true);
    } else {
      result.put("result", false);
      result.put("error", "Индексация не запущена");
    }
    return result;
  }

  @PostMapping("/indexPage")
  public Map<String, Object> indexPage(@RequestParam String url) {
    Map<String, Object> result = new HashMap<>();
    IndexingService service = context.getBean(IndexingService.class);

    boolean validPage = service.indexPage(url);
    result.put("result", validPage);
    if (!validPage) {
      result.put("error",
          "Данная страница находится за пределами сайтов, указанных в конфигурационном файле");
    }
    return result;
  }

  @Override
  public void setApplicationContext(ApplicationContext context) throws BeansException {
    this.context = context;
  }
}
