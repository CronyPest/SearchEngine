package searchEngine.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.Callable;
import javax.annotation.PostConstruct;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import searchEngine.model.Lemma;
import searchEngine.model.Page;
import searchEngine.model.SearchResult;
import searchEngine.repository.DBConnection;
import searchEngine.service.morphology.Lemmatizer;

@Component
@Scope("prototype")
public class SearchProcessor implements ApplicationContextAware, Callable<List<SearchResult>> {

  private final String query;
  private final int siteId;
  private Lemmatizer lemmatizer;
  private DBConnection connection;
  private Lemma rarestLemma;
  private ApplicationContext context;

  public SearchProcessor(String query, int siteId) {
    this.query = query;
    this.siteId = siteId;
  }

  @PostConstruct
  private void postConstruct() {
    lemmatizer = context.getBean(Lemmatizer.class);
    connection = context.getBean(DBConnection.class);
  }

  @Override
  public List<SearchResult> call() {
    HashMap<String, Integer> lemmaList = lemmatizer.getLemmaList(query);
    List<Lemma> lemmas = connection.getLemmas(new ArrayList<>(lemmaList.keySet()), siteId);
    if (lemmas.size() != lemmaList.size()) {
      return new ArrayList<>(); //Возвращаем пустой список
    }
    rarestLemma = lemmas.get(0);
    List<Page> pages = connection.getInitialPageListByRarestLemma(rarestLemma);
    for (Lemma lemma : lemmas.subList(1, lemmas.size())) {
      pages = checkPagesForLemmaMatch(pages, lemma);
      if (pages.isEmpty()) {
        return new ArrayList<>(); //Возвращаем пустой список
      }
    }
    return pagesToSearchResults(pages);
  }

  private List<SearchResult> pagesToSearchResults(List<Page> pages) {
    List<SearchResult> results = new ArrayList<>();
    HashMap<Page, Float> pagesWithAbsoluteRelevance = new HashMap<>();
    float maxRelevance = 0f;
    for (Page page : pages) {
      float absRelevance = page.getLemmas().values().stream().reduce(0f, Float::sum);
      maxRelevance = Math.max(maxRelevance, absRelevance);
      pagesWithAbsoluteRelevance.put(page, absRelevance);
    }
    float finalMaxRelevance = maxRelevance;
    pagesWithAbsoluteRelevance.forEach((page, absRelevance) -> {
      Document document = Jsoup.parse(page.getContent());
      String text = document.select("body").text();
      String site = page.getSite();
      String siteName = page.getSiteName();
      String uri = page.getPath();
      String title = document.title();
      String snippet = lemmatizer.getSnippet(text, rarestLemma.getLemma());
      float relevance = absRelevance / finalMaxRelevance;
      SearchResult result = SearchResult.builder()
          .site(site)
          .siteName(siteName)
          .uri(uri)
          .title(title)
          .snippet(snippet)
          .relevance(relevance).build();
      results.add(result);
    });
    return results;
  }

  private List<Page> checkPagesForLemmaMatch(List<Page> pages, Lemma lemma) {
    List<Page> checkedPages = new ArrayList<>();
    pages.forEach(page -> {
      if (page.getLemmas().containsKey(lemma.getLemma())) {
        checkedPages.add(page);
      }
    });
    return checkedPages;
  }

  @Override
  public void setApplicationContext(ApplicationContext context) throws BeansException {
    this.context = context;
  }
}
