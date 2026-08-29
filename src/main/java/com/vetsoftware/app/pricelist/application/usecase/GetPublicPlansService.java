package com.vetsoftware.app.pricelist.application.usecase;

import com.vetsoftware.app.pricelist.application.dto.PublicPlanCapacityDto;
import com.vetsoftware.app.pricelist.application.dto.PublicPlanCatalogDto;
import com.vetsoftware.app.pricelist.application.dto.PublicPlanComponentRowDto;
import com.vetsoftware.app.pricelist.application.dto.PublicPlanDto;
import com.vetsoftware.app.pricelist.application.dto.PublicPlanIncludedDto;
import com.vetsoftware.app.pricelist.application.dto.PublicPlanRowDto;
import com.vetsoftware.app.pricelist.application.dto.PublicPriceListDto;
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
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * El catalogo publico en <strong>tres consultas</strong>, no en una por plan.
 *
 * <p>
 * Se traen las tarifas publicadas, los paquetes de la vigente y todas sus
 * lineas, y se agrupan en memoria. Es la misma forma que
 * {@code GetPublicQuestionnaireService} y por el mismo motivo: sobre decenas de
 * filas eso es mas barato que cualquier {@code JOIN} anidado, y sobre todo
 * evita el N+1 que produciria recorrer los planes pidiendo sus lineas — en el
 * endpoint que sirve a gente sin autenticar, que es donde un N+1 se convierte
 * en una via de saturacion gratuita.
 *
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

        List<PublicPlanDto> plans = queryPort.findPlans(tarifa.id()).stream()
                .map(plan -> toPlan(plan, porPlan.getOrDefault(plan.code(), List.of()))).toList();

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

    private static PublicPlanDto toPlan(PublicPlanRowDto plan,
            List<PublicPlanComponentRowDto> lineas) {
        List<PublicPlanIncludedDto> includes = lineas.stream().filter(linea -> !linea.esCapacidad())
                .map(linea -> new PublicPlanIncludedDto(linea.code(), linea.name(),
                        linea.trialDays()))
                .toList();
        List<PublicPlanCapacityDto> capacities = lineas.stream()
                .filter(PublicPlanComponentRowDto::esCapacidad)
                .map(linea -> new PublicPlanCapacityDto(linea.code(), linea.name(),
                        linea.capacityUnit(), linea.includedQuantity(),
                        linea.monthlyExtraUnitAmount(), linea.annualExtraUnitAmount()))
                .toList();
        return new PublicPlanDto(plan.code(), plan.name(), plan.tagline(), plan.monthlyFromAmount(),
                plan.annualFromAmount(), plan.setupAmount(), plan.taxRate(), plan.taxTreatment(),
                includes, capacities);
    }
}
