package com.vetsoftware.app.openaccount.application.port.out;

/**
 * Puerto de salida: al cerrar/cobrar una cuenta se emite su documento electrónico (venta). Lo
 * implementa un adaptador de la feature electronicdocument, manteniendo la dirección de dependencia
 * electronicdocument → openaccount (openaccount solo conoce esta interfaz, no la otra feature).
 *
 * <p>Es una llamada directa y síncrona dentro de la transacción del cierre: la emisión ve la cuenta
 * ya CLOSE y el documento se guarda atómicamente con el cierre. Si la emisión no puede completarse
 * (p. ej. la empresa no tiene perfil fiscal), el cierre falla con ese error en vez de cerrar sin
 * documento.
 */
public interface ClosedAccountEmissionPort {

  /**
   * @param documentType "DOC_EQUIV_POS" o "FE_VENTA"; null/blanco → DOC_EQUIV_POS.
   */
  void emitForClosedAccount(
      Long openAccountId, Long companyId, String documentType, boolean finalConsumer);
}
