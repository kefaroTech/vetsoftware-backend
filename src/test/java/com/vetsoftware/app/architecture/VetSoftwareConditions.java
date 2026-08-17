package com.vetsoftware.app.architecture;

import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaField;
import com.tngtech.archunit.core.domain.JavaMethod;
import com.tngtech.archunit.core.domain.JavaMethodCall;
import com.tngtech.archunit.core.domain.JavaParameterizedType;
import com.tngtech.archunit.core.domain.JavaType;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;
import jakarta.persistence.ManyToOne;
import java.lang.reflect.Parameter;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.scheduling.annotation.Async;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * Condiciones de arquitectura propias del proyecto, las que no se expresan con
 * la API fluida de ArchUnit.
 */
final class VetSoftwareConditions {

    private static final String APP_PACKAGE = "com.vetsoftware.app";

    /**
     * Profundidad máxima de la búsqueda transitiva; evita recorrer el grafo entero.
     */
    private static final int MAX_DEPTH = 6;

    /** Captura la expresión que recibe {@code @authz.isMyCompany(#…)}. */
    private static final Pattern ISMYCOMPANY_REF = Pattern
            .compile("isMyCompany\\(\\s*#([A-Za-z0-9_.]+)");

    /** Disyunto que deja pasar al principal SYSTEM, cross-tenant por diseño. */
    private static final String SYSTEM_DISJUNCT = "hasRole('SYSTEM')";

    private static final String PORT_IN_PACKAGE = ".application.port.in";

    private static final String PORT_OUT_PACKAGE = ".application.port.out";

    /** Envoltorio de página de la capa de aplicación; hay exactamente uno. */
    private static final String PAGE_RESULT = "PageResult";

    /**
     * Los nombres con los que se ha declarado —o se podría volver a declarar— el
     * concepto «página». No es una lista de prohibidos: es la firma del hallazgo
     * BE-21, donde treinta y cinco features escribieron el mismo record con el
     * mismo nombre en su propio paquete.
     */
    private static final Set<String> NOMBRES_DE_PAGINA = Set.of(PAGE_RESULT, "PageResponse",
            "PagedResult", "PagedResponse", "PageDto", "Slice", "SliceResult");

    /** El {@code PageRequest} de Spring Data: el que hay que acotar. */
    private static final String PAGE_REQUEST = "org.springframework.data.domain.PageRequest";

    /** La API del commit: quien la toca sabe a qué lado del commit está. */
    private static final String TRANSACTION_SYNCHRONIZATION_MANAGER = TransactionSynchronizationManager.class
            .getName();

    private VetSoftwareConditions() {
    }

    /**
     * Las clases que declaran el concepto «página» por su nombre. Mira el nombre y
     * no la forma a propósito: la copia 37 se llamará igual que las 36 anteriores,
     * y para cuando difiera en un campo ya será otra deuda distinta.
     */
    static DescribedPredicate<JavaClass> declaranElConceptoDePagina() {
        return DescribedPredicate.describe("declaran el concepto «pagina»",
                clazz -> NOMBRES_DE_PAGINA.contains(clazz.getSimpleName()));
    }

    /**
     * Toda llamada a {@code PageRequest.of(...)}. Fuera del kernel de paginación no
     * hay ninguna legítima: el índice hay que normalizarlo y el tamaño hay que
     * toparlo, y eso ya lo hace {@code Pages.request(...)}.
     */
    static DescribedPredicate<JavaMethodCall> esUnPageRequestSinAcotar() {
        return DescribedPredicate.describe("construye un PageRequest fuera del kernel",
                call -> PAGE_REQUEST.equals(call.getTargetOwner().getFullName())
                        && "of".equals(call.getTarget().getName()));
    }

    /**
     * Detecta si un método alcanza —directamente o a través de la cadena de
     * llamadas del propio proyecto— un cliente HTTP externo.
     *
     * <p>
     * La búsqueda es transitiva a propósito. El caso real que motivó la regla
     * (BE-02) no llamaba al cliente HTTP desde el método transaccional, sino desde
     * un provider dos saltos más abajo; una regla de llamada directa no lo habría
     * visto. Cuando el salto es a una interfaz, se siguen también sus
     * implementaciones, que es donde vive la llamada.
     *
     * @param httpClientTypes
     *            tipos considerados cliente HTTP (p. ej. {@code RestClient})
     */
    static ArchCondition<JavaMethod> alcanzarUnClienteHttp(Class<?>... httpClientTypes) {
        Set<String> clientNames = new HashSet<>();
        for (Class<?> type : httpClientTypes) {
            clientNames.add(type.getName());
        }
        String description = "alcanzar un cliente HTTP externo (" + String.join(", ", clientNames)
                + ") directamente o a traves de la cadena de llamadas";

        return new ArchCondition<>(description) {
            @Override
            public void check(JavaMethod method, ConditionEvents events) {
                List<String> path = rutaHasta(method,
                        call -> clientNames.contains(call.getTargetOwner().getFullName()),
                        VetSoftwareConditions::saltaDeHilo);
                if (!path.isEmpty()) {
                    events.add(SimpleConditionEvent.satisfied(method, method.getFullName()
                            + " alcanza un cliente HTTP: " + String.join(" -> ", path)));
                }
            }
        };
    }

