package searchEngine.model;

import lombok.Builder;
import lombok.NonNull;
import lombok.Value;

@Value
@Builder
public class Lemma {

  int id;
  @NonNull
  String lemma;
  @NonNull
  int frequency;
}
