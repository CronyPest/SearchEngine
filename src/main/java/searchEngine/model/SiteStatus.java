package searchEngine.model;

public enum SiteStatus {
  INDEXING("INDEXING"),
  INDEXED("INDEXED"),
  FAILED("FAILED");

  private final String status;

  SiteStatus(String status) {
    this.status = status;
  }

  public String getStatus() {
    return status;
  }
}