    /**
     * La otra mitad de {@link #alcanzarUnClienteHttp(Class...)}, y el cierre de
     * BE-18: detecta el efecto {@code @Async} que se dispara <em>dentro</em> de una
     * transacción sin esperar a que confirme.
     *
     * <p>
     * Las dos reglas miran el mismo salto y sacan conclusiones opuestas, a
     * propósito. Aquella se detiene ahí porque lo que cruza de hilo ya no retiene
     * la conexión ni los locks del caller: deja de ser su problema. Esta empieza
     * justo ahí porque lo que cruza de hilo <b>tampoco vuelve</b>. El proxy encola
     * la tarea al instante y el pool la ejecuta cuando quiere —normalmente antes de
     * que el caller haya hecho flush—, así que un rollback posterior no deshace
     * nada: el correo ya salió.
     *
     * <p>
     * El caso real: {@code CreateAppointmentService.execute} enviaba la
     * confirmación desde dentro de la transacción, y {@code AppointmentJpaEntity}
     * tiene {@code @Version}, cuyo choque optimista salta en el flush,
     * <i>después</i> de la última línea del método. El cliente recibía la
     * confirmación de una cita que nunca existió.
     *
     * <p>
     * <b>Dónde se detiene la búsqueda.</b> En el primer método que habla con
     * {@code TransactionSynchronizationManager} —el origen incluido—. Ese método
     * está decidiendo explícitamente qué ocurre a cada lado del commit; si lo
     * decide mal, eso lo ve una revisión, no una regla de arquitectura. Sin este
     * corte la regla marcaría el propio patrón correcto: la rama de guarda de
     * {@code sendAfterCommit} —la que envía en el acto cuando no hay transacción
     * activa, porque {@code registerSynchronization} lanzaría ahí— es una llamada
     * directa al port como cualquier otra.
     *
     * <p>
     * <b>Por qué el callback no se comprueba.</b> No hace falta: el cuerpo de un
     * {@code afterCommit} vive en una clase anónima aparte, así que no cuelga del
     * método transaccional y el recorrido nunca llega. Es la misma razón por la que
     * el patrón es correcto en ejecución. El día que alguien difiera con un
     * <i>lambda</i>, ArchUnit atribuirá su cuerpo al método que lo declara y la
     * regla dará un falso positivo: la salida es extraer el diferido a su propio
     * método, como hacen hoy los dos que existen.
     */
    static ArchCondition<JavaMethod> alcanzarUnEfectoAsincrono() {
        return new ArchCondition<>("disparar un efecto @Async sin esperar al commit") {
            @Override
            public void check(JavaMethod method, ConditionEvents events) {
                if (difiereAlCommit(method)) {
                    return;
                }
                List<String> path = rutaHasta(method, VetSoftwareConditions::saltaAOtroHilo,
                        VetSoftwareConditions::difiereAlCommit);
                if (!path.isEmpty()) {
                    events.add(SimpleConditionEvent.satisfied(method,
                            method.getFullName() + " dispara un efecto @Async dentro de la"
                                    + " transaccion: " + String.join(" -> ", path)));
                }
            }
        };
    }

