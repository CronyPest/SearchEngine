package searchEngine.model;

import lombok.Builder;
import lombok.NonNull;
import lombok.Value;

@Value
@Builder
public class Field {

  @NonNull
  String selector;
  @NonNull
  float weight;
}
