package com.vetsoftware.app.paymentgateway.infrastructure.web;

import com.vetsoftware.app.paymentgateway.domain.WompiWebhookBodyTooLargeException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Rechaza con 413 un webhook de Wompi cuyo cuerpo supere
 * {@code vetsoftware.payments.wompi.max-event-body-bytes}.
 *
 * <p>
 * <strong>Dos defensas, no una.</strong> Con {@code Content-Length} declarado,
 * el corte es inmediato y antes de leer nada. Sin él —una transferencia por
 * {@code chunked}, que un cliente hostil puede forzar aunque Wompi no lo haga—
 * el tamaño no se conoce de antemano, así que el límite se aplica mientras se
 * lee: {@link LimitedBodyRequest} envuelve el cuerpo en un flujo que cuenta
 * bytes y lanza {@link WompiWebhookBodyTooLargeException} al superarlo. Esa
 * excepción la resuelve el {@code @ExceptionHandler} de
 * {@code GlobalExceptionHandler} dentro del propio despacho de
 * {@code DispatcherServlet} —{@code @RequestBody} se resuelve ahí, antes de
 * invocar el controller—, así que normalmente nunca vuelve a cruzar
 * {@code chain.doFilter}; el {@code catch} de este filtro es una segunda red
 * para el caso en que ese handler deje de estar registrado.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 5)
public class WompiWebhookBodySizeFilter extends OncePerRequestFilter {

    private static final String WOMPI_WEBHOOK_PATH = "/payment-gateway/wompi/events";

    private final long maxEventBodyBytes;

    public WompiWebhookBodySizeFilter(
            @Value("${vetsoftware.payments.wompi.max-event-body-bytes:65536}") long maxEventBodyBytes) {
        this.maxEventBodyBytes = maxEventBodyBytes;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !WOMPI_WEBHOOK_PATH.equals(request.getServletPath());
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
            FilterChain chain) throws ServletException, IOException {
        if (request.getContentLengthLong() > maxEventBodyBytes) {
            response.sendError(HttpStatus.PAYLOAD_TOO_LARGE.value(),
                    "El cuerpo del webhook de Wompi supera el límite permitido");
            return;
        }
        try {
            chain.doFilter(new LimitedBodyRequest(request, maxEventBodyBytes), response);
        } catch (WompiWebhookBodyTooLargeException fallo) {
            if (!response.isCommitted()) {
                response.sendError(HttpStatus.PAYLOAD_TOO_LARGE.value(), fallo.getMessage());
            }
        }
    }

    private static final class LimitedBodyRequest extends HttpServletRequestWrapper {

        private final long limit;

        LimitedBodyRequest(HttpServletRequest request, long limit) {
            super(request);
            this.limit = limit;
        }

        @Override
        public ServletInputStream getInputStream() throws IOException {
            return new LimitedServletInputStream(super.getInputStream(), limit);
        }

        @Override
        public BufferedReader getReader() throws IOException {
            String encoding = getCharacterEncoding();
            return new BufferedReader(new InputStreamReader(getInputStream(),
                    encoding != null ? encoding : StandardCharsets.UTF_8.name()));
        }
    }

    private static final class LimitedServletInputStream extends ServletInputStream {

        private final ServletInputStream delegate;
        private final long limit;
        private long read;

        LimitedServletInputStream(ServletInputStream delegate, long limit) {
            this.delegate = delegate;
            this.limit = limit;
        }

        @Override
        public int read() throws IOException {
            int value = delegate.read();
            if (value >= 0) {
                count(1);
            }
            return value;
        }

        @Override
        public int read(byte[] b, int off, int len) throws IOException {
            int n = delegate.read(b, off, len);
            if (n > 0) {
                count(n);
            }
            return n;
        }

        private void count(int n) {
            read += n;
            if (read > limit) {
                throw new WompiWebhookBodyTooLargeException(
                        "El cuerpo del webhook de Wompi supera el límite permitido");
            }
        }

        @Override
        public boolean isFinished() {
            return delegate.isFinished();
        }

        @Override
        public boolean isReady() {
            return delegate.isReady();
        }

        @Override
        public void setReadListener(ReadListener readListener) {
            delegate.setReadListener(readListener);
        }
    }
}
