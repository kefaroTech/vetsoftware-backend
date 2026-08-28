package com.vetsoftware.app.entitlement.application.usecase;

import static com.vetsoftware.app.entitlement.testsupport.EntitlementMother.COMPANY_ID;
import static com.vetsoftware.app.entitlement.testsupport.EntitlementMother.SUBSCRIPTION_ID;
import static com.vetsoftware.app.entitlement.testsupport.EntitlementMother.historiaClinica;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.entitlement.application.dto.CompanyAccessDto;
import com.vetsoftware.app.entitlement.application.port.out.CompanyCapacityRepository;
import com.vetsoftware.app.entitlement.application.port.out.CompanyEntitlementRepository;
import com.vetsoftware.app.entitlement.domain.AccessLevel;
import com.vetsoftware.app.entitlement.domain.CompanyEntitlement;
import com.vetsoftware.app.entitlement.domain.EntitlementSource;
import com.vetsoftware.app.infrastructure.config.ClockConfig;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Sujeta el arreglo de D-81 en esta feature: el bean de reloj declara
 * {@link ClockConfig#BUSINESS_ZONE} y no {@code systemDefaultZone()}.
 *
 * <p>
 * {@code FindCompanyAccessService} deriva {@code LocalDateTime.now(clock)} —una
 * lectura de reloj de pared, sin zona— y con ella pregunta
 * {@code CompanyEntitlement.grantsAt(now)}. Las ventanas las construye
 * {@code EntitlementCalculator} con {@code effectiveFrom().atStartOfDay()}: un
 * permiso vigente desde el dia D vale desde las <b>00:00 locales</b> del dia D.
 * Por eso <b>la zona del reloj es la que decide</b> si un permiso ya empezo, y
 * en UTC empieza cinco horas antes de tiempo.
 *
 * <p>
 * El escenario que fija: «un permiso con inicio manana no se concede esta
 * tarde». Los tres casos comparten <b>un unico instante</b>
 * ({@link #TARDE_DEL_DIA_D}) y lo unico que cambia entre ellos es la zona, para
 * que ninguno pueda pasar por casualidad.
 *
 * <p>
 * <b>Aviso para quien lo lea despues</b>: el caso de {@link RegresionZonaUtc}
 * afirma <i>a proposito</i> el comportamiento equivocado —el acceso regalado—
 * porque es la unica forma de demostrar que la zona correcta es la que lo
 * impide. No es un test que haya que «arreglar»: si algun dia deja de conceder
 * el permiso en UTC, es que el filtro de vigencia dejo de depender del reloj de
 * pared y este test sobra entero.
 *
 * <p>
 * Deliberadamente <b>no</b> usa {@code EntitlementMother.relojFijo()}: ese
 * fixture esta anclado a {@code ZoneOffset.UTC} y usarlo aqui asumiria justo lo
 * que hay que comprobar.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("FindCompanyAccessService — la zona del reloj decide cuando empieza un permiso (D-81)")
class FindCompanyAccessServiceZoneTest {

    /** 30 de septiembre: la tarde en la que el defecto regalaba el acceso. */
    private static final LocalDate DIA_D = LocalDate.of(2026, 9, 30);

    /** 1 de octubre: el dia en el que el permiso empieza de verdad. */
    private static final LocalDate DIA_D_MAS_1 = DIA_D.plusDays(1);

    /**
     * Las 19:30 del dia D en hora de Bogota. Ese mismo instante, leido en UTC, ya
     * es el dia D+1 a las 00:30: ahi vive el defecto.
     */
    private static final Instant TARDE_DEL_DIA_D = ZonedDateTime
            .of(DIA_D, LocalTime.of(19, 30), ClockConfig.BUSINESS_ZONE).toInstant();

    @Mock
    private CompanyEntitlementRepository entitlementRepository;
    @Mock
    private CompanyCapacityRepository capacityRepository;

    /**
     * Permiso cuya ventana arranca a las 00:00 <b>locales</b> del dia D+1, tal como
     * la deja {@code EntitlementCalculator} con {@code atStartOfDay()}.
     */
    private static CompanyEntitlement permisoQueEmpiezaElDiaSiguiente() {
        return new CompanyEntitlement(1L, COMPANY_ID, historiaClinica(), AccessLevel.FULL,
                EntitlementSource.SUBSCRIPTION, SUBSCRIPTION_ID, 900L, DIA_D_MAS_1.atStartOfDay(),
                null, DIA_D.atStartOfDay(), DIA_D.atStartOfDay());
    }

    /** Consulta el acceso de la empresa con el reloj dado. */
    private CompanyAccessDto accesoCon(Clock reloj) {
        when(entitlementRepository.findAllByCompanyId(COMPANY_ID))
                .thenReturn(List.of(permisoQueEmpiezaElDiaSiguiente()));
        return new FindCompanyAccessService(entitlementRepository, capacityRepository, reloj)
                .findByCompanyId(COMPANY_ID);
    }

    @Nested
    @DisplayName("con la zona del negocio")
    class ZonaDelNegocio {

        @Test
        @DisplayName("un permiso con inicio manana no se concede esta tarde")
        void no_concede_esta_tarde_un_permiso_que_empieza_manana() {
            Clock reloj = Clock.fixed(TARDE_DEL_DIA_D, ClockConfig.BUSINESS_ZONE);

            CompanyAccessDto acceso = accesoCon(reloj);

            assertThat(LocalDateTime.now(reloj)).as("el reloj de pared que ve el service")
                    .isEqualTo(DIA_D.atTime(19, 30));
            assertThat(acceso.entitlements())
                    .as("la ventana abre el %s a las 00:00 y aun no ha llegado", DIA_D_MAS_1)
                    .isEmpty();
        }

        @Test
        @DisplayName("pasada la medianoche local, a las 00:01 del dia siguiente, ya se concede")
        void concede_el_permiso_al_minuto_de_abrirse_la_ventana() {
            Clock reloj = Clock.fixed(ZonedDateTime
                    .of(DIA_D_MAS_1, LocalTime.of(0, 1), ClockConfig.BUSINESS_ZONE).toInstant(),
                    ClockConfig.BUSINESS_ZONE);

            CompanyAccessDto acceso = accesoCon(reloj);

            assertThat(LocalDateTime.now(reloj)).as("el reloj de pared que ve el service")
                    .isEqualTo(DIA_D_MAS_1.atTime(0, 1));
            assertThat(acceso.entitlements()).extracting(dto -> dto.subModule().code())
                    .containsExactly("CLINICAL_HISTORY");
        }
    }

    @Nested
    @DisplayName("con el reloj sin zona que tenia produccion (D-81)")
    class RegresionZonaUtc {

        /**
         * El defecto en una linea: el <b>mismo</b> instante de la tarde del dia D,
         * leido en UTC, ya cae en el dia D+1, la ventana parece abierta y la empresa
         * entra cinco horas antes de tiempo. Esto es lo que vuelve si alguien devuelve
         * el bean a {@code Clock.systemDefaultZone()} sobre una imagen que no declara
         * zona.
         */
        @Test
        @DisplayName("el mismo instante leido en UTC regala el acceso cinco horas antes")
        void el_mismo_instante_en_utc_regala_el_acceso_antes_de_tiempo() {
            Clock relojSinZona = Clock.fixed(TARDE_DEL_DIA_D, ZoneOffset.UTC);

            CompanyAccessDto acceso = accesoCon(relojSinZona);

            assertThat(relojSinZona.instant()).as("es el mismo instante, solo cambia la zona")
                    .isEqualTo(TARDE_DEL_DIA_D);
            assertThat(LocalDateTime.now(relojSinZona)).as("en UTC «hoy» ya es manana")
                    .isEqualTo(DIA_D_MAS_1.atTime(0, 30));
            assertThat(acceso.entitlements()).extracting(dto -> dto.subModule().code())
                    .as("acceso concedido antes de tiempo: exactamente el defecto D-81")
                    .containsExactly("CLINICAL_HISTORY");
        }
    }
}
