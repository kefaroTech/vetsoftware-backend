package com.vetsoftware.app.infrastructure.pdf;

import java.util.Locale;
import java.util.Map;
import org.springframework.stereotype.Component;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

@Component
public class HtmlPdfRenderer {

    private final TemplateEngine templateEngine;
    private final GotenbergClient gotenbergClient;

    public HtmlPdfRenderer(TemplateEngine templateEngine, GotenbergClient gotenbergClient) {
        this.templateEngine = templateEngine;
        this.gotenbergClient = gotenbergClient;
    }

    public byte[] render(String templateName, Map<String, Object> model, PdfOptions options) {
        Context ctx = new Context(Locale.forLanguageTag("es"));
        ctx.setVariables(model);
        String html = templateEngine.process("pdf/" + templateName, ctx);
        return gotenbergClient.convertHtmlToPdf(html, options);
    }
}
