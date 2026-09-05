---
name: vs-agente-base-backend
description: Copia versionada de vs-agente-base (reglas comunes de los subagentes) para sesiones abiertas dentro de VetSoftware/; en la raíz manda .claude/skills/vs-agente-base. No hace falta invocarla.
user-invocable: false
---

# Base común de los subagentes de VetSoftware

## Lo que ya tienes en contexto — no lo vuelvas a leer

Recibes automáticamente el `CLAUDE.md` de la raíz, la política de comentarios
(`.claude/rules/code-comments.md`) y, en cuanto lees cualquier fichero de un repo, el
`CLAUDE.md` de ese repo y las reglas de su `.claude/rules/`. **No hagas `Read` de ningún
`CLAUDE.md`**: lo duplicarías (el del backend son ~17k tokens; el del tenant, ~10k). Si
necesitas una sección concreta que no ves, `Grep` el encabezado y lee solo esas líneas.

## Herramientas, en este orden

1. **`codegraph_explore` primero**, una llamada con varios nombres (clase, puerto, vista,
   store, composable, test). Devuelve fuente verbatim numerada, llamadores y _blast radius_;
   en Vue sigue props, emits, slots y re-render, que `grep` no ve. Nombres genéricos
   (`module`, `service`, `store`) devuelven ruido: afina o usa `codegraph query "NombreExacto"`.
   Para Terraform **solo** `codegraph query`/`node` (`explore` contesta Java). Un índice, en
   `.codegraph/` de la raíz, para los cuatro repos; los dos fronts comparten rutas relativas:
   mira el prefijo del repo antes de editar.
2. **`Read`/`Grep` solo para lo que el grafo no indexa**: `pom.xml`, changesets, `openapi.json`,
   `package.json`, `*.config.ts`, YAML, `.terraform.lock.hcl`, snapshots y artefactos de CI.
3. **`mcp__idea__get_file_problems` sobre cada fichero que escribas, antes de cualquier gate.**
   Un segundo por fichero, en los cuatro repos (`.java`, `.ts`, `.vue`, `.tf`): imports rotos,
   tipos mal resueltos, variables sin declarar. `errorsOnly: true` mientras iteras. En los
   gemelos TR-02 marca _Duplicated code fragment_: es política, no hallazgo.
4. **`mcp__idea__analyze_calls`** (`INCOMING_CALLS`, `depth=2`) antes de cambiar una firma o un
   tipo compartido. Si expira, **no reintentes con otra profundidad**: cae al _blast radius_ de
   CodeGraph. Un árbol vacío sí es respuesta (cero llamadores).
5. Lecturas sin dependencia entre sí: **todas en un mismo mensaje**. Escrituras de ficheros
   independientes: en lote, no de una en una.

## Recursos únicos — uno a la vez por repo

Cada repo tiene recursos que no admiten dos procesos a la vez. En un abanico, **el brief
nombra a la única instancia que los usa**; las demás verifican con IntelliJ y dejan los
comandos escritos para quien los lance. El protocolo por niveles de cada repo, con sus costes
medidos, entra solo en tu contexto con el primer `Read` de un fichero suyo:

| Repo                                         | Recurso único                                                                                                                          | Protocolo                                           |
| -------------------------------------------- | -------------------------------------------------------------------------------------------------------------------------------------- | --------------------------------------------------- |
| `VetSoftware`                                | Maven: `target/`, informes, `config/archunit/violation-store`                                                                          | `VetSoftware/.claude/rules/verificacion-backend.md` |
| `VetSoftwareFront`, `VetSoftwarePublicFront` | por repo: el servidor de dev (5173 / 5174), el navegador de Playwright y `test-results/`; `dist/`; los `*.tsbuildinfo` de `vue-tsc -b` | `<front>/.claude/rules/verificacion-front.md`       |
| `VetSoftwareIaC`                             | el `.terraform/` de cada root, `tflint --init` y el lock del state remoto (`plan`/`apply` van por workflow)                            | `VetSoftwareIaC/.claude/rules/verificacion-iac.md`  |

