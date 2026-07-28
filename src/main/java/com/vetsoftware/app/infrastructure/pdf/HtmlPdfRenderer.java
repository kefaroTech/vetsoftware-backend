package com.vetsoftware.app.infrastructure.pdf;

import io.micrometer.observation.annotation.Observed;
import java.util.Locale;
import java.util.Map;
import org.springframework.stereotype.Component;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

@Component
public class HtmlPdfRenderer {

    private final TemplateEngine templateEngine;
    private final HtmlToPdfEngine pdfEngine;

    public HtmlPdfRenderer(TemplateEngine templateEngine, HtmlToPdfEngine pdfEngine) {
        this.templateEngine = templateEngine;
        this.pdfEngine = pdfEngine;
    }

    @Observed(name = "pdf.render", contextualName = "render pdf")
    public byte[] render(String templateName, Map<String, Object> model) {
        Context ctx = new Context(Locale.forLanguageTag("es"));
        ctx.setVariables(model);
        String html = templateEngine.process("pdf/" + templateName, ctx);
        return pdfEngine.render(html);
    }
}
