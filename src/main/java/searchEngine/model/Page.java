package searchEngine.model;

import java.util.HashMap;
import lombok.Builder;
import lombok.NonNull;
import lombok.Value;

@Value
@Builder
public class Page {

  int id;
  String site;
  String siteName;
  @NonNull
  String path;
  @NonNull
  int code;
  @NonNull
  String content;
  @NonNull
  HashMap<String, Float> lemmas;
}
