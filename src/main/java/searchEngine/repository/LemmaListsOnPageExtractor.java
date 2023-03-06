package searchEngine.repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.ResultSetExtractor;

final class LemmaListsOnPageExtractor implements ResultSetExtractor<HashMap<Integer, HashMap<String, Float>>> {

  @Override
  public HashMap<Integer, HashMap<String, Float>> extractData(ResultSet resultSet) throws SQLException, DataAccessException {
    HashMap<Integer, HashMap<String, Float>> lemmaListsByPageId = new HashMap<>();
    while (resultSet.next()) {
      Integer pageId = resultSet.getInt("page_id");
      String lemma = resultSet.getString("lemma");
      float rank = resultSet.getFloat("rank");
      HashMap<String, Float> lemmas = lemmaListsByPageId.computeIfAbsent(pageId,
          k -> new HashMap<>());
      lemmas.put(lemma, rank);
    }
    return lemmaListsByPageId;
  }
}
