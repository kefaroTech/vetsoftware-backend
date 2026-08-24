package com.vetsoftware.app.registration.infrastructure.orchestration;

import com.vetsoftware.app.auth.infrastructure.security.SystemAuthRunner;
import com.vetsoftware.app.employeebranch.application.command.SetEmployeeBranchesCommand;
import com.vetsoftware.app.employeebranch.application.port.in.GetEmployeeBranchesUseCase;
import com.vetsoftware.app.employeebranch.application.port.in.SetEmployeeBranchesUseCase;
import com.vetsoftware.app.registration.application.port.out.OwnerBranchAssigner;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * Adapter de orquestacion: ata al dueño con sus sedes por el <b>mismo camino
 * que usa el alta de staff</b> ({@code EmployeeBranchAssignerAdapter} →
 * {@link SetEmployeeBranchesUseCase}), no con un INSERT paralelo dentro del
 * registro.
 *
 * <p>
 * <b>Por que reusar y no escribir el INSERT aqui.</b> El set de sedes no es una
 * fila: es un set atomico —desactivar lo vigente, reactivar lo soft-deleted,
 * insertar lo que falte, con el unique {@code (employee_id, branch_id)} de por
 * medio— mas la validacion de que cada sede es de la empresa, mas la
 * invalidacion del cache Redis {@code employee-branch-ids} <em>despues del
 * commit</em>. Esas cuatro cosas ya estan resueltas, probadas y acotadas por
 * empresa en {@code employeebranch}. Un INSERT propio en el registro habria
 * duplicado las cuatro y —esto es lo que de verdad importa— habria olvidado la
 * cuarta: es literalmente el evict que #510 tuvo que hacer <em>a mano</em> en
 * Redis para poder terminar de reproducir el defecto.
 *
 * <p>
 * <b>{@code allBranches = true} y no la id de la sede recien creada.</b> Evita
 * cambiar la firma de {@code BranchCreator} para devolver una id, y sobre todo
 * dice lo correcto: el dueño opera en <em>toda</em> su empresa. Ademas encaja
 * con {@code findFullCoverageEmployeeIds}, que al crear una sede nueva se la
 * hereda a quien ya cubre todas — con la id fija, el dueño se habria quedado
 * clavado en la Principal el dia que abriera la segunda sede.
 *
 * <p>
 * <b>Se lee de vuelta.</b> El {@code EmployeeBranchesDto} que devuelve el set
 * repite el objetivo pedido, lo haya escrito o no; la lectura por
 * {@link GetEmployeeBranchesUseCase} sale de
 * {@code EmployeeBranchJpaRepository.findBranchIdsByEmployeeId}, que es la
 * misma consulta con la que {@code JpaBranchAccessResolver} construye el
 * {@code currentBranchIds()} del 403. Comprobar la misma fuente que autoriza es
 * lo unico que convierte la guarda del alta en una guarda de verdad.
 *
 * <p>
 * Bajo {@link SystemAuthRunner} por lo mismo que
 * {@code CreateEmployeeRoleAdapter} : todavia no existe ningun empleado con
 * sesion, y el empleado y la sede los acaba de generar el propio
 * {@code RegisterUserService} en la misma transaccion, asi que no hay id que
 * adivinar.
 */
@Component
public class SetEmployeeBranchesAdapter implements OwnerBranchAssigner {

    private final SetEmployeeBranchesUseCase setUseCase;
    private final GetEmployeeBranchesUseCase getUseCase;
    private final SystemAuthRunner systemAuthRunner;

    public SetEmployeeBranchesAdapter(SetEmployeeBranchesUseCase setUseCase,
            GetEmployeeBranchesUseCase getUseCase, SystemAuthRunner systemAuthRunner) {
        this.setUseCase = setUseCase;
        this.getUseCase = getUseCase;
        this.systemAuthRunner = systemAuthRunner;
    }

    @Override
    public List<Long> assignAllCompanyBranches(Long employeeId, Long companyId) {
        return systemAuthRunner.call(() -> {
            setUseCase.execute(new SetEmployeeBranchesCommand(employeeId, companyId, true, null));
            return getUseCase.execute(employeeId, companyId).branchIds();
        });
    }
}