    /**
     * Recorre en anchura las llamadas salientes del proyecto y devuelve la primera
     * ruta que llega al destino, o una lista vacía si no hay ninguna.
     *
     * <p>
     * La búsqueda es transitiva a propósito. El caso real que motivó la primera
     * regla que la usa (BE-02) no llamaba al cliente HTTP desde el método
     * transaccional, sino desde un provider dos saltos más abajo; una regla de
     * llamada directa no lo habría visto. Cuando el salto es a una interfaz, se
     * siguen también sus implementaciones, que es donde vive la llamada.
     *
     * @param esElDestino
     *            qué llamada cierra la búsqueda
     * @param cortaLaBusqueda
     *            métodos en los que no se sigue bajando
     */
    private static List<String> rutaHasta(JavaMethod origin, Predicate<JavaMethodCall> esElDestino,
            Predicate<JavaMethod> cortaLaBusqueda) {
        Set<String> visited = new HashSet<>();
        Deque<List<JavaMethod>> queue = new ArrayDeque<>();
        queue.add(List.of(origin));
        visited.add(origin.getFullName());

        while (!queue.isEmpty()) {
            List<JavaMethod> path = queue.poll();
            JavaMethod current = path.get(path.size() - 1);

            for (JavaMethodCall call : current.getMethodCallsFromSelf()) {
                if (esElDestino.test(call)) {
                    return describe(path, call);
                }
                if (path.size() >= MAX_DEPTH || !isOwnCode(call.getTargetOwner())) {
                    continue;
                }
                for (JavaMethod next : resolveTargets(call)) {
                    if (cortaLaBusqueda.test(next) || !visited.add(next.getFullName())) {
                        continue;
                    }
                    List<JavaMethod> extended = new ArrayList<>(path);
                    extended.add(next);
                    queue.add(extended);
                }
            }
        }
        return List.of();
    }

    /**
     * {@code true} si la llamada entrega el trabajo a otro hilo vía {@code @Async}.
     */
    private static boolean saltaAOtroHilo(JavaMethodCall call) {
        return resolveTargets(call).stream().anyMatch(VetSoftwareConditions::saltaDeHilo);
    }

    /**
     * {@code true} si el método habla con
     * {@code TransactionSynchronizationManager}, en cualquiera de sus dos formas:
     * registrar el callback o preguntar si hay sincronización activa. Las dos
     * delatan a un método que sabe dónde está el commit.
     */
    private static boolean difiereAlCommit(JavaMethod method) {
        for (JavaMethodCall call : method.getMethodCallsFromSelf()) {
            if (TRANSACTION_SYNCHRONIZATION_MANAGER.equals(call.getTargetOwner().getFullName())) {
                return true;
            }
        }
        return false;
    }

    /**
     * Resuelve el destino de una llamada. Si apunta a una interfaz, devuelve además
     * el método homónimo de cada implementación: es ahí donde está el cuerpo que
     * interesa.
     */
    private static List<JavaMethod> resolveTargets(JavaMethodCall call) {
        List<JavaMethod> targets = new ArrayList<>();
        Optional<JavaMethod> resolved = call.getTarget().resolveMember();
        if (resolved.isEmpty()) {
            return targets;
        }
        JavaMethod target = resolved.get();
        targets.add(target);

        JavaClass owner = target.getOwner();
        if (owner.isInterface()) {
            String[] parameterTypeNames = target.getRawParameterTypes().stream()
                    .map(JavaClass::getName).toArray(String[]::new);
            for (JavaClass implementation : owner.getAllSubclasses()) {
                if (!isOwnCode(implementation)) {
                    continue;
                }
                implementation.tryGetMethod(target.getName(), parameterTypeNames)
                        .ifPresent(targets::add);
            }
        }
        return targets;
    }

    private static boolean isOwnCode(JavaClass javaClass) {
        return javaClass.getPackageName().startsWith(APP_PACKAGE);
    }

    /**
     * {@code true} si la llamada cruza a otro hilo vía {@code @Async}.
     *
     * <p>
     * El proxy encola la ejecución en otro pool y devuelve inmediatamente, así que
     * lo que pase al otro lado ya no corre dentro de la transacción del caller. De
     * ahí que las dos reglas que usan este predicado lo lean al revés:
     * {@link #alcanzarUnClienteHttp(Class...)} <b>corta</b> aquí, porque más allá
     * del salto ya no se retiene la conexión ni los locks;
     * {@link #alcanzarUnEfectoAsincrono()} <b>reporta</b> aquí, porque más allá del
     * salto el efecto ya no se puede deshacer.
     */
    private static boolean saltaDeHilo(JavaMethod method) {
        return method.isAnnotatedWith(Async.class)
                || method.getOwner().isAnnotatedWith(Async.class);
    }

