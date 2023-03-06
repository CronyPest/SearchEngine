package searchEngine.config;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import javax.sql.DataSource;
import org.apache.lucene.morphology.LuceneMorphology;
import org.apache.lucene.morphology.russian.RussianLuceneMorphology;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.core.task.TaskExecutor;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;

@Configuration
public class Beans {

  @Bean
  public DataSourceTransactionManager transactionManager(DataSource dataSource) {
    DataSourceTransactionManager transactionManager = new DataSourceTransactionManager();
    transactionManager.setDataSource(dataSource);
    return transactionManager;
  }

  @Bean
  public LuceneMorphology luceneMorphology() throws IOException {
    return new RussianLuceneMorphology();
  }

  @Bean
  ExecutorService executorService() {
    return Executors.newFixedThreadPool(10);
  }

  @Bean
  TaskExecutor taskExecutor() {
    return new SimpleAsyncTaskExecutor();
  }
}
