package searchEngine.config;

import java.util.List;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import searchEngine.model.Site;

@Data
@Component
@ConfigurationProperties(prefix = "available-sites-list")
public class AvailableSitesConfiguration {

  private List<Site> sites;
}
