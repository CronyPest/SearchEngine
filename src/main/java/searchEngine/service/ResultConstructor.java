package searchEngine.service;

import java.util.ArrayList;
import java.util.List;
import javax.annotation.PostConstruct;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;
import searchEngine.model.FoundPage;
import searchEngine.model.Page;
import searchEngine.model.SearchResult;
import searchEngine.service.morphology.Lemmatizer;

@Component
public class ResultConstructor implements ApplicationContextAware {

  private ApplicationContext context;
  private Lemmatizer lemmatizer;

  @PostConstruct
  private void postConstruct() {
    lemmatizer = context.getBean(Lemmatizer.class);
  }

  public List<SearchResult> constructResults(List<FoundPage> foundPages) {
    List<SearchResult> results = new ArrayList<>();
    foundPages.forEach(foundPage -> {
      Page page = foundPage.getPage();
      Document document = Jsoup.parse(page.getContent());
      String text = document.select("body").text();
      String site = page.getSite();
      String siteName = page.getSiteName();
      String uri = page.getPath();
      String title = document.title();
      String snippet = lemmatizer.getSnippet(text, foundPage.getRarestLemma());
      SearchResult result = SearchResult.builder()
          .site(site)
          .siteName(siteName)
          .uri(uri)
          .title(title)
          .snippet(snippet)
          .relevance(foundPage.getRelevance()).build();
      results.add(result);
    });
    return results;
  }

  @Override
  public void setApplicationContext(ApplicationContext context) throws BeansException {
    this.context = context;
  }
}
