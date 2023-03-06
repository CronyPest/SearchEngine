package searchEngine.service.morphology;

import java.util.HashMap;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.lucene.morphology.LuceneMorphology;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class Lemmatizer {

  private final LuceneMorphology luceneMorphology;
  private final String wordRegex = "[А-Яа-я]+";

  @Autowired
  public Lemmatizer(LuceneMorphology luceneMorphology) {
    this.luceneMorphology = luceneMorphology;
  }

  public HashMap<String, Integer> getLemmaList(String text) {
    HashMap<String, Integer> result = new HashMap<>();
    splitToWords(text).stream().map(String::toLowerCase).forEach(word -> {
      if (word.matches(wordRegex)) {
        String lemma = luceneMorphology.getNormalForms(word).get(0);
        List<String> lemmaInfo = luceneMorphology.getMorphInfo(word);
        boolean correctLemma = true;
        for (String info : lemmaInfo) {
          if (!isValid(info)) {
            correctLemma = false;
            break;
          }
        }
        word = correctLemma ? lemma : "";
      }
      if (!word.isEmpty()) {
        Integer count = result.get(word);
        result.put(word, (count == null) ? 1 : count + 1);
      }
    });
    return result;
  }


  public String getSnippet(String text, String lemmaToFind) {
    String foundWord;
    List<String> words = splitToWords(text);
    if (lemmaToFind.matches(wordRegex)) {
      foundWord = words.stream().filter(w -> w.matches(wordRegex))
          .filter(w -> luceneMorphology.getNormalForms(w.toLowerCase()).get(0).equals(lemmaToFind))
          .findFirst().orElseThrow();
    } else {
      foundWord = words.stream().filter(w -> w.toLowerCase().equals(lemmaToFind))
          .findFirst().orElseThrow();
    }
    return buildSnippet(text, foundWord);
  }

  private List<String> splitToWords(String text) {
    return Stream.of(text.split("[^А-Яа-яA-Za-z\\d]+"))
        .collect(Collectors.toList());
  }

  //  |o - междометие, |l - предлог, |n - союз, |p - честица, |e - местоимение, |f - местоимение-прилагательное, |j - наречие, |k - предикатив
  private boolean isValid(String s) {
    return Stream.of("|o", "|l", "|n", "|p", "|e", "|f", "|j", "|k").noneMatch(s::contains);
  }

  private String buildSnippet(String text, String word) {
    StringBuilder sb = new StringBuilder();
    Pattern pattern = Pattern.compile("(.{0,50}" + word + ".{0,50})");
    Matcher matcher = pattern.matcher(text);
    if (matcher.find()) {
      String regex = "(.+)(" + word + ")(.+)";
      sb.append("...")
          .append(matcher.group().replaceAll(regex, "$1"))
          .append("<b>")
          .append(matcher.group().replaceAll(regex, "$2"))
          .append("</b>")
          .append(matcher.group().replaceAll(regex, "$3"))
          .append("...");
    }
    return sb.toString();
  }
}
