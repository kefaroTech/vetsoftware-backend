package com.vetsoftware.app.pricelist.application.usecase;

import static com.vetsoftware.app.pricelist.testsupport.PublicPlanMother.HOY;
import static com.vetsoftware.app.pricelist.testsupport.PublicPlanMother.PLAN;
import static com.vetsoftware.app.pricelist.testsupport.PublicPlanMother.TARIFA_VIGENTE_ID;
import static com.vetsoftware.app.pricelist.testsupport.PublicPlanMother.contador;
import static com.vetsoftware.app.pricelist.testsupport.PublicPlanMother.contadorSoloMensual;
import static com.vetsoftware.app.pricelist.testsupport.PublicPlanMother.moduloConPrueba;
import static com.vetsoftware.app.pricelist.testsupport.PublicPlanMother.moduloSinPrueba;
import static com.vetsoftware.app.pricelist.testsupport.PublicPlanMother.plan;
import static com.vetsoftware.app.pricelist.testsupport.PublicPlanMother.tarifa;
import static com.vetsoftware.app.pricelist.testsupport.PublicPlanMother.tarifaCaducada;
import static com.vetsoftware.app.pricelist.testsupport.PublicPlanMother.tarifaFutura;
import static com.vetsoftware.app.pricelist.testsupport.PublicPlanMother.tarifaVigente;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.infrastructure.config.ClockConfig;
import com.vetsoftware.app.pricelist.application.dto.PublicPlanCapacityDto;
import com.vetsoftware.app.pricelist.application.dto.PublicPlanCatalogDto;
import com.vetsoftware.app.pricelist.application.dto.PublicPlanComponentRowDto;
import com.vetsoftware.app.pricelist.application.dto.PublicPlanDto;
import com.vetsoftware.app.pricelist.application.dto.PublicPlanIncludedDto;
import com.vetsoftware.app.pricelist.application.port.out.PublicPlanQueryPort;
import java.lang.reflect.RecordComponent;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZonedDateTime;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * El caso de uso que compone la portada. Lo que se fija aqui es <b>que decide
 * el servidor y que no llega a salir</b>, porque esta respuesta la lee
 * cualquiera sin autenticarse.
 *
 * <p>
 * Tres cosas, en este orden de importancia:
 *
 * <ol>
 * <li><b>Cual es la tarifa vigente.</b> No la decide el motor de base de datos
 * —el puerto devuelve todas las publicadas, caducadas incluidas— sino
 * {@code PriceListValidity} sobre el dia derivado del {@link Clock} inyectado,
 * que es el unico que lleva la zona del negocio (D-81). {@link ZonaDelReloj} es
 * el chivato.</li>
 * <li><b>El desempate es determinista.</b> Nada impide dos ventanas solapadas
 * en el esquema; gana el {@code validFrom} mas reciente y, a igualdad, el id
 * mayor. «La primera que devuelva la consulta» seria un precio distinto segun
 * el plan de ejecucion.</li>
 * <li><b>El recorte.</b> El {@code id} y el {@code validTo} de la tarifa llegan
 * hasta este servicio y <b>mueren aqui</b>: {@link PublicPlanCatalogDto} no los
 * declara. {@link Recorte} lo fija por reflexion, que es lo que se pone rojo el
 * dia que alguien anada un campo.</li>
 * </ol>
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("GetPublicPlansService — el catalogo publico, y lo que no publica")
class GetPublicPlansServiceTest {

    /** Las 10:30 del 2026-08-28 en la zona del negocio. */
    private static final Clock RELOJ = Clock.fixed(
            ZonedDateTime.of(HOY, LocalTime.of(10, 30), ClockConfig.BUSINESS_ZONE).toInstant(),
            ClockConfig.BUSINESS_ZONE);

    @Mock
    private PublicPlanQueryPort queryPort;

    private GetPublicPlansService servicio(Clock reloj) {
        return new GetPublicPlansService(queryPort, reloj);
    }

    /** Un solo plan tarifado, con las lineas que se le pasen. */
    private void catalogoDe(Long tarifaId, List<PublicPlanComponentRowDto> lineas) {
        when(queryPort.findPlans(tarifaId)).thenReturn(List.of(plan()));
        when(queryPort.findPlanComponents(tarifaId)).thenReturn(lineas);
    }