    /**
     * Exige que un puerto cuyo command transporta {@code companyId} valide el
     * tenant en su {@code @PreAuthorize}.
     *
     * <p>
     * El {@code companyId} lo inyecta el controller desde el principal, pero el
     * puerto es invocable por cualquier otro caller; sin {@code @authz.isMyCompany}
     * un caller que pase otra empresa opera sobre datos ajenos. Es la defensa en
     * profundidad que pide el CLAUDE.md.
     */
    static ArchCondition<JavaMethod> validarElTenantCuandoRecibeCompanyId() {
        return new ArchCondition<>("validar el tenant con @authz.isMyCompany") {
            @Override
            public void check(JavaMethod method, ConditionEvents events) {
                if (!transportaCompanyId(method)) {
                    return;
                }
                Optional<PreAuthorize> gate = method.tryGetAnnotationOfType(PreAuthorize.class);
                if (gate.isPresent() && soloAlcanzablePorSystem(gate.get().value())) {
                    return;
                }
                if (gate.isEmpty()) {
                    events.add(new SimpleConditionEvent(method, false,
                            method.getFullName() + " recibe companyId y no tiene @PreAuthorize"));
                    return;
                }
                String motivo = revisarGate(method, gate.get().value());
                events.add(new SimpleConditionEvent(method, motivo == null, method.getFullName()
                        + " recibe companyId pero su @PreAuthorize " + motivo));
            }
        };
    }

    /**
     * {@code null} si el gate valida el tenant correctamente; si no, qué le pasa.
     *
     * <p>
     * No basta con que aparezca {@code isMyCompany}: hay que comprobar a qué
     * apunta. SpEL resuelve un {@code #parametroInexistente} a {@code null} sin
     * avisar, {@code isMyCompany(null)} devuelve {@code false} y la regla queda
     * siempre falsa — el endpoint se cierra para todos y nadie entiende por qué. Es
     * la trampa que advierte el CLAUDE.md, y con decenas de anotaciones editadas a
     * la vez es fácil de sembrar.
     */
    private static String revisarGate(JavaMethod method, String expression) {
        Matcher reference = ISMYCOMPANY_REF.matcher(expression);
        if (!reference.find()) {
            return "no invoca @authz.isMyCompany";
        }
        String root = reference.group(1).split("\\.")[0];
        Optional<Set<String>> parameterNames = nombresDeParametro(method);
        if (parameterNames.isPresent() && !parameterNames.get().contains(root)) {
            return "invoca @authz.isMyCompany(#" + reference.group(1) + "), y '" + root
                    + "' no es un parametro del metodo: SpEL lo resuelve a null y el gate"
                    + " queda siempre en false";
        }
        return null;
    }

    /**
     * {@code true} si al gate solo llega un principal SYSTEM, que es cross-tenant
     * por diseño. Ahí el conjunto del tenant seria codigo muerto: ningun empleado
     * alcanza el metodo.
     */
    private static boolean soloAlcanzablePorSystem(String expression) {
        return Arrays.stream(expression.split("\\bor\\b")).map(part -> part.replaceAll("\\s", ""))
                .allMatch(part -> part.isEmpty() || SYSTEM_DISJUNCT.equals(part));
    }

    /**
     * Vacío si la clase no se puede reflexionar: sin nombres no hay nada que
     * validar.
     */
    private static Optional<Set<String>> nombresDeParametro(JavaMethod method) {
        try {
            Set<String> names = new HashSet<>();
            for (Parameter parameter : method.reflect().getParameters()) {
                names.add(parameter.getName());
            }
            return Optional.of(names);
        } catch (RuntimeException | NoClassDefFoundError ignored) {
            return Optional.empty();
        }
    }

    /**
     * {@code true} si el método recibe la empresa, de cualquiera de las dos formas
     * que se usan en el proyecto: dentro de un command
     * ({@code execute(CreateXxxCommand)}, con un campo {@code companyId}) o como
     * parámetro suelto ({@code find(Long companyId, Long id)}).
     *
     * <p>
     * Las dos cuentan. Mirar solo los commands deja fuera los Find/List/Delete, que
     * es justo donde estaban los hallazgos financieros y clínicos de BE-08; mirar
     * solo los parámetros sueltos deja fuera los Create/Update. Son conjuntos casi
     * disjuntos.
     */
    private static boolean transportaCompanyId(JavaMethod method) {
        for (JavaClass parameter : method.getRawParameterTypes()) {
            if (!isOwnCode(parameter)) {
                continue;
            }
            for (JavaField field : parameter.getAllFields()) {
                if ("companyId".equals(field.getName())) {
                    return true;
                }
            }
        }
        return tieneParametroLlamadoCompanyId(method);
    }

    /**
     * ArchUnit no expone los nombres de parámetro, así que se reflexiona la clase
     * real. Funciona porque Spring Boot compila con {@code -parameters}; si no
     * estuviera, los nombres serían {@code arg0}, {@code arg1}… y este chequeo
     * simplemente no encontraría nada.
     */
    private static boolean tieneParametroLlamadoCompanyId(JavaMethod method) {
        try {
            for (Parameter parameter : method.reflect().getParameters()) {
                if ("companyId".equals(parameter.getName())) {
                    return true;
                }
            }
        } catch (RuntimeException | NoClassDefFoundError ignored) {
            // La clase no se puede cargar; el chequeo por command sigue aplicando.
        }
        return false;
    }

