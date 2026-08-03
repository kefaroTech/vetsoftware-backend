# Contenedores y entrega

## Stack local completo

El archivo `docker-compose.override.yml` agrega backend y los dos fronts al stack de infraestructura. El perfil `application` evita construirlos cuando solo se necesitan las dependencias.

Desde este repositorio:

```powershell
$env:GRAFANA_PASSWORD = "use-un-valor-local-seguro"
docker compose --profile application up --build
```

Servicios de aplicación:

- API: `http://localhost:8080/api/v1`
- Front privado: `http://localhost:5173`
- Front público: `http://localhost:5174`
- Readiness del backend: `http://localhost:8080/api/v1/actuator/health/readiness`

El backend usa los nombres DNS de Compose (`mysql`, `redis`, `localstack` y `otel-collector`). Prometheus raspa `backend:8080`; ya no depende de una JVM ejecutada en el host.

Para iniciar únicamente las dependencias:

```powershell
$env:GRAFANA_PASSWORD = "use-un-valor-local-seguro"
docker compose up
```

## Imagen del backend

El `Dockerfile` compila con Maven y Java 25, y ejecuta con Eclipse Temurin JRE 25 sobre Ubuntu Noble. La imagen final:

- se construye para `linux/arm64`, igual que la task definition de ECS;
- corre como UID/GID `10001`, sin privilegios de root;
- incluye fuentes Noto y Fontconfig para OpenHTMLToPDF;
- usa el mismo endpoint de readiness que Docker, ECS y el target group;
- fija por digest las imágenes base.

El CI construye la imagen sin publicarla. Una release SemVer aprobada publica en ECR los tags inmutables `X.Y.Z` y `sha-<12 caracteres>`, resuelve el digest `sha256`, espera el escaneo ECR y bloquea la release ante hallazgos High o Critical.

Después de crear el tag y la GitHub Release, `publish-release.yml` solicita a `VetSoftwareIaC/deploy-backend.yml` un despliegue de producción con estos datos auditables: versión, digest, commit completo y URL del run que publicó la imagen. La autenticación entre repositorios usa un token efímero de GitHub App; el `GITHUB_TOKEN` del backend no se amplía ni se comparte.

La conexión externa queda deliberadamente pendiente hasta configurar GitHub. En el environment `production` se necesitarán la variable `IAC_DISPATCH_CLIENT_ID` y el secret `IAC_DISPATCH_PRIVATE_KEY`; `IAC_GITHUB_OWNER` e `IAC_GITHUB_REPOSITORY` son variables opcionales y usan `kefaroTech`/`VetSoftwareIaC` por omisión.
