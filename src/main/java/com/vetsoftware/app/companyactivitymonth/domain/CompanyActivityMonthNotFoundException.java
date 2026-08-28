package com.vetsoftware.app.companyactivitymonth.domain;

/**
 * No hay fila de actividad para ese identificador, o para ese par empresa-mes.
 *
 * <p>
 * <strong>Ausencia no es cero.</strong> Una empresa sin fila en un mes no es
 * una empresa que no entro ninguna vez: es una empresa cuya actividad <em>nadie
 * calculo todavia</em>. La distincion importa porque el barrido de dormidos
 * ({@code ListDormantCompaniesUseCase}) se apoya en filas escritas: si el
 * proceso que las genera no corrio, la lista de dormidos sale vacia y parece
 * que todo el mundo esta activo. Leer la ausencia como cero convertiria ese
 * silencio en un informe con numeros.
 */
public class CompanyActivityMonthNotFoundException extends RuntimeException {

    public CompanyActivityMonthNotFoundException(Long id) {
        super("Company activity month not found: " + id);
    }

    public CompanyActivityMonthNotFoundException(Long companyId, String periodKey) {
        super("Company " + companyId + " has no activity row for period " + periodKey
                + ": an absent row means the month was never computed, not that the company"
                + " never logged in");
    }
}
