package searchEngine.repository;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import searchEngine.model.Field;
import searchEngine.model.Index;
import searchEngine.model.Lemma;
import searchEngine.model.Page;
import searchEngine.model.Site;
import searchEngine.model.SiteStatus;
import searchEngine.service.IndexationStatus;

@Component
public class DBConnection {

  private final int batchSize = 500;
  private final ExecutorService executor;
  private final JdbcTemplate jdbcTemplate;

  @Autowired
  public DBConnection(JdbcTemplate jdbcTemplate, ExecutorService executor) {
    this.jdbcTemplate = jdbcTemplate;
    this.executor = executor;
  }

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public void addPages(List<Page> pages, int siteId)
      throws InterruptedException, ExecutionException {
    String sql = "INSERT IGNORE INTO page(site_id, path, code, content) VALUES(?, ?, ?, ?)";
    @SuppressWarnings("unchecked")
    CompletableFuture<?>[] futures = getSublistStream(pages)
        .map(pageSublist -> CompletableFuture.runAsync(
            () -> {
              if (IndexationStatus.isNotCanceled()) {
                jdbcTemplate.batchUpdate(sql,
                    new PageBatchPreparedStatementSetter((List<Page>) pageSublist, siteId));
              }
            }, executor))
        .toArray(CompletableFuture[]::new);

    CompletableFuture<Void> run = CompletableFuture.allOf(futures);
    run.get();
  }

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public void addPage(Page page, int siteId) {
    String path = page.getPath();
    String sql = """
        UPDATE lemma lem1
        JOIN (SELECT lemma FROM lemma l
        JOIN `index` i ON i.lemma_id = l.id
        JOIN page p ON p.id = i.page_id
        WHERE p.path = ?
        ) lem2 ON lem1.lemma = lem2.lemma
        AND site_id = ?
        SET frequency = frequency -1
        """;
    jdbcTemplate.update(sql, path, siteId);
    jdbcTemplate.update("DELETE FROM page WHERE site_id=? AND path=?", siteId, path);
    jdbcTemplate.update("INSERT INTO page(site_id, path, code, content) VALUES(?, ?, ?, ?)",
        siteId, path, page.getCode(), page.getContent());
  }

  @SuppressWarnings("unchecked")
  public void addLemmas(List<Index> indexes, int siteId) {
    String sql = """
        INSERT INTO lemma(site_id, lemma, frequency) VALUES(?, ?, 1)
        ON DUPLICATE KEY UPDATE frequency = frequency + 1""";
    getSublistStream(indexes).forEach(indexSublist -> {
      if (IndexationStatus.isNotCanceled()) {
        jdbcTemplate.batchUpdate(sql,
            new LemmaBatchPreparedStatementSetter((List<Index>) indexSublist, siteId));
      }
    });
  }

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public void addIndexes(List<Index> indexes, int siteId)
      throws ExecutionException, InterruptedException {
    String sql = """
        INSERT INTO `index`(page_id, lemma_id, `rank`) VALUES(
        (SELECT id FROM page WHERE site_id=? AND path=?),
        (SELECT id FROM lemma WHERE site_id=? AND lemma=?),
        ?)""";
    @SuppressWarnings("unchecked")
    CompletableFuture<?>[] futures = getSublistStream(indexes)
        .map(indexSublist -> CompletableFuture.runAsync(
            () -> {
              if (IndexationStatus.isNotCanceled()) {
                jdbcTemplate.batchUpdate(sql,
                    new IndexBatchPreparedStatementSetter((List<Index>) indexSublist, siteId));
              }
            }, executor))
        .toArray(CompletableFuture[]::new);

    CompletableFuture<Void> run = CompletableFuture.allOf(futures);
    run.get();
  }

  private Stream<?> getSublistStream(List<?> list) {
    final AtomicInteger sublist = new AtomicInteger();
    return list.stream()
        .collect(Collectors.groupingBy(t -> sublist.getAndIncrement() / batchSize))
        .values()
        .stream();
  }

  public List<Field> getFieldsList() {
    return jdbcTemplate.query("SELECT selector, weight FROM field",
        (resultSet, i) -> Field.builder()
            .selector(resultSet.getString("selector"))
            .weight(resultSet.getFloat("weight"))
            .build());
  }

  public List<Lemma> getLemmas(List<String> lemmas, int siteId) {
    String inSql = String.join(",", Collections.nCopies(lemmas.size(), "?"));
    String sql = String.format("""
        SELECT id, lemma, frequency FROM lemma
        WHERE lemma IN(%s) AND site_id = ? ORDER BY frequency
        """, inSql);
    lemmas.add(String.valueOf(siteId));
    return jdbcTemplate.query(sql, (resultSet, i) -> Lemma.builder()
        .id(resultSet.getInt("id"))
        .lemma(resultSet.getString("lemma"))
        .frequency(resultSet.getInt("frequency"))
        .build(), lemmas.toArray());
  }

