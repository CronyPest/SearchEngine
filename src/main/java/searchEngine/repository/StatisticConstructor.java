package searchEngine.repository;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import searchEngine.service.IndexationStatus;

public class StatisticConstructor {

  private final List<Map<String, Object>> resultMaps;

  public StatisticConstructor(List<Map<String, Object>> resultMaps) {
    this.resultMaps = resultMaps;
  }

  public Map<String, Object> constructStatistic() {
    Map<String, Object> result = new HashMap<>();
    Map<String, Object> statistics = new HashMap<>();
    Map<String, Object> total = new HashMap<>();
    List<Map<String, Object>> detailed = new ArrayList<>();

    total.put("sites", resultMaps.size());
    total.put("isIndexing", IndexationStatus.isIndexing());

    for (Map<String, Object> map : resultMaps) {
      total.merge("pages", map.get("page_count"), (a, b) -> (long) a + (long) b);
      total.merge("lemmas", map.get("lemma_count"), (a, b) -> (long) a + (long) b);

      Map<String, Object> site = new HashMap<>();
      site.put("url", map.get("url"));
      site.put("name", map.get("name"));
      site.put("status", map.get("status"));
      site.put("statusTime", Timestamp.valueOf((LocalDateTime) map.get("status_time")).getTime());
      if (map.get("last_error") != null) {
        site.put("error", map.get("last_error"));
      }
      site.put("pages", map.get("page_count"));
      site.put("lemmas", map.get("lemma_count"));
      detailed.add(site);
    }
    statistics.put("total", total);
    statistics.put("detailed", detailed);
    result.put("result", true);
    result.put("statistics", statistics);
    return result;
  }
}
