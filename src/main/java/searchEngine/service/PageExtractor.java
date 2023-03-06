package searchEngine.service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.RecursiveTask;
import java.util.regex.Pattern;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import searchEngine.model.Page;

@Component
@Scope("prototype")
public class PageExtractor extends RecursiveTask<List<Page>> implements ApplicationContextAware {

  private static HashSet<String> uniqueURL = new HashSet<>();
  private final String url;
  private final String mySite;
  @Value("${search-bot-user-agent}")
  String userAgent;
  @Value("${referrer}")
  String referrer;
  private ApplicationContext context;

  public PageExtractor(String url, String mySite) {
    this.url = url;
    this.mySite = mySite;
  }

  public static void clearUniqueUrl() {
    uniqueURL = new HashSet<>();
  }

  @Override
  protected List<Page> compute() {
    if (uniqueURL.isEmpty()) {
      uniqueURL.add(url);
    }
    List<PageExtractor> tasks = new ArrayList<>();
    List<Page> list = new ArrayList<>();
    try {
      Thread.sleep(150);
      Connection.Response response = getConnection(url);
      Document document = response.parse();

      Page page = buildPage(response, document);
      list.add(page);

      Elements links = document.select("a");
      if (!links.isEmpty()) {
        links.stream().map((link) -> link.attr("abs:href")).forEachOrdered((thisUrl) -> {
          boolean add = uniqueURL.add(thisUrl);
          if (add && isValidUrl(thisUrl) && IndexationStatus.isNotCanceled()) {
            PageExtractor task = context.getBean(PageExtractor.class, thisUrl, mySite);
            task.fork();
            tasks.add(task);
          }
        });
      }
    } catch (InterruptedException | IOException e) {
      e.printStackTrace();
    }
    if (IndexationStatus.isNotCanceled()) {
      for (PageExtractor task : tasks) {
        list.addAll(task.join());
      }
    }
    return list;
  }

  public Page indexPage() {
    try {
      Connection.Response response = getConnection(url);
      Document document = response.parse();
      return buildPage(response, document);
    } catch (IOException e) {
      e.printStackTrace();
    }
    return null;
  }

  public Page buildPage(Connection.Response response, Document document) {
    String path = getPath(url);
    int responseCode = response.statusCode();
    String content = document.outerHtml();
    LemmaExtractor lemmaExtractor = context.getBean(LemmaExtractor.class);
    HashMap<String, Float> lemmas =
        (responseCode < 400) ? lemmaExtractor.getLemmas(document) : new HashMap<>();
    return Page.builder().path(path).code(responseCode).content(content).lemmas(lemmas)
        .build();
  }

  private Connection.Response getConnection(String url) throws IOException {
    return Jsoup.connect(url)
        .userAgent(userAgent)
        .ignoreContentType(true)
        .ignoreHttpErrors(true)
        .timeout(0)
        .referrer(referrer)
        .execute();
  }

  private boolean isValidUrl(String url) {
    boolean notSubjectForIndexing = Pattern.compile(
        "^(?i).+\\.(jpe?g|png|gif|eps|webp|avif|svg|tiff|pdf|xlsx?$)").matcher(url).matches();
    boolean hasValidProtocol = url.contains("http:") || url.contains("https:");
    return hasValidProtocol && url.contains(mySite) && !url.contains("#") && !notSubjectForIndexing;
  }

  private String getPath(String url) {
    int startPathIndex = url.indexOf(mySite) + mySite.length();
    String path = url.substring(startPathIndex);
    return path.isEmpty() ? "/" : path;
  }

  @Override
  public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
    this.context = applicationContext;
  }
}
