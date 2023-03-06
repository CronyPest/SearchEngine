package searchEngine.repository;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.List;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import searchEngine.model.Index;

public class LemmaBatchPreparedStatementSetter implements BatchPreparedStatementSetter {

  private final List<Index> indexes;
  private final int siteId;

  public LemmaBatchPreparedStatementSetter(List<Index> indexes, int siteId) {
    super();
    this.indexes = indexes;
    this.siteId = siteId;
  }

  @Override
  public void setValues(PreparedStatement preparedStatement, int i) throws SQLException {
    Index index = indexes.get(i);
    preparedStatement.setInt(1, siteId);
    preparedStatement.setString(2, index.getLemma());
  }

  @Override
  public int getBatchSize() {
    return indexes.size();
  }
}