    @Nested
    @DisplayName("Que tarifa manda")
    class TarifaVigente {

        @Test
        @DisplayName("de las publicadas elige la vigente hoy y descarta la caducada y la futura")
        void elige_la_vigente_hoy() {
            when(queryPort.findPublishedPriceLists())
                    .thenReturn(List.of(tarifaCaducada(), tarifaVigente(), tarifaFutura()));
            catalogoDe(TARIFA_VIGENTE_ID, List.of(moduloConPrueba(PLAN)));

            assertThat(servicio(RELOJ).get().priceValidFrom()).isEqualTo(LocalDate.of(2026, 8, 1));
        }

        /**
         * Dos ventanas solapadas son legales en el esquema: nada lo impide. Lo que no
         * puede ser es que el precio publicado dependa del orden en que la consulta
         * devuelva las filas.
         */
        @Test
        @DisplayName("con dos vigentes solapadas gana la de validFrom mas reciente")
        void con_dos_vigentes_gana_la_de_valid_from_mas_reciente() {
            when(queryPort.findPublishedPriceLists()).thenReturn(
                    List.of(tarifa(900L, LocalDate.of(2026, 1, 1), null), tarifaVigente()));
            catalogoDe(TARIFA_VIGENTE_ID, List.of(moduloConPrueba(PLAN)));

            assertThat(servicio(RELOJ).get().priceValidFrom()).isEqualTo(LocalDate.of(2026, 8, 1));
        }

        @Test
        @DisplayName("a igualdad de validFrom gana el id mayor: la ultima publicada")
        void a_igualdad_de_valid_from_gana_el_id_mayor() {
            when(queryPort.findPublishedPriceLists())
                    .thenReturn(List.of(tarifa(TARIFA_VIGENTE_ID, LocalDate.of(2026, 8, 1), null),
                            tarifa(900L, LocalDate.of(2026, 8, 1), null)));
            when(queryPort.findPlans(900L)).thenReturn(List.of(plan()));
            when(queryPort.findPlanComponents(900L)).thenReturn(List.of(moduloConPrueba(PLAN)));

            assertThat(servicio(RELOJ).get().plans()).hasSize(1);
        }

        /**
         * <b>El caso vacio es correcto, no un fallo.</b> Sin tarifa vigente la portada
         * tiene que seguir cargando, asi que devuelve el catalogo vacio — y ni siquiera
         * pregunta por planes ni por lineas, que es lo que impide que un endpoint
         * anonimo gaste dos consultas para no publicar nada.
         */
        @Test
        @DisplayName("sin tarifa vigente devuelve el catalogo vacio y no consulta planes ni lineas")
        void sin_tarifa_vigente_devuelve_el_catalogo_vacio() {
            when(queryPort.findPublishedPriceLists())
                    .thenReturn(List.of(tarifaCaducada(), tarifaFutura()));

            assertThat(servicio(RELOJ).get())
                    .isEqualTo(new PublicPlanCatalogDto(null, null, List.of()));

            verify(queryPort, never()).findPlans(any());
            verify(queryPort, never()).findPlanComponents(any());
        }

        @Test
        @DisplayName("sin ninguna tarifa publicada devuelve el catalogo vacio")
        void sin_ninguna_tarifa_publicada_devuelve_el_catalogo_vacio() {
            when(queryPort.findPublishedPriceLists()).thenReturn(List.of());

            assertThat(servicio(RELOJ).get())
                    .isEqualTo(new PublicPlanCatalogDto(null, null, List.of()));

            verify(queryPort, never()).findPlans(any());
            verify(queryPort, never()).findPlanComponents(any());
        }
    }

    @Nested
    @DisplayName("La zona del reloj decide el dia (D-81)")
    class ZonaDelReloj {

