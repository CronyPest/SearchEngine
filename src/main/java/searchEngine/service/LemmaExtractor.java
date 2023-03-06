package searchEngine.service;

import java.util.HashMap;
import java.util.List;
import org.jsoup.nodes.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import searchEngine.model.Field;
import searchEngine.repository.DBConnection;
import searchEngine.service.morphology.Lemmatizer;

@Component
public class LemmaExtractor {

  private final List<Field> fields;
  private final Lemmatizer lemmatizer;

  @Autowired
  public LemmaExtractor(DBConnection connection, Lemmatizer lemmatizer) {
    this.lemmatizer = lemmatizer;
    fields = connection.getFieldsList();
  }

  public HashMap<String, Float> getLemmas(Document doc) {
    HashMap<String, Float> rankedLemmas = new HashMap<>();
    fields.forEach(field -> {
      String text = doc.select(field.getSelector()).text();
      HashMap<String, Integer> countedLemmas = lemmatizer.getLemmaList(text);
      countedLemmas.forEach((lemma, count) -> {
        float calculatedRank = count * field.getWeight();
        Float rank = rankedLemmas.get(lemma);
        rankedLemmas.put(lemma, rank == null ? calculatedRank : rank + calculatedRank);
      });
    });
    return rankedLemmas;
  }
}
