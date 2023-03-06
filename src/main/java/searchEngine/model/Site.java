package searchEngine.model;

import lombok.Builder;
import lombok.Data;

@Data
public class Site {

  private int id;
  private String url;
  private String name;

  public Site() {
  }

  @Builder
  public Site(int id, String url, String name) {
    this.id = id;
    this.url = url;
    this.name = name;
  }
}
