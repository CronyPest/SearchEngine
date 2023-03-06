package searchEngine;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.everyItem;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.List;
import org.hamcrest.Matchers;
import org.hamcrest.beans.HasPropertyWithValue;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import searchEngine.config.AvailableSitesConfiguration;
import searchEngine.model.Site;

@SpringBootTest(properties = "spring.main.lazy-initialization=true")
class SearchEngineApplicationTest {

  private final SearchEngineApplication application;
  private final AvailableSitesConfiguration configuration;

  @Autowired
  public SearchEngineApplicationTest(SearchEngineApplication application,
      AvailableSitesConfiguration configuration) {
    this.application = application;
    this.configuration = configuration;
  }

  @Test
  @DisplayName("Проверка загрузки контекста")
  void contextLoad() {
    assertNotNull(application);
  }

  @Test
  @DisplayName("Проверка валидности url сайтов")
  void checkSitesForValidUrl() {
    List<Site> sites = configuration.getSites();
    String urlValidationRegex = "^(https?|ftp|file)://[-a-zA-Z\\d+&@#/%?=~_|!:,.;]*[-a-zA-Z\\d+&@#/%=~_|]";
    assertThat(sites, everyItem(
        HasPropertyWithValue.hasProperty("url", Matchers.matchesPattern(urlValidationRegex))));
  }
}