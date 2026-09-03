package com.vetsoftware.app.pricelist.application.usecase;

import com.vetsoftware.app.pricelist.application.dto.PublicCatalogAreaDto;
import com.vetsoftware.app.pricelist.application.dto.PublicCatalogCapacityDto;
import com.vetsoftware.app.pricelist.application.dto.PublicCatalogDto;
import com.vetsoftware.app.pricelist.application.dto.PublicCatalogItemDto;
import com.vetsoftware.app.pricelist.application.dto.PublicCatalogItemRowDto;
import com.vetsoftware.app.pricelist.application.dto.PublicCatalogPackComponentRowDto;
import com.vetsoftware.app.pricelist.application.dto.PublicCatalogPackDto;
import com.vetsoftware.app.pricelist.application.dto.PublicCatalogRequirementDto;
import com.vetsoftware.app.pricelist.application.dto.PublicPriceListDto;
import com.vetsoftware.app.pricelist.application.port.in.GetPublicCatalogUseCase;
import com.vetsoftware.app.pricelist.application.port.out.PublicCatalogQueryPort;
import com.vetsoftware.app.pricelist.application.port.out.PublicPlanQueryPort;
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
 * El catalogo contratable en <strong>un punado de consultas fijas</strong>, no
 * en una por articulo.
 *
 * <p>
 * Misma forma que {@link GetPublicPlansService} y por el mismo motivo: se traen
 * las tarifas publicadas, los articulos sueltos de la vigente, sus paquetes, la
 * composicion de esos paquetes y las cabeceras funcionales, y se agrupa en
 * memoria. Sobre unas decenas de filas eso es mas barato que cualquier
 * {@code JOIN} anidado y, sobre todo, evita el N+1 que produciria recorrer los
 * paquetes pidiendo sus piezas — en un endpoint que sirve a gente sin
 * autenticar, que es donde un N+1 es una via de saturacion gratuita.
 *
 * <p>
 * <strong>La tarifa la elige {@link PublicPriceListSelector}</strong>,
 * compartido con el servicio de planes. Los dos endpoints publican precios el
 * mismo dia y tienen que publicar los de la <em>misma</em> lista; con el
 * criterio duplicado, el dia que uno de los dos cambiara, la portada y el
 * configurador dirian cifras distintas sin que nada fallara.
 *
 * <p>
 * <strong>Lo que este servicio NO hace: sumar.</strong> No calcula el total de
 * una seleccion ni compara un paquete con la suma de sus piezas. Publica los
 * numeros con los que esa cuenta se puede hacer y la deja donde tiene que
 * estar: el precio de verdad lo congela {@code CreateQuoteService} contra el
 * catalogo, y cualquier cifra que este endpoint calculara seria una segunda
 * opinion sin autoridad que tarde o temprano discreparia de la primera.
 */
@Observed(name = "pricelist.publiccatalog.get")
@Service
public class GetPublicCatalogService implements GetPublicCatalogUseCase {

    private static final PublicCatalogDto SIN_TARIFA = new PublicCatalogDto(null, null, List.of(),
            List.of(), List.of(), List.of(), List.of(), List.of());

    private final PublicPlanQueryPort priceListQueryPort;
    private final PublicCatalogQueryPort queryPort;
    private final Clock clock;

    public GetPublicCatalogService(PublicPlanQueryPort priceListQueryPort,
            PublicCatalogQueryPort queryPort, Clock clock) {
        this.priceListQueryPort = priceListQueryPort;
        this.queryPort = queryPort;
        this.clock = clock;
    }

    @Override
    @Transactional(readOnly = true)
    public PublicCatalogDto get() {
        LocalDate today = LocalDateTime.now(clock).toLocalDate();
        Optional<PublicPriceListDto> vigente = PublicPriceListSelector
                .vigente(priceListQueryPort.findPublishedPriceLists(), today);
        if (vigente.isEmpty()) {
            return SIN_TARIFA;
        }
        PublicPriceListDto tarifa = vigente.get();

        List<PublicCatalogItemRowDto> articulos = queryPort.findContractableItems(tarifa.id());
        Map<String, List<String>> porPaquete = queryPort.findPackComponents(tarifa.id()).stream()
                .collect(Collectors.groupingBy(PublicCatalogPackComponentRowDto::packCode,
                        Collectors.mapping(PublicCatalogPackComponentRowDto::componentCode,
                                Collectors.toList())));

        return new PublicCatalogDto(tarifa.currency(), tarifa.validFrom(),
                articulos.stream().filter(PublicCatalogItemRowDto::esModulo)
                        .map(PublicCatalogItemDto::from).toList(),
                articulos.stream().filter(PublicCatalogItemRowDto::esCapacidad)
                        .map(PublicCatalogCapacityDto::from).toList(),
                articulos.stream().filter(PublicCatalogItemRowDto::esCargoUnico)
                        .map(PublicCatalogItemDto::from).toList(),
                queryPort.findPacks(tarifa.id()).stream()
                        .map(pack -> PublicCatalogPackDto.from(pack,
                                porPaquete.getOrDefault(pack.code(), List.of())))
                        .toList(),
                queryPort.findRequirements().stream().map(PublicCatalogRequirementDto::from)
                        .toList(),
                queryPort.findAreas().stream().map(PublicCatalogAreaDto::from).toList());
    }
}