    /**
     * Exige que un listado que no filtra por empresa quede cerrado a
     * {@code ROLE_SYSTEM}.
     *
     * <p>
     * Es el hueco por el que se coló BE-29.
     * {@link #validarElTenantCuandoRecibeCompanyId()} mira los puertos <em>que
     * reciben</em> un {@code companyId}: si el método no recibe ninguno no hay nada
     * que validar, así que el puerto pasa limpio aunque por debajo esté sirviendo
     * filas de todas las empresas. Desde esa regla, un catálogo global legítimo y
     * una fuga entre empresas se ven exactamente igual.
     *
     * <p>
     * Lo que los distingue es el repositorio. Si <strong>sabe</strong> filtrar por
     * empresa —declara algún método que recibe {@code companyId}, del tipo
     * {@code findByIdAndCompanyId}— entonces sus filas son de alguien, y servirlas
     * sin ese filtro a un permiso de empleado es una fuga. Los catálogos maestros
     * (ciudades, razas, especies) no declaran ningún método así y la regla ni los
     * mira.
     *
     * <p>
     * A propósito no se comprueba la <em>forma</em> de la llamada: da igual que sea
     * {@code findAll()}, {@code findAll(page, size)} o
     * {@code findAllByAnimalId(id, …)}. Paginar un listado no lo hace multi-tenant,
     * y acotarlo por una FK ajena tampoco: mientras el {@code WHERE} no nombre la
     * empresa, las filas que salen son de cualquiera.
     */
    static ArchCondition<JavaClass> cerrarASystemLosListadosSinEmpresa() {
        return new ArchCondition<>("cerrar a ROLE_SYSTEM los listados que no filtran por empresa") {
            @Override
            public void check(JavaClass service, ConditionEvents events) {
                for (JavaMethod method : service.getMethods()) {
                    Optional<JavaMethod> puerto = puertoQueImplementa(method);
                    if (puerto.isEmpty() || transportaCompanyId(puerto.get())) {
                        continue;
                    }
                    for (JavaMethodCall call : method.getMethodCallsFromSelf()) {
                        Optional<JavaMethod> listado = listadoSinEmpresa(call);
                        if (listado.isPresent()) {
                            events.add(evaluarElGate(puerto.get(), method, listado.get()));
                        }
                    }
                }
            }
        };
    }

    /**
     * El método homónimo del puerto de entrada que este service implementa, si lo
     * hay. Un método que no implementa ningún puerto no es alcanzable desde fuera:
     * su gate es el del puerto que acabe llamándolo.
     */
    private static Optional<JavaMethod> puertoQueImplementa(JavaMethod method) {
        String[] parameterTypeNames = method.getRawParameterTypes().stream().map(JavaClass::getName)
                .toArray(String[]::new);
        for (JavaClass implemented : method.getOwner().getAllRawInterfaces()) {
            if (!implemented.getPackageName().contains(PORT_IN_PACKAGE)) {
                continue;
            }
            Optional<JavaMethod> candidato = implemented.tryGetMethod(method.getName(),
                    parameterTypeNames);
            if (candidato.isPresent()) {
                return candidato;
            }
        }
        return Optional.empty();
    }

    /**
     * La llamada, si es a un listado sin empresa de un repositorio que sí sabe
     * filtrar por ella.
     */
    private static Optional<JavaMethod> listadoSinEmpresa(JavaMethodCall call) {
        JavaClass repositorio = call.getTargetOwner();
        if (!isOwnCode(repositorio) || !repositorio.getPackageName().contains(PORT_OUT_PACKAGE)) {
            return Optional.empty();
        }
        Optional<JavaMethod> destino = call.getTarget().resolveMember();
        if (destino.isEmpty() || !esUnFinderDeVariasFilas(destino.get())
                || filtraPorEmpresa(destino.get())) {
            return Optional.empty();
        }
        boolean sabeFiltrarPorEmpresa = repositorio.getMethods().stream()
                .anyMatch(VetSoftwareConditions::filtraPorEmpresa);
        return sabeFiltrarPorEmpresa ? destino : Optional.empty();
    }