  public List<Page> getInitialPageListByRarestLemma(Lemma rarestLemma) {
    String sqlStep1 = """
        SELECT i.page_id, l.lemma, i.rank FROM `index` i
        JOIN lemma l ON  l.id = i.lemma_id
        WHERE i.page_id IN (SELECT page_id FROM `index` WHERE lemma_id=?)""";
    HashMap<Integer, HashMap<String, Float>> lemmaListsByPageId = jdbcTemplate.query(sqlStep1,
        new LemmaListsOnPageExtractor(), rarestLemma.getId());

    assert lemmaListsByPageId != null;
    List<Integer> ids = new ArrayList<>(lemmaListsByPageId.keySet());
    String inSql = String.join(",", Collections.nCopies(ids.size(), "?"));
    String sqlStep2 = String.format("""
        SELECT p.id, s.url, s.name, p.path, p.code, p.content FROM page p
        JOIN site s ON s.id=p.site_id
        WHERE p.id IN(%s)
        """, inSql);
    return jdbcTemplate.query(sqlStep2, (resultSet, i) -> {
      int pageId = resultSet.getInt("id");
      return Page.builder()
          .site(resultSet.getString("url"))
          .siteName(resultSet.getString("name"))
          .path(resultSet.getString("path"))
          .code(resultSet.getInt("code"))
          .content(resultSet.getString("content"))
          .lemmas(lemmaListsByPageId.get(pageId))
          .build();
    }, ids.toArray());
  }

  public int addSite(Site site) {
    jdbcTemplate.update("DELETE FROM site WHERE url=?", site.getUrl());

    KeyHolder keyHolder = new GeneratedKeyHolder();
    String sql = "INSERT INTO site(status, status_time, url, name) VALUES(?, ?, ?, ?)";
    jdbcTemplate.update(con -> {
      PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
      ps.setString(1, SiteStatus.INDEXING.getStatus());
      ps.setTimestamp(2, new Timestamp(System.currentTimeMillis()));
      ps.setString(3, site.getUrl());
      ps.setString(4, site.getName());
      return ps;
    }, keyHolder);
    return Objects.requireNonNull(keyHolder.getKey()).intValue();
  }

  public List<Site> getSites() {
    String sql = "SELECT * FROM site";
    return jdbcTemplate.query(sql, (resultSet, i) -> Site.builder()
        .id(resultSet.getInt("id"))
        .url(resultSet.getString("url"))
        .name(resultSet.getString("name"))
        .build());
  }

  @SuppressWarnings("ConstantConditions")
  public int defineSiteId(String mySite) {
    int resultId;
    try {
      String sql = "SELECT id FROM site WHERE url LIKE CONCAT('%', ?, '%')";
      resultId = jdbcTemplate.queryForObject(sql, Integer.class, mySite);
    } catch (EmptyResultDataAccessException e) {
      return -1;
    }
    return resultId;
  }

  public void updateSiteStatus(int siteId, String status) {
    String sql = "UPDATE site SET status=?, status_time=? WHERE id=?";
    jdbcTemplate.update(con -> {
      PreparedStatement ps = con.prepareStatement(sql);
      ps.setString(1, status);
      ps.setTimestamp(2, new Timestamp(System.currentTimeMillis()));
      ps.setInt(3, siteId);
      return ps;
    });
  }

  public void updateSiteStatus(int siteId, String status, String errorMessage) {
    String sql = "UPDATE site SET status=?, status_time=?, last_error=? WHERE id=?";
    jdbcTemplate.update(con -> {
      PreparedStatement ps = con.prepareStatement(sql);
      ps.setString(1, status);
      ps.setTimestamp(2, new Timestamp(System.currentTimeMillis()));
      ps.setString(3, errorMessage);
      ps.setInt(4, siteId);
      return ps;
    });
  }

  public boolean checkSiteStatus() {
    String sql = "SELECT status FROM site";
    List<Map<String, Object>> resultMaps = jdbcTemplate.queryForList(sql);
    for (Map<String, Object> map : resultMaps) {
      if (!map.get("status").equals("INDEXED")) {
        return false;
      }
    }
    return true;
  }

  public boolean checkSiteStatus(String site) {
    String sql = "SELECT status FROM site WHERE url=?";
    Map<String, Object> resultMap = jdbcTemplate.queryForMap(sql, site);
    return resultMap.get("status").equals("INDEXED");
  }

  public Map<String, Object> getStatistic() {
    String sql = """
        SELECT * FROM site s
        JOIN (SELECT site_id, count(*) page_count FROM page
        GROUP BY site_id) temp1 ON temp1.site_id = s.id
        JOIN (SELECT site_id, count(*) lemma_count FROM lemma
        GROUP BY site_id) temp2 ON temp2.site_id = s.id""";
    return new StatisticConstructor(jdbcTemplate.queryForList(sql)).constructStatistic();
  }

}
