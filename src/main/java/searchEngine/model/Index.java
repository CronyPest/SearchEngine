package searchEngine.model;

import lombok.Builder;
import lombok.NonNull;
import lombok.Value;

@Value
@Builder
public class Index {

  @NonNull
  Page page;
  @NonNull
  String lemma;
  @NonNull
  float rank;
}