    private static SimpleConditionEvent evaluarElGate(JavaMethod puerto, JavaMethod servicio,
            JavaMethod listado) {
        Optional<PreAuthorize> gate = puerto.tryGetAnnotationOfType(PreAuthorize.class);
        boolean cerrado = gate.isPresent() && soloAlcanzablePorSystem(gate.get().value());
        String hecho = servicio.getOwner().getSimpleName() + "." + servicio.getName() + "() sirve "
                + listado.getOwner().getSimpleName() + "." + listado.getName()
                + "(), que no filtra por empresa, y su puerto no recibe companyId";
        return new SimpleConditionEvent(servicio, cerrado,
                cerrado
                        ? hecho + ", pero solo lo alcanza ROLE_SYSTEM"
                        : hecho + " ni esta cerrado a ROLE_SYSTEM: "
                                + gate.map(PreAuthorize::value).orElse("sin @PreAuthorize"));
    }

    /**
     * {@code true} si el método lleva la empresa consigo. El nombre cuenta porque
     * los repositorios la declaran ahí ({@code findByIdAndCompanyId},
     * {@code findAllAvailableForCompany}) y así el chequeo no depende de que la
     * clase se pueda reflexionar para leer los nombres de parámetro.
     */
    private static boolean filtraPorEmpresa(JavaMethod method) {
        return transportaCompanyId(method) || method.getName().contains("Company");
    }

    /**
     * Solo los <em>finders</em> que devuelven varias filas. El nombre importa: un
     * {@code saveAll} también devuelve una colección y no es un listado.
     */
    private static boolean esUnFinderDeVariasFilas(JavaMethod method) {
        if (!method.getName().startsWith("find")) {
            return false;
        }
        JavaClass returnType = method.getRawReturnType();
        return returnType.isAssignableTo(Collection.class)
                || PAGE_RESULT.equals(returnType.getSimpleName());
    }

    /**
     * Exige {@code @EntityGraph} en todo finder que devuelva una entidad JPA con
     * asociaciones {@code @ManyToOne}: sin él, cada fila hidrata su proxy LAZY con
     * una consulta aparte (N+1).
     *
     * <p>
     * La regla del CLAUDE.md habla de {@code findAll}/{@code findById}, pero en
     * este código los repositorios multi-tenant no los sobreescriben —declaran su
     * equivalente por empresa, del tipo {@code findAllByCompany_Id}—. Mirar solo
     * los dos nombres canónicos dejaría fuera justamente los finders que se usan.
     * Una {@code @Query} propia también vale: ahí el {@code JOIN FETCH} se escribe
     * a mano.
     */
    static ArchCondition<JavaClass> declararEntityGraphEnLosFinders() {
        return new ArchCondition<>(
                "declarar @EntityGraph en los finders que devuelven la entidad") {
            @Override
            public void check(JavaClass repository, ConditionEvents events) {
                Optional<JavaClass> entity = entidadDe(repository);
                if (entity.isEmpty() || !tieneManyToOne(entity.get())) {
                    return;
                }
                for (JavaMethod finder : repository.getMethods()) {
                    if (!devuelveLaEntidad(finder, entity.get())) {
                        continue;
                    }
                    boolean resuelto = finder.isAnnotatedWith(EntityGraph.class)
                            || finder.isAnnotatedWith(Query.class);
                    events.add(new SimpleConditionEvent(finder, resuelto,
                            repository.getSimpleName() + "." + finder.getName() + "() devuelve "
                                    + entity.get().getSimpleName()
                                    + " (con @ManyToOne) sin @EntityGraph ni @Query: N+1"));
                }
            }
        };
    }

    /**
     * {@code true} si el método devuelve la entidad, sola o envuelta en
     * Optional/List/Page.
     */
    private static boolean devuelveLaEntidad(JavaMethod method, JavaClass entity) {
        JavaType returnType = method.getReturnType();
        if (returnType.toErasure().equals(entity)) {
            return true;
        }
        if (returnType instanceof JavaParameterizedType parameterized) {
            return parameterized.getActualTypeArguments().stream()
                    .anyMatch(argument -> argument.toErasure().equals(entity));
        }
        return false;
    }

    /**
     * Primer argumento genérico de {@code JpaRepository<Entidad, Id>}, si se
     * declara directo.
     */
    private static Optional<JavaClass> entidadDe(JavaClass repository) {
        for (JavaType implemented : repository.getInterfaces()) {
            if (!(implemented instanceof JavaParameterizedType parameterized)) {
                continue;
            }
            if (!parameterized.toErasure().isAssignableTo(JpaRepository.class)) {
                continue;
            }
            List<JavaType> arguments = parameterized.getActualTypeArguments();
            if (!arguments.isEmpty()) {
                return Optional.of(arguments.get(0).toErasure());
            }
        }
        return Optional.empty();
    }

    private static boolean tieneManyToOne(JavaClass entity) {
        return entity.getAllFields().stream().anyMatch(f -> f.isAnnotatedWith(ManyToOne.class));
    }

