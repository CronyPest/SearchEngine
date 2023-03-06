package searchEngine.service;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import searchEngine.model.SearchResult;
import searchEngine.model.Site;
import searchEngine.repository.DBConnection;

@SpringBootTest(properties = "spring.main.lazy-initialization=true")
class SearchingServiceTest {

  private final SearchingService searchingService;
  @MockBean
  private DBConnection connectionMock;
  @MockBean
  private SearchProcessor searcherMock;

  @Autowired
  public SearchingServiceTest(SearchingService searchingService) {
    this.searchingService = searchingService;
  }

  @SuppressWarnings("unchecked")
  @Test
  @DisplayName("Успешный поиск по одному сайту")
  void singleSiteSearchTest() {
    resultsFound();
    Map<String, Object> result = searchingService.executeSearchQuery("test", "site", 0, 10);

    assertNotNull(result);
    assertThat(result, Matchers.aMapWithSize(3));
    assertThat(result, Matchers.hasEntry("result", true));
    assertThat(result, Matchers.hasEntry("count", 1));
    assertThat(result, Matchers.hasKey("data"));
    assertThat((List<SearchResult>) result.get("data"), Matchers.hasSize(1));
    Mockito.verify(searcherMock, Mockito.times(1)).call();
  }

  @SuppressWarnings("unchecked")
  @Test
  @DisplayName("Успешный поиск по всем сайтам")
  void allSitesSearchTest() {
    resultsFound();
    Mockito.doReturn(List.of(Site.builder().id(1).build(), Site.builder().id(2).build()))
        .when(connectionMock).getSites();
    Map<String, Object> result = searchingService.executeSearchQuery("test", null, 0, 10);

    assertNotNull(result);
    assertThat(result, Matchers.aMapWithSize(3));
    assertThat(result, Matchers.hasEntry("result", true));
    assertThat(result, Matchers.hasEntry("count", 2));
    assertThat(result, Matchers.hasKey("data"));
    assertThat((List<SearchResult>) result.get("data"), Matchers.hasSize(2));
    Mockito.verify(connectionMock, Mockito.times(1)).getSites();
    Mockito.verify(searcherMock, Mockito.times(2)).call();
  }

  @SuppressWarnings("unchecked")
  @Test
  @DisplayName("Ничего не найдено")
  void nothingFoundTest() {
    nothingFound();
    Map<String, Object> result = searchingService.executeSearchQuery("test", "site", 0, 10);

    assertNotNull(result);
    assertThat(result, Matchers.aMapWithSize(3));
    assertThat(result, Matchers.hasEntry("result", true));
    assertThat(result, Matchers.hasEntry("count", 0));
    assertThat(result, Matchers.hasKey("data"));
    assertThat((List<SearchResult>) result.get("data"), Matchers.hasSize(0));
    Mockito.verify(searcherMock, Mockito.times(1)).call();
  }

  private void resultsFound() {
    Mockito.doReturn(List.of(SearchResult.builder()
            .site("site")
            .siteName("siteName")
            .uri("uri")
            .title("title")
            .snippet("snippet")
            .relevance(0.1f).build()))
        .when(searcherMock).call();
  }

  private void nothingFound() {
    Mockito.doReturn(new ArrayList<SearchResult>()).when(searcherMock).call();
  }


}