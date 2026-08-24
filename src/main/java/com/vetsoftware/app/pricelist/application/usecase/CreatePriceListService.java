package com.vetsoftware.app.pricelist.application.usecase;

import com.vetsoftware.app.pricelist.application.command.CreatePriceListCommand;
import com.vetsoftware.app.pricelist.application.dto.LinkStateDto;
import com.vetsoftware.app.pricelist.application.dto.PriceListDto;
import com.vetsoftware.app.pricelist.application.port.in.CreatePriceListUseCase;
import com.vetsoftware.app.pricelist.application.port.out.PriceListRepository;
import com.vetsoftware.app.pricelist.domain.PriceList;
import com.vetsoftware.app.pricelist.domain.PriceListCodeAlreadyExistsException;
import com.vetsoftware.app.pricelist.domain.PriceListNotFoundException;
import io.micrometer.observation.annotation.Observed;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Alta de una lista de precios, con la guarda de codigo que este servicio no
 * tenia y que sus tres hermanos —{@code CreateCatalogItemService},
 * {@code CreateConfiguratorQuestionService} y
 * {@code CreateConfiguratorOptionService}— si.
 *
 * <p>
 * Sin ella, repetir un codigo no daba un error de campo sobre {@code code}: el
 * {@code INSERT} chocaba contra {@code uq_price_lists_code} y el handler caia
 * en su rama sin mapeo, con lo que quien creaba la tarifa recibia un 409
 * generico y tenia que adivinar cual de los cinco campos estaba mal.
 *
 * <p>
 * Y la guarda mira <strong>tambien las listas retiradas</strong>, porque el
 * borrado de {@code price_lists} es logico y la clave unica no ignora
 * {@code enabled}: una lista dada de baja sigue ocupando su codigo. Como en las
 * tablas puente de {@code catalogitem}, el alta la <strong>reactiva</strong> en
 * vez de insertar otra.
 */
@Observed(name = "pricelist.create")
@Service
public class CreatePriceListService implements CreatePriceListUseCase {

    private final PriceListRepository repository;
    private final Clock clock;

    public CreatePriceListService(PriceListRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    /**
     * {@code @Transactional} no es decorativo aqui: la reactivacion y la
     * reescritura son dos operaciones, y si la segunda falla —por ejemplo porque la
     * lista retirada estaba PUBLISHED y {@code update} exige DRAFT— la primera
     * tiene que deshacerse. Sin transaccion, un intento rechazado dejaria la lista
     * vieja encendida.
     */
    @Override
    @Transactional
    public PriceListDto execute(CreatePriceListCommand command) {
        // La entidad se construye primero porque valida las invariantes del comando:
        // un comando invalido no debe llegar a consultar nada, que es el mismo orden
        // que sigue CreateBundleComponentService en catalogitem.
        PriceList priceList = PriceList.create(command.code(), command.name(), command.currency(),
                command.validFrom(), command.validTo(), LocalDateTime.now(clock));
        Optional<LinkStateDto> existente = repository.findAnyByCode(command.code());
        if (existente.isPresent()) {
            return revivir(existente.get(), command);
        }
        return PriceListDto.from(repository.save(priceList));
    }

    /**
     * Reescribir la lista revivida con lo que trae el comando pasa por
     * {@code PriceList.update}, que exige DRAFT (R9). Es deliberado: una lista
     * retirada que ya estaba publicada no se puede reciclar —sus importes son parte
     * de contratos vivos— y el rechazo llega como
     * {@code PriceListNotEditableException}, un 409 que nombra el estado, en vez de
     * como una tarifa publicada sobrescrita en silencio.
     */
    private PriceListDto revivir(LinkStateDto estado, CreatePriceListCommand command) {
        if (estado.enabled()) {
            throw new PriceListCodeAlreadyExistsException(command.code());
        }
        repository.reactivate(estado.id());
        PriceList revivida = repository.findById(estado.id())
                .orElseThrow(() -> new PriceListNotFoundException(estado.id()));
        revivida.update(command.name(), command.currency(), command.validFrom(), command.validTo());
        return PriceListDto.from(repository.save(revivida));
    }
}
