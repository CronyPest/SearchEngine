package searchEngine.service;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import searchEngine.config.AvailableSitesConfiguration;
import searchEngine.model.Site;

@SpringBootTest(properties = "spring.main.lazy-initialization=true")
class IndexingServiceTest {

  private final IndexingService indexingService;
  @MockBean
  AvailableSitesConfiguration sitesConfig;
  @MockBean
  SiteIndexer siteIndexer;


  @Autowired
  IndexingServiceTest(IndexingService indexingService) {
    this.indexingService = indexingService;
  }

  @AfterEach
  void tearDown() {
    IndexationStatus.cancelIndexing();
  }

  @Test
  @DisplayName("Запуск полной индексации")
  void beginIndexation() {
    getSites();
    indexingService.beginIndexation();

    Mockito.verify(sitesConfig, Mockito.times(1)).getSites();
    Mockito.verify(siteIndexer, Mockito.times(2)).run();
    assertThat(IndexationStatus.isIndexing(), equalTo(true));
    assertThat(IndexationStatus.isNotCanceled(), equalTo(true));

  }

  @Test
  @DisplayName("Отмена индексации")
  void cancelIndexation() {
    getSites();
    indexingService.beginIndexation();
    indexingService.cancelIndexation();

    Mockito.verify(siteIndexer, Mockito.times(2)).cancel();
    assertThat(IndexationStatus.isIndexing(), equalTo(false));
    assertThat(IndexationStatus.isNotCanceled(), equalTo(false));
  }

  @Test
  @DisplayName("Запуск индексации отдельной страницы")
  void indexPage() {
    Mockito.doReturn(true).when(siteIndexer).indexPage();
    boolean result = indexingService.indexPage("testUrl");

    Mockito.verify(siteIndexer, Mockito.times(1)).indexPage();
    assertThat(result, equalTo(true));
  }

  private void getSites() {
    Mockito.doReturn(List.of(Site.builder().id(1).build(), Site.builder().id(2).build()))
        .when(sitesConfig).getSites();
  }
}