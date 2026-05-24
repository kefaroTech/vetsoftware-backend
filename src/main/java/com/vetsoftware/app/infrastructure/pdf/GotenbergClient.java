package com.vetsoftware.app.infrastructure.pdf;

import java.nio.charset.StandardCharsets;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Component
public class GotenbergClient {

    private final RestClient restClient;

    public GotenbergClient(@Qualifier("gotenbergRestClient") RestClient restClient) {
        this.restClient = restClient;
    }

    public byte[] convertHtmlToPdf(String html, PdfOptions options) {
        ByteArrayResource indexHtml =
                new ByteArrayResource(html.getBytes(StandardCharsets.UTF_8)) {
                    @Override
                    public String getFilename() {
                        return "index.html";
                    }
                };

        MultiValueMap<String, Object> form = new LinkedMultiValueMap<>();
        form.add("files", indexHtml);
        form.add("paperWidth", options.paperWidth());
        form.add("paperHeight", options.paperHeight());
        form.add("marginTop", options.margin());
        form.add("marginBottom", options.margin());
        form.add("marginLeft", options.margin());
        form.add("marginRight", options.margin());
        form.add("printBackground", String.valueOf(options.printBackground()));
        form.add("preferCssPageSize", String.valueOf(options.preferCssPageSize()));

        try {
            byte[] pdf = restClient
                    .post()
                    .uri("/forms/chromium/convert/html")
                    .contentType(MediaType.MULTIPART_FORM_DATA)
                    .body(form)
                    .retrieve()
                    .body(byte[].class);
            if (pdf == null || pdf.length == 0) {
                throw new PdfRenderException("Gotenberg returned empty PDF");
            }
            return pdf;
        } catch (RestClientException e) {
            throw new PdfRenderException(
                    "Failed to render PDF via Gotenberg: " + e.getMessage(), e);
        }
    }
}