        /**
         * Las 19:30 del ultimo dia de la tarifa en Bogota son ya el dia siguiente en
         * horario universal. Con la zona del negocio la portada sigue publicando
         * precios; con un {@code CURRENT_DATE} del motor —o un reloj en UTC— se
         * quedaria sin ellos media tarde antes de tiempo.
         */
        @Test
        @DisplayName("a las 19:30 del ultimo dia de vigencia la tarifa sigue publicandose")
        void a_las_19_30_del_ultimo_dia_la_tarifa_sigue_publicandose() {
            LocalDate ultimoDia = LocalDate.of(2026, 8, 28);
            Clock alFilo = Clock.fixed(ZonedDateTime
                    .of(ultimoDia, LocalTime.of(19, 30), ClockConfig.BUSINESS_ZONE).toInstant(),
                    ClockConfig.BUSINESS_ZONE);
            when(queryPort.findPublishedPriceLists()).thenReturn(
                    List.of(tarifa(TARIFA_VIGENTE_ID, LocalDate.of(2026, 8, 1), ultimoDia)));
            catalogoDe(TARIFA_VIGENTE_ID, List.of(moduloConPrueba(PLAN)));

            assertThat(servicio(alFilo).get().plans()).hasSize(1);
        }
    }

    @Nested
    @DisplayName("Como se arma cada plan")
    class Composicion {

        @Test
        @DisplayName("separa modulos y contadores por capacityUnit, y conserva el nulo de la"
                + " prueba")
        void separa_modulos_y_contadores() {
            when(queryPort.findPublishedPriceLists()).thenReturn(List.of(tarifaVigente()));
            catalogoDe(TARIFA_VIGENTE_ID,
                    List.of(moduloConPrueba(PLAN), moduloSinPrueba(PLAN), contador(PLAN)));

            PublicPlanDto publicado = servicio(RELOJ).get().plans().get(0);

            assertThat(publicado.includes()).containsExactly(
                    new PublicPlanIncludedDto("AGENDA", "Agenda", 30),
                    new PublicPlanIncludedDto("CAJA", "Caja", null));
            assertThat(publicado.capacities())
                    .containsExactly(new PublicPlanCapacityDto("EXTRA_USER", "Usuario adicional",
                            "USER", 3, new BigDecimal("15000.00"), new BigDecimal("145000.00")));
        }

        /**
         * El servicio <b>copia</b> los dos importes; no calcula el anual a partir del
         * mensual. La fixture lo hace comprobable: 150.000 no es 15.000 por doce ni por
         * diez, asi que cualquier extrapolacion falla aqui en vez de llegar a la
         * landing como un precio que el servidor no va a cobrar.
         */
        @Test
        @DisplayName("el contador publica el importe de CADA ciclo, leidos de su fila, sin"
                + " extrapolar el anual desde el mensual")
        void el_contador_publica_el_importe_de_cada_ciclo() {
            when(queryPort.findPublishedPriceLists()).thenReturn(List.of(tarifaVigente()));
            catalogoDe(TARIFA_VIGENTE_ID, List.of(contador(PLAN)));

            PublicPlanCapacityDto contador = servicio(RELOJ).get().plans().get(0).capacities()
                    .get(0);

            assertThat(contador.monthlyExtraUnitAmount()).isEqualByComparingTo("15000.00");
            assertThat(contador.annualExtraUnitAmount()).isEqualByComparingTo("145000.00");
        }

        /**
         * Nulo no es «no lo sabemos»: es «ese contador no se vende suelto en ese
         * ciclo», y es la misma respuesta que dara el {@code JOIN} por ciclo de la
         * contratacion. La linea sigue saliendo porque {@code included} —lo que el plan
         * trae dentro— es cierto en los dos ciclos.
         */
        @Test
        @DisplayName("un contador sin precio anual sale igual, con el anual nulo: lo incluido es"
                + " cierto en los dos ciclos")
        void un_contador_sin_precio_anual_sale_con_el_anual_nulo() {
            when(queryPort.findPublishedPriceLists()).thenReturn(List.of(tarifaVigente()));
            catalogoDe(TARIFA_VIGENTE_ID, List.of(contadorSoloMensual(PLAN)));

            PublicPlanCapacityDto contador = servicio(RELOJ).get().plans().get(0).capacities()
                    .get(0);

            assertThat(contador.included()).isEqualTo(1);
            assertThat(contador.monthlyExtraUnitAmount()).isEqualByComparingTo("45000.00");
            assertThat(contador.annualExtraUnitAmount()).isNull();
        }

        @Test
        @DisplayName("un plan sin lineas sale con las dos colecciones vacias, no nulas")
        void un_plan_sin_lineas_sale_con_las_colecciones_vacias() {
            when(queryPort.findPublishedPriceLists()).thenReturn(List.of(tarifaVigente()));
            catalogoDe(TARIFA_VIGENTE_ID, List.of());

            PublicPlanDto publicado = servicio(RELOJ).get().plans().get(0);

            assertThat(publicado.includes()).isEmpty();
            assertThat(publicado.capacities()).isEmpty();
        }