    private static List<String> describe(List<JavaMethod> path, JavaMethodCall finalCall) {
        List<String> steps = new ArrayList<>();
        for (JavaMethod method : path) {
            steps.add(method.getOwner().getSimpleName() + "." + method.getName() + "()");
        }
        steps.add(finalCall.getTargetOwner().getSimpleName() + "." + finalCall.getTarget().getName()
                + "()");
        return steps;
    }

    // ── BE-10: cada adaptador con su rodaja ──────────────────────────────────

    /** Sufijo de las rodajas de persistencia; failsafe solo recoge {@code *IT}. */
    private static final String SUFIJO_RODAJA_PERSISTENCIA = "IT";

    /**
     * Sufijo de las rodajas web; surefire solo recoge {@code *Test}/{@code *Tests}.
     */
    private static final String SUFIJO_RODAJA_WEB = "Test";

    /** Marca de {@code target/test-classes} en el URI de origen de una clase. */
    private static final String SALIDA_DE_TEST = "/test-classes/";

    /**
     * Deja fuera lo que no es código de producción. El {@code @AnalyzeClasses} de
     * {@link PiramideDeTestsTest} importa {@code src/test} a propósito —lo necesita
     * para <em>encontrar</em> las rodajas—, y eso mete en el universo cosas que
     * parecen adaptadores sin serlo.
     *
     * <p>
     * El caso real: {@code GlobalExceptionHandlerTest.BoomController}, un
     * {@code @RestController} anidado de juguete cuyas rutas lanzan cada excepción
     * que se quiere mapear. Sin este filtro la regla exigía un
     * {@code BoomControllerTest} —una rodaja para un fixture—, y esa violación
     * fantasma habría entrado al store congelada, contaminando el indicador desde
     * el primer día.
     *
     * <p>
     * Dos filtros, porque hacen falta los dos: el URI de origen descarta lo que
     * viene de {@code target/test-classes} (la misma señal que usa
     * {@code ImportOption.DoNotIncludeTests}), y {@code getEnclosingClass} descarta
     * las anidadas, que nunca son un adaptador ni un controller de producción.
     */
    static DescribedPredicate<JavaClass> sonCodigoDeProduccion() {
        return DescribedPredicate.describe("son codigo de produccion",
                VetSoftwareConditions::esCodigoDeProduccion);
    }

    private static boolean esCodigoDeProduccion(JavaClass javaClass) {
        return javaClass.getEnclosingClass().isEmpty() && !vieneDelArbolDeTest(javaClass);
    }

    /**
     * La mitad de {@link #sonCodigoDeProduccion()} que mira <em>de dónde salió el
     * {@code .class}</em>, sin el filtro de clases anidadas.
     *
     * <p>
     * Existe separada porque hay una regla —{@code DOBLE_DE_TEST_NO_ESCANEABLE}— a
     * la que el filtro de anidadas le sobra, y no en el sentido de que le dé igual:
     * las clases anidadas son <strong>el 100 %</strong> de lo que esa regla
     * persigue, así que negar {@code sonCodigoDeProduccion()} entero descartaría
     * exactamente los casos que hay que cazar. Extraída aquí para que las dos
     * reglas compartan la única constante que dice qué es
     * {@code target/test-classes}: dos copias de esa cadena es como una de las dos
     * se queda atrás.
     */
    static DescribedPredicate<JavaClass> vienenDelArbolDeTest() {
        return DescribedPredicate.describe("vienen del arbol de test",
                VetSoftwareConditions::vieneDelArbolDeTest);
    }

    /**
     * Sin origen conocido no se puede afirmar que sea de test: se trata como
     * producción, igual que hacía el chequeo del que se extrajo.
     */
    private static boolean vieneDelArbolDeTest(JavaClass javaClass) {
        return javaClass.getSource()
                .map(source -> source.getUri().toString().contains(SALIDA_DE_TEST)).orElse(false);
    }

