package searchEngine.repository;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import searchEngine.model.Page;

public class PageBatchPreparedStatementSetter implements BatchPreparedStatementSetter {

  private final List<Page> pages;
  private final int siteId;

  public PageBatchPreparedStatementSetter(List<Page> pages, int siteId) {
    super();
    this.pages = pages;
    this.siteId = siteId;
  }

  @Override
  public void setValues(PreparedStatement preparedStatement, int i) throws SQLException {
    Page page = pages.get(i);
    preparedStatement.setInt(1, siteId);
    preparedStatement.setString(2, page.getPath());
    preparedStatement.setInt(3, page.getCode());
    preparedStatement.setString(4, page.getContent());
  }

  @Override
  public int getBatchSize() {
    return pages.size();
  }
}
