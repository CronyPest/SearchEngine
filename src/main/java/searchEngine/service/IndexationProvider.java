package searchEngine.service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import searchEngine.model.Index;
import searchEngine.model.Page;
import searchEngine.repository.DBConnection;

@Component
public class IndexationProvider {

  private final int sublistSize = 500;
  private final DBConnection connection;
  private final ExecutorService executor;

  @Autowired
  public IndexationProvider(DBConnection connection, ExecutorService executor) {
    this.connection = connection;
    this.executor = executor;
  }

  public void createIndexation(List<Page> pages, int siteId)
      throws ExecutionException, InterruptedException {

    if (IndexationStatus.isNotCanceled()) {
      connection.addPages(pages, siteId);
    }

    final AtomicInteger sublist = new AtomicInteger();
    @SuppressWarnings("unchecked")
    CompletableFuture<List<Index>>[] futures = pages.stream()
        .collect(Collectors.groupingBy(t -> sublist.getAndIncrement() / sublistSize))
        .values()
        .stream()
        .map(this::getIndexesAsync)
        .toArray(CompletableFuture[]::new);

    List<Index> indexes = Stream.of(futures)
        .map(CompletableFuture::join)
        .flatMap(Collection::stream)
        .collect(Collectors.toList());

    if (IndexationStatus.isNotCanceled()) {
      connection.addLemmas(indexes, siteId);
    }

    if (IndexationStatus.isNotCanceled()) {
      connection.addIndexes(indexes, siteId);
    }
  }

  public void createIndexation(Page page, int siteId)
      throws ExecutionException, InterruptedException {
    connection.addPage(page, siteId);
    if (!page.getLemmas().isEmpty()) {
      List<Index> indexes = new ArrayList<>();
      page.getLemmas().forEach(
          (lemma, rank) -> indexes.add(Index.builder().page(page).lemma(lemma).rank(rank).build()));
      connection.addLemmas(indexes, siteId);
      connection.addIndexes(indexes, siteId);
    }
  }

  private CompletableFuture<List<Index>> getIndexesAsync(List<Page> pages) {
    return CompletableFuture.supplyAsync(() -> {
      List<Index> indexes = new ArrayList<>();
      pages.stream()
          .filter(page -> !page.getLemmas().isEmpty())
          .forEach(page -> page.getLemmas()
              .forEach((lemma, rank) -> indexes.add(
                  Index.builder().page(page).lemma(lemma).rank(rank).build())));
      return indexes;
    }, executor);
  }
}