    /**
     * Exige que todo adaptador {@code JpaXxxRepository} tenga en su mismo paquete
     * una rodaja de persistencia: una clase {@code *IT} cuyo nombre contenga el
     * núcleo del adaptador ({@code JpaStockLotRepository} →
     * {@code StockLotPersistenceIT}).
     *
     * <p>
     * <strong>Por qué el nombre y no solo el paquete.</strong> Bastaría con exigir
     * «algún {@code *IT} en el paquete», pero entonces una sola rodaja taparía los
     * dieciséis adaptadores de {@code animal.infrastructure.persistence} y el
     * indicador dejaría de contar lo que importa. Cruzar por el núcleo del nombre
     * hace que la unidad de medida sea el adaptador, que es la unidad de riesgo:
     * cada uno traduce su propio SQL y su propio {@code Sort}.
     *
     * <p>
     * <strong>Qué queda fuera, a propósito.</strong> Solo entran los adaptadores
     * que siguen el naming del CLAUDE.md ({@code Jpa} + entidad +
     * {@code Repository}). Los {@code JpaXxxQueryPort},
     * {@code JpaXxxValidationPort} y los adaptadores sueltos
     * ({@code NumberingAllocationAdapter}, {@code DianJobLeaseAdapter}) no se
     * exigen: son proyecciones de una consulta o de un lease, y su rodaja aporta
     * bastante menos que la del adaptador que escribe. Ampliar el alcance es
     * ampliar el predicado, no relajar esta condición.
     */
    static ArchCondition<JavaClass> tenerRodajaDePersistencia() {
        return new ArchCondition<>("tener una rodaja *IT en su mismo paquete") {
            @Override
            public void check(JavaClass adaptador, ConditionEvents events) {
                String nucleo = nucleoDelAdaptador(adaptador.getSimpleName());
                boolean cubierto = hermanasDePaquete(adaptador)
                        .anyMatch(nombre -> nombre.endsWith(SUFIJO_RODAJA_PERSISTENCIA)
                                && nombre.contains(nucleo));
                events.add(new SimpleConditionEvent(adaptador, cubierto,
                        adaptador.getSimpleName() + " no tiene rodaja de persistencia: falta "
                                + nucleo + "PersistenceIT (@DataJpaTest) en "
                                + adaptador.getPackageName()));
            }
        };
    }

    /**
     * Exige que todo {@code @RestController} tenga en su mismo paquete una rodaja
     * web: una clase {@code *Test} cuyo nombre empiece por el del controller
     * ({@code AnimalController} → {@code AnimalControllerTest}).
     *
     * <p>
     * <strong>Por qué el prefijo y no «algún {@code *Test} en el paquete».</strong>
     * Porque en {@code infrastructure/web} conviven rodajas y tests unitarios
     * corrientes: {@code RefreshTokenCookieTest}, {@code CashArqueoCsvTest} e
     * {@code InventoryCsvTest} son JUnit puro sobre un helper, no ejercitan ningún
     * endpoint, y un criterio por paquete los contaría como red que no existe.
     * Exigir el prefijo del controller elimina los tres falsos positivos sin
     * enumerar excepciones.
     *
     * <p>
     * <strong>El {@code GlobalExceptionHandler} no cuenta como controller</strong>:
     * es {@code @RestControllerAdvice}, una anotación distinta que no está
     * meta-anotada con {@code @RestController}, así que el predicado ni lo mira.
     */
    static ArchCondition<JavaClass> tenerRodajaWeb() {
        return new ArchCondition<>("tener una rodaja *Test en su mismo paquete") {
            @Override
            public void check(JavaClass controller, ConditionEvents events) {
                String esperado = controller.getSimpleName() + SUFIJO_RODAJA_WEB;
                boolean cubierto = hermanasDePaquete(controller)
                        .anyMatch(nombre -> nombre.endsWith(SUFIJO_RODAJA_WEB)
                                && nombre.startsWith(controller.getSimpleName()));
                events.add(new SimpleConditionEvent(controller, cubierto,
                        controller.getSimpleName() + " no tiene rodaja web: falta " + esperado
                                + " (@WebMvcTest) en " + controller.getPackageName()));
            }
        };
    }

    /**
     * {@code JpaStockLotRepository} → {@code StockLot}. El núcleo es lo que
     * comparten el adaptador y su rodaja; el prefijo {@code Jpa} y el sufijo
     * {@code Repository} son ruido del naming.
     */
    private static String nucleoDelAdaptador(String simpleName) {
        String nucleo = simpleName.substring("Jpa".length());
        return nucleo.substring(0, nucleo.length() - "Repository".length());
    }

    /**
     * Nombres simples de las clases de nivel superior del mismo paquete —de los dos
     * árboles, porque el {@code @AnalyzeClasses} de {@code PiramideDeTestsTest} sí
     * importa {@code src/test}—. Sin subpaquetes: un test de {@code web/request} no
     * es la rodaja del controller. Sin clases anidadas: las {@code @Nested} de una
     * rodaja no son rodajas.
     */
    private static Stream<String> hermanasDePaquete(JavaClass javaClass) {
        return javaClass.getPackage().getClasses().stream()
                .filter(hermana -> hermana.getEnclosingClass().isEmpty())
                .filter(hermana -> !hermana.equals(javaClass)).map(JavaClass::getSimpleName);
    }
}
