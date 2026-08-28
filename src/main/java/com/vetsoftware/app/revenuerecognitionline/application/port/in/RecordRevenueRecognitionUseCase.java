package com.vetsoftware.app.revenuerecognitionline.application.port.in;

import com.vetsoftware.app.revenuerecognitionline.application.command.RecordRevenueRecognitionCommand;
import com.vetsoftware.app.revenuerecognitionline.application.dto.RevenueRecognitionLineDto;
import org.springframework.security.access.prepost.PreAuthorize;

public interface RecordRevenueRecognitionUseCase {

    /**
     * Registra un renglon de reconocimiento de ingreso, resolviendo el periodo
     * contable en el que cabe escribirlo.
     *
     * <p>
     * <strong>Este puerto NO tiene endpoint, y esa ausencia es la decision de
     * diseño de toda la rodaja.</strong> {@code revenue_recognition_lines} es un
     * <em>libro derivado</em>: cada renglon sale del prorrateo de un
     * {@code subscription_charges}, no de que alguien escriba un importe en una
     * pantalla. Un alta manual por HTTP permitiria inventar ingreso que ningun
     * cargo respalda y el libro dejaria de cuadrar contra la cartera <b>sin que
     * nada lo delate</b>: no hay constraint que pueda comprobar que un
     * reconocimiento corresponde a lo realmente devengado. Por eso solo lo alcanza
     * el proceso que factura, y por eso
     * {@code SystemRevenueRecognitionLineController} es de solo lectura.
     *
     * <p>
     * <strong>{@code hasRole('SYSTEM')} a secas</strong>, no
     * {@code @NoAuthorizationRequired}: el proceso que lo llama corre con principal
     * de plataforma, asi que hay gate real que exigir y no hay que declarar ninguna
     * excepcion. El command transporta {@code companyId}, y con este gate
     * {@code TENANT_DEFENSA_EN_PROFUNDIDAD} queda satisfecha por la via correcta —a
     * un principal SYSTEM no se le puede pedir {@code @authz.isMyCompany}, porque
     * es cross-tenant por diseño y no tiene empresa propia—.
     */
    @PreAuthorize("hasRole('SYSTEM')")
    RevenueRecognitionLineDto execute(RecordRevenueRecognitionCommand command);
}
