package searchEngine.model;

import lombok.Builder;
import lombok.NonNull;
import lombok.Value;

@Value
@Builder
public class FoundPage implements Comparable<FoundPage> {

  @NonNull
  float relevance;
  @NonNull
  Page page;
  @NonNull
  String rarestLemma;

  @Override
  public int compareTo(FoundPage o) {
    return Float.compare(this.relevance, o.getRelevance());
  }
}
