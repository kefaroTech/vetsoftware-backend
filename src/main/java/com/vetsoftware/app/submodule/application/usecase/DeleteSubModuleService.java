package com.vetsoftware.app.submodule.application.usecase;

import com.vetsoftware.app.submodule.application.port.in.DeleteSubModuleUseCase;
import com.vetsoftware.app.submodule.application.port.out.CatalogItemChildrenQueryPort;
import com.vetsoftware.app.submodule.application.port.out.CompanyEntitlementChildrenQueryPort;
import com.vetsoftware.app.submodule.application.port.out.SubModuleRepository;
import com.vetsoftware.app.submodule.domain.SubModuleHasActiveChildrenException;
import com.vetsoftware.app.submodule.domain.SubModuleNotFoundException;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * La guarda de hijos <b>sobrevive</b>, apuntando al modelo nuevo, y son
 * <b>dos</b>.
 *
 * <p>
 * Al desaparecer {@code membership_sub_modules} era tentador quitarla: era la
 * unica que tenia. Pero el motivo por el que existia no desaparecio, empeoro.
 * Borrar un submodulo aqui es un borrado <b>logico</b> —{@code @SQLDelete} pone
 * {@code enabled = false}—, asi que la clave foranea de la base <b>nunca
 * salta</b>: nada impide apagar un submodulo que un articulo del catalogo sigue
 * vendiendo. El resultado seria una empresa pagando por una pantalla que ya no
 * existe, y un recalculo de {@code company_entitlements} generando permisos
 * sobre una fila apagada. Con dinero de por medio, el fallo es mas caro que el
 * que la guarda vieja prevenia, no menos.
 *
 * <p>
 * <b>Por que hay una segunda guarda</b> (#413). La primera solo mira el
 * <b>catalogo comercial</b>, y hay concesiones que no pasan por el: una
 * {@code MANUAL_GRANT} no tiene ninguna fila en
 * {@code catalog_item_sub_modules}. Con una sola guarda, apagar ese submodulo
 * pasaba limpio, la fila de {@code company_entitlements} seguia vigente con
 * nivel {@code FULL} y en la peticion siguiente los empleados de esa clinica
 * perdian los codigos —la consulta de permisos efectivos exige
 * {@code sub_modules.enabled = TRUE}—. La ficha de la empresa decia
 * \"concedido\" y el endpoint respondia 403, semanas despues y sin que nada
 * relacionara los dos hechos.
 *
 * <p>
 * <b>La guarda del inquilino bloquea con CUALQUIER origen vigente, no solo con
 * {@code MANUAL_GRANT}</b>, y esa es la decision deliberada del hallazgo. Los
 * dos fallos posibles no son simetricos:
 * <ul>
 * <li><b>Bloquear de mas</b> es un estorbo <em>reversible y visible</em>: el
 * usuario de plataforma —que es SYSTEM— recibe en el acto un 409 que nombra el
 * tipo de hijo, y retira primero los articulos o espera a que los contratos
 * venzan. Nadie pierde acceso ni datos.
 * <li><b>Dejar pasar</b> es un fallo <em>silencioso y repartido</em>: se apaga
 * un submodulo y N clinicas descubren el 403 por su cuenta, cada una en un
 * momento distinto.
 * </ul>
 * A eso se anade que, con #414 arreglado, un recalculo posterior ya <em>no</em>
 * recrea filas sobre un submodulo apagado: los entitlements derivados de
 * contrato desapareceran en el siguiente cambio de contrato mientras la linea
 * de suscripcion se sigue facturando. Eso es literalmente \"una empresa pagando
 * por una pantalla que ya no existe\", que es el proposito escrito de esta
 * guarda. Y seria arbitrario que el submodulo se protegiera de una venta
 * <em>potencial</em> (el articulo de catalogo) y no de una <em>consumada</em>
 * (el contrato vivo).
 *
 * <p>
 * Un {@code access_level = 'NONE'} no bloquea: significa explicitamente \"este
 * submodulo no existe para esta empresa\", asi que no hay acceso que proteger.
 */
@Observed(name = "submodule.delete")
@Service
public class DeleteSubModuleService implements DeleteSubModuleUseCase {
    private final SubModuleRepository repository;
    private final CatalogItemChildrenQueryPort catalogItemChildrenQueryPort;
    private final CompanyEntitlementChildrenQueryPort companyEntitlementChildrenQueryPort;

    public DeleteSubModuleService(SubModuleRepository repository,
            CatalogItemChildrenQueryPort catalogItemChildrenQueryPort,
            CompanyEntitlementChildrenQueryPort companyEntitlementChildrenQueryPort) {
        this.repository = repository;
        this.catalogItemChildrenQueryPort = catalogItemChildrenQueryPort;
        this.companyEntitlementChildrenQueryPort = companyEntitlementChildrenQueryPort;
    }

    @Override
    @Transactional
    public void execute(Long id) {
        repository.findById(id).orElseThrow(() -> new SubModuleNotFoundException(id));
        if (catalogItemChildrenQueryPort.existsActiveBySubModuleId(id)) {
            throw new SubModuleHasActiveChildrenException(id, "catalogItemSubModule");
        }
        // El orden importa poco funcionalmente, pero el catalogo se comprueba primero
        // porque es la causa mas probable y la que el usuario de plataforma puede
        // resolver el mismo: retirar el articulo esta en sus manos, cancelar el
        // contrato de una clinica no.
        if (companyEntitlementChildrenQueryPort.existsActiveBySubModuleId(id)) {
            throw new SubModuleHasActiveChildrenException(id, "companyEntitlement");
        }
        repository.delete(id);
    }
}
