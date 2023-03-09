package searchEngine.model;

import lombok.Builder;
import lombok.NonNull;
import lombok.Value;

@Value
@Builder
public class SearchResult {

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
}
