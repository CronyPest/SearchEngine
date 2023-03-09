package searchEngine.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.Callable;
import javax.annotation.PostConstruct;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import searchEngine.model.FoundPage;
import searchEngine.model.Lemma;
import searchEngine.model.Page;
import searchEngine.repository.DBConnection;
import searchEngine.service.morphology.Lemmatizer;

@Component
@Scope("prototype")
public class SearchProcessor implements ApplicationContextAware, Callable<List<FoundPage>> {

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
  public List<FoundPage> call() {
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
    return pagesWithRelevance(pages);
  }

  private List<FoundPage> pagesWithRelevance(List<Page> pages) {
    List<FoundPage> results = new ArrayList<>();
    HashMap<Page, Float> pagesWithAbsoluteRelevance = new HashMap<>();
    float maxRelevance = 0f;
    for (Page page : pages) {
      float absRelevance = page.getLemmas().values().stream().reduce(0f, Float::sum);
      maxRelevance = Math.max(maxRelevance, absRelevance);
      pagesWithAbsoluteRelevance.put(page, absRelevance);
    }
    float finalMaxRelevance = maxRelevance;
    pagesWithAbsoluteRelevance.forEach((page, absRelevance) -> {
      FoundPage result = FoundPage.builder().page(page).relevance(absRelevance / finalMaxRelevance)
          .rarestLemma(rarestLemma.getLemma()).build();
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
