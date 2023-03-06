package searchEngine.model;

import lombok.Builder;
import lombok.NonNull;
import lombok.Value;

@Value
@Builder
public class SearchResult implements Comparable<SearchResult> {

  @NonNull
  String site;
  @NonNull
  String siteName;
  @NonNull
  String uri;
  @NonNull
  String title;
  @NonNull
  String snippet;
  @NonNull
  float relevance;

  @Override
  public int compareTo(SearchResult o) {
    return Float.compare(this.relevance, o.getRelevance());
  }
}