        /**
         * {@code findPlanComponents} <b>no acota los paquetes por tarifa</b> —la lista
         * solo entra en el {@code LEFT JOIN} del precio—, asi que devuelve tambien
         * lineas de paquetes que no estan tarifados en ella. Quien las descarta es el
         * {@code getOrDefault} de este servicio, y eso es lo que fija este caso: una
         * linea huerfana no inventa un plan.
         */
        @Test
        @DisplayName("las lineas de un paquete que no esta en la tarifa se descartan")
        void las_lineas_de_un_paquete_ajeno_se_descartan() {
            when(queryPort.findPublishedPriceLists()).thenReturn(List.of(tarifaVigente()));
            when(queryPort.findPlans(TARIFA_VIGENTE_ID)).thenReturn(List.of(plan()));
            when(queryPort.findPlanComponents(TARIFA_VIGENTE_ID))
                    .thenReturn(List.of(moduloConPrueba(PLAN), moduloSinPrueba("OTRO_PACK")));

            PublicPlanCatalogDto catalogo = servicio(RELOJ).get();

            assertThat(catalogo.plans()).extracting(PublicPlanDto::code).containsExactly(PLAN);
            assertThat(catalogo.plans().get(0).includes())
                    .containsExactly(new PublicPlanIncludedDto("AGENDA", "Agenda", 30));
        }

        @Test
        @DisplayName("la moneda y el «desde» salen de la tarifa elegida, no de la primera de la"
                + " lista")
        void la_moneda_y_el_desde_salen_de_la_tarifa_elegida() {
            when(queryPort.findPublishedPriceLists())
                    .thenReturn(List.of(tarifaCaducada(), tarifaVigente()));
            catalogoDe(TARIFA_VIGENTE_ID, List.of(moduloConPrueba(PLAN)));

            PublicPlanCatalogDto catalogo = servicio(RELOJ).get();

            assertThat(catalogo.currency()).isEqualTo("COP");
            assertThat(catalogo.priceValidFrom()).isEqualTo(LocalDate.of(2026, 8, 1));
        }
    }

    @Nested
    @DisplayName("El recorte del contrato publico")
    class Recorte {

        /**
         * Aqui es donde se cortan el id y la caducidad de la tarifa: el puerto los
         * transporta —los necesitan {@code findPlans} y {@code PriceListValidity}— y
         * este record ya no los declara. Si manana alguien anade uno, este caso lo
         * nombra en vez de dejar que la landing publique cuantas tarifas hay o hasta
         * cuando dura la oferta.
         */
        @Test
        @DisplayName("el catalogo publica moneda, «desde» y planes: ni id, ni validTo, ni firma")
        void el_catalogo_no_publica_ni_id_ni_valid_to() {
            assertThat(PublicPlanCatalogDto.class.getRecordComponents())
                    .extracting(RecordComponent::getName)
                    .containsExactly("currency", "priceValidFrom", "plans");
        }

        @Test
        @DisplayName("el plan publica diez campos y ninguno es un id ni un tramo")
        void el_plan_no_publica_ni_id_ni_tramos() {
            assertThat(PublicPlanDto.class.getRecordComponents())
                    .extracting(RecordComponent::getName).containsExactly("code", "name", "tagline",
                            "monthlyFromAmount", "annualFromAmount", "setupAmount", "taxRate",
                            "taxTreatment", "includes", "capacities");
        }

        @Test
        @DisplayName("la linea incluida publica codigo, nombre y dias de prueba, y nada mas")
        void la_linea_incluida_publica_tres_campos() {
            assertThat(PublicPlanIncludedDto.class.getRecordComponents())
                    .extracting(RecordComponent::getName)
                    .containsExactly("code", "name", "trialDays");
        }

        @Test
        @DisplayName("el contador publica el rotulo del eje, no su id, y un importe POR CICLO")
        void el_contador_publica_el_rotulo_del_eje() {
            assertThat(PublicPlanCapacityDto.class.getRecordComponents())
                    .extracting(RecordComponent::getName).containsExactly("code", "name", "unit",
                            "included", "monthlyExtraUnitAmount", "annualExtraUnitAmount");
        }
    }
}
