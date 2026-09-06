package com.vetsoftware.app.pricelist.application.usecase;

import com.vetsoftware.app.pricelist.application.dto.PublicPlanCapacityDto;
import com.vetsoftware.app.pricelist.application.dto.PublicPlanCatalogDto;
import com.vetsoftware.app.pricelist.application.dto.PublicPlanComponentRowDto;
import com.vetsoftware.app.pricelist.application.dto.PublicPlanDto;
import com.vetsoftware.app.pricelist.application.dto.PublicPlanIncludedDto;
import com.vetsoftware.app.pricelist.application.dto.PublicPlanRowDto;
import com.vetsoftware.app.pricelist.application.dto.PublicPriceListDto;
import com.vetsoftware.app.pricelist.application.dto.PublicStructuralCapacityRowDto;
import com.vetsoftware.app.pricelist.application.port.in.GetPublicPlansUseCase;
import com.vetsoftware.app.pricelist.application.port.out.PublicPlanQueryPort;
import com.vetsoftware.app.shared.pricing.PriceListValidity;
import io.micrometer.observation.annotation.Observed;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * El catalogo publico en <strong>cuatro consultas</strong>, no en una por plan.
 *
 * <p>
 * Se traen las tarifas publicadas, los paquetes de la vigente, todas sus lineas
 * y el minimo estructural, y se agrupan en memoria. Es la misma forma que
 * {@code GetPublicQuestionnaireService} y por el mismo motivo: sobre decenas de
 * filas eso es mas barato que cualquier {@code JOIN} anidado, y sobre todo
 * evita el N+1 que produciria recorrer los planes pidiendo sus lineas — en el
 * endpoint que sirve a gente sin autenticar, que es donde un N+1 se convierte
 * en una via de saturacion gratuita.
 *
 * <p>
 * <strong>El minimo estructural se anade a TODOS los planes por igual.</strong>
 * Todo paquete publicable contiene el nucleo, asi que lo que el nucleo concede
 * aplica igual a cualquiera de ellos. Se compone aqui y no con un {@code UNION}
 * en el SQL de las lineas, para que la regla del techo viva en Java junto a la
 * de la firma del alta.
 * <p>
 * <strong>Que significa «vigente».</strong> La decide {@link PriceListValidity}
 * sobre la fecha derivada del {@link Clock} inyectado, no el motor de base de
 * datos: la zona del negocio vive en el reloj (D-81) y un {@code CURRENT_DATE}
 * dejaria la portada sin precios entre las 19:00 y la medianoche del ultimo dia
 * de una tarifa. Si hay varias vigentes a la vez —situacion legal en el
 * esquema, porque nada impide dos ventanas solapadas— gana la de
 * {@code validFrom} mas reciente, y a igualdad la de id mayor: la ultima que se
 * publico es la que manda, y el criterio es determinista en vez de «la primera
 * que devuelva la consulta».
 */
@Observed(name = "pricelist.publicplans.get")
@Service
public class GetPublicPlansService implements GetPublicPlansUseCase {

    private static final PublicPlanCatalogDto SIN_TARIFA = new PublicPlanCatalogDto(null, null,
            List.of());

    private final PublicPlanQueryPort queryPort;
    private final Clock clock;

    public GetPublicPlansService(PublicPlanQueryPort queryPort, Clock clock) {
        this.queryPort = queryPort;
        this.clock = clock;
    }

    @Override
    @Transactional(readOnly = true)
    public PublicPlanCatalogDto get() {
        LocalDate today = LocalDateTime.now(clock).toLocalDate();
        Optional<PublicPriceListDto> vigente = tarifaVigente(today);
        if (vigente.isEmpty()) {
            return SIN_TARIFA;
        }
        PublicPriceListDto tarifa = vigente.get();

        Map<String, List<PublicPlanComponentRowDto>> porPlan = queryPort
                .findPlanComponents(tarifa.id()).stream()
                .collect(Collectors.groupingBy(PublicPlanComponentRowDto::planCode));
        List<PublicPlanCapacityDto> estructurales = queryPort.findStructuralCapacities(tarifa.id())
                .stream().map(GetPublicPlansService::toEstructural).toList();

        List<PublicPlanDto> plans = queryPort.findPlans(tarifa.id()).stream().map(
                plan -> toPlan(plan, porPlan.getOrDefault(plan.code(), List.of()), estructurales))
                .toList();

        return new PublicPlanCatalogDto(tarifa.currency(), tarifa.validFrom(), plans);
    }

    /**
     * La ventana la evalua el kernel y no el SQL; ver el javadoc de
     * {@code PublicPlanQueryPort.findPublishedPriceLists()}.
     *
     * <p>
     * El criterio en si vive en {@link PublicPriceListSelector} y no aqui desde que
     * hay un segundo endpoint publico con precios ({@code GET /catalog}): los dos
     * tienen que elegir la <em>misma</em> tarifa el mismo dia, y con el criterio
     * duplicado el dia que uno cambiara la portada y el configurador dirian cifras
     * distintas sin que nada fallara.
     */
    private Optional<PublicPriceListDto> tarifaVigente(LocalDate today) {
        return PublicPriceListSelector.vigente(queryPort.findPublishedPriceLists(), today);
    }

    /**
     * El techo con el que el plan publica una capacidad del minimo estructural: la
     * misma regla que {@code CreateInitialSubscriptionService.capacityLine} firma
     * en el alta inicial ({@code CapacityGrantLine.ceiling()}).
     */
    private static PublicPlanCapacityDto toEstructural(PublicStructuralCapacityRowDto fila) {
        return new PublicPlanCapacityDto(fila.code(), fila.name(), fila.capacityUnit(),
                fila.includedQuantity() + Math.max(fila.minQuantity(), 1),
                fila.monthlyExtraUnitAmount(), fila.annualExtraUnitAmount());
    }

    /**
     * Una capacidad de paquete cuyo eje ya cubre el minimo estructural se descarta:
     * el mismo eje no puede salir dos veces, y la del nucleo es la que manda porque
     * aplica a todos los planes por igual.
     */
    private static PublicPlanDto toPlan(PublicPlanRowDto plan,
            List<PublicPlanComponentRowDto> lineas, List<PublicPlanCapacityDto> estructurales) {
        List<PublicPlanIncludedDto> includes = lineas.stream().filter(linea -> !linea.esCapacidad())
                .map(linea -> new PublicPlanIncludedDto(linea.code(), linea.name(),
                        linea.trialDays()))
                .toList();
        Set<String> unidadesEstructurales = estructurales.stream().map(PublicPlanCapacityDto::unit)
                .collect(Collectors.toSet());
        Stream<PublicPlanCapacityDto> delPaquete = lineas.stream()
                .filter(PublicPlanComponentRowDto::esCapacidad)
                .filter(linea -> !unidadesEstructurales.contains(linea.capacityUnit()))
                .map(linea -> new PublicPlanCapacityDto(linea.code(), linea.name(),
                        linea.capacityUnit(), linea.includedQuantity(),
                        linea.monthlyExtraUnitAmount(), linea.annualExtraUnitAmount()));
        List<PublicPlanCapacityDto> capacities = Stream.concat(estructurales.stream(), delPaquete)
                .toList();
        return new PublicPlanDto(plan.code(), plan.name(), plan.tagline(), plan.monthlyFromAmount(),
                plan.annualFromAmount(), plan.setupAmount(), plan.taxRate(), plan.taxTreatment(),
                includes, capacities);
    }
}
