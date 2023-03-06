package searchEngine.service;

import java.util.concurrent.atomic.AtomicInteger;
import org.springframework.stereotype.Component;

@Component
public class IndexationStatus {

  private static volatile AtomicInteger numberOfIndexingThreads;
  private static volatile boolean isIndexing;
  private static volatile boolean isCanceled;

  public static void startIndexing() {
    numberOfIndexingThreads = new AtomicInteger(0);
    isIndexing = true;
    isCanceled = false;
  }

  public static void startIndexingThread() {
    numberOfIndexingThreads.incrementAndGet();
  }

  public static void endIndexingThread() {
    numberOfIndexingThreads.decrementAndGet();
    if (numberOfIndexingThreads.get() == 0) {
      isIndexing = false;
      isCanceled = false;
    }
  }

  public static void cancelIndexing() {
    isCanceled = true;
    isIndexing = false;
  }

  public static boolean isIndexing() {
    return isIndexing;
  }

  public static boolean isNotCanceled() {
    return !isCanceled;
  }
}
