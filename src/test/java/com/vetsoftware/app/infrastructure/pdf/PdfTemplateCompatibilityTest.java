package com.vetsoftware.app.infrastructure.pdf;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

class PdfTemplateCompatibilityTest {

  private static final List<String> TEMPLATES =
      List.of(
          "cash-arqueo",
          "clinical-history",
          "electronic-invoice",
          "inventory-kardex",
          "inventory-purchases",
          "prescription",
          "purchase-book");

  private static final Pattern UNSUPPORTED_LAYOUT =
      Pattern.compile(
          "display\\s*:\\s*(?:flex|grid)|\\bflex(?:-[a-z]+)?\\s*:|\\bgap\\s*:",
          Pattern.CASE_INSENSITIVE);

  @Test
  void templatesAvoidUnsupportedFlexboxAndGridLayouts() throws IOException {
    for (String template : TEMPLATES) {
      String html = readTemplate(template);

      assertThat(UNSUPPORTED_LAYOUT.matcher(html).find())
          .as("CSS no compatible en pdf/%s.html", template)
          .isFalse();
      assertThat(html)
          .as("Declaración UTF-8 en pdf/%s.html", template)
          .containsIgnoringCase("<meta charset=\"UTF-8\"");
    }
  }

  private static String readTemplate(String template) throws IOException {
    var resource = new ClassPathResource("templates/pdf/" + template + ".html");
    return resource.getContentAsString(StandardCharsets.UTF_8);
  }
}