## Esperas largas — nunca mirar la barra

Todo comando de más de ~30 s va en segundo plano (`run_in_background`) con el código de salida
a fichero: `… > salida.log 2>&1; echo $? > salida.exit`. Tres estados y ninguno se solapa:

| Proceso | `.exit`  | Significa                                                          |
| ------- | -------- | ------------------------------------------------------------------ |
| vivo    | ausente  | corriendo: sigue trabajando                                        |
| ausente | presente | terminó: lee el veredicto del fichero, no del log                  |
| ausente | ausente  | **murió con el shell**: relanza una vez; si muere dos, para y dilo |

Arráncalo en cuanto el árbol esté consistente, nunca al final. Mientras corre: solo lectura
(`codegraph`, `Read`, IntelliJ, `git status/diff`), redacta el informe y los cuerpos de issue
con huecos, prepara los comandos siguientes. **Nunca** edites ficheros que el comando compila
o sirve, ni lances otro proceso sobre el mismo recurso único, ni escribas en git, ni hagas
`sleep`/sondeo en bucle. `cmd | tail` devuelve el exit de `tail`: captura el código aparte y
lee el texto. Un workflow remoto (`gh run`) no avisa: consúltalo una vez pasado su tiempo
típico, no con `gh run watch` en primer plano.

## Cierre obligatorio — nada abierto sin issue

Lo que quede vivo al terminar —hallazgo sin arreglar, gate que no pudiste ejecutar, deuda
descubierta, decisión que necesita un humano— va a GitHub **antes** de tu respuesta final.
Abrir un issue no es un commit: no pide aprobación.

| Directorio                | Repositorio                             |
| ------------------------- | --------------------------------------- |
| `VetSoftware/`            | `kefaroTech/vetsoftware-backend`        |
| `VetSoftwareFront/`       | `kefaroTech/vetsoftware-admin-web`      |
| `VetSoftwarePublicFront/` | `kefaroTech/vetsoftware-public-web`     |
| `VetSoftwareIaC/`         | `kefaroTech/vetsoftware-infrastructure` |

- Busca antes: `gh issue list --repo <owner/repo> --state all --search "<claves>"`. Si existe,
  `gh issue comment <n>` y reporta ese número. **Los números de issue son por repositorio.**
- Cuerpo en fichero (`--body-file`), nunca inline: título en español que nombra el **problema**,
  evidencia `archivo:línea`, escenario concreto de fallo (sin escenario no es hallazgo), qué haría
  falta para cerrarlo, qué no comprobaste. Cierra con
  `🤖 Generated with [Claude Code](https://claude.com/claude-code)`.
- Un hallazgo, un issue; cross-repo va donde está la causa. Lo arreglado y verificado hoy no lleva.
- Un gate que no pudiste ejecutar (Docker caído, dev apagado, red IPv6 al registro de Terraform
  caída) es límite del entorno: se declara `no ejecutado: <motivo>`, no es issue ni se da por
  pasado.

## Higiene de salida

- **JSON siempre recortado**: `gh --json campos --jq`, `aws --query --output text`, `jq -r` desde
  Bash (no desde PowerShell). Un pipe se traga el exit code: ante un comando sospechoso, sin pipe.
- Ficheros de scratchpad con prefijo de agente y repo (`ADMINWEB-cuerpo-issue.md`): el directorio
  es único por sesión y **compartido con los agentes hermanos**; relee antes de usar con `-F`.
- Reporta la salida real, incluidos los fallos. Nunca un veredicto sin el comando que lo produjo.
- Comentarios en el código: rige `.claude/rules/code-comments.md`. Antes de terminar, `git diff`
  y borra los que solo narran qué hace el código o cuentan la tarea. En un gemelo TR-02, ni se
  añade ni se quita un comentario sin replicarlo en el otro front.
