package com.vetsoftware.app.subscriptionpayment.infrastructure.web;

import com.vetsoftware.app.infrastructure.web.PageResponse;
import com.vetsoftware.app.subscriptionpayment.application.port.in.ExportSubscriptionPaymentsUseCase;
import com.vetsoftware.app.subscriptionpayment.application.port.in.ListAllSubscriptionPaymentsUseCase;
import com.vetsoftware.app.subscriptionpayment.application.query.ListAllSubscriptionPaymentsQuery;
import com.vetsoftware.app.subscriptionpayment.domain.SubscriptionPaymentStatus;
import com.vetsoftware.app.subscriptionpayment.infrastructure.web.response.SubscriptionPaymentResponse;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Tesorería cross-tenant de la plataforma; no expone mutaciones tenant. */
@RestController
@RequestMapping("/system/subscription-payments")
public class SystemSubscriptionPaymentController {

    private static final MediaType CSV = new MediaType("text", "csv", StandardCharsets.UTF_8);

    private final ListAllSubscriptionPaymentsUseCase listUseCase;
    private final ExportSubscriptionPaymentsUseCase exportUseCase;

    public SystemSubscriptionPaymentController(ListAllSubscriptionPaymentsUseCase listUseCase,
            ExportSubscriptionPaymentsUseCase exportUseCase) {
        this.listUseCase = listUseCase;
        this.exportUseCase = exportUseCase;
    }

    @GetMapping
    public PageResponse<SubscriptionPaymentResponse> listAll(
            @RequestParam(required = false) Long companyId,
            @RequestParam(required = false) SubscriptionPaymentStatus status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime receivedFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime receivedTo,
            @RequestParam(required = false) Integer pendingOlderThanMinutes,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int pageSize) {
        return PageResponse.from(
                listUseCase.listAll(new ListAllSubscriptionPaymentsQuery(companyId, status,
                        receivedFrom, receivedTo, pendingOlderThanMinutes, page, pageSize)),
                SubscriptionPaymentResponse::from);
    }

    @GetMapping("/export")
    public ResponseEntity<byte[]> export(@RequestParam(required = false) Long companyId,
            @RequestParam(required = false) SubscriptionPaymentStatus status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime receivedFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime receivedTo,
            @RequestParam(required = false) Integer pendingOlderThanMinutes) {
        byte[] body = SubscriptionPaymentCsv
                .rows(exportUseCase.export(new ListAllSubscriptionPaymentsQuery(companyId, status,
                        receivedFrom, receivedTo, pendingOlderThanMinutes, 0, 0)));
        return file(body, "subscription_payments.csv");
    }

    private static ResponseEntity<byte[]> file(byte[] body, String filename) {
        String encoded = URLEncoder.encode(filename, StandardCharsets.UTF_8).replace("+", "%20");
        return ResponseEntity.ok().contentType(CSV)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + filename + "\"; filename*=UTF-8''" + encoded)
                .body(body);
    }
}
