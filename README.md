# 🏥 HomeMed — Microservicio Usuario DevOps

Pipeline CI/CD completo con monitoreo, dashboards, cumplimiento automatizado y despliegue en AWS ECS.

---

## 📐 Arquitectura General

```
Developer → GitHub → CI Pipeline → Quality Gate → Docker Push → AWS ECS (producción)
                         ↓                                          ↓
                   SonarQube +                              CloudWatch Logs
                   JaCoCo +                                 + Prometheus
                   OWASP                                    + Grafana Dashboard
```

---

## 🔁 Pipeline CI/CD — Integración de Herramientas (IE4)

### Flujo completo

```
push a feature/** ──→ [ci.yml]
                         │
                    ┌────▼────────────────────────┐
                    │  JOB 1: test-and-quality     │
                    │                              │
                    │  1. ./mvnw verify -Pcoverage │  ← JaCoCo genera reporte
                    │  2. mvn sonar:sonar          │  ← Análisis estático
                    │  3. Verificar Quality Gate   │  ← API SonarQube
                    │  4. OWASP Dependency Check   │  ← CVE score ≥ 7 = falla
                    └────┬────────────────────────┘
                         │ ✅ Solo si todo pasó
                    ┌────▼────────────────────────┐
                    │  JOB 2: build-and-push       │
                    │                              │
                    │  5. docker build & push ECR  │
                    └────┬────────────────────────┘
                         │ merge a main
                    ┌────▼────────────────────────┐
                    │  [deploy.yml]                │
                    │                              │
                    │  6. ECS update service       │
                    │  7. Esperar estabilidad       │
                    └─────────────────────────────┘
```

### Decisiones técnicas que habilita el pipeline

| Señal del pipeline | Decisión técnica |
|---|---|
| Quality Gate rojo (cobertura < 60%) | PR bloqueado automáticamente, developer debe agregar tests |
| CVE crítico (score ≥ 7) en dependencias | Build falla, se actualiza versión de la dependencia vulnerable |
| Métricas de error rate > 0.5 rps en Grafana | Rollback manual o auto-scaling |
| Latencia P95 > 1000ms en Grafana | Investigar trazas en Tempo, optimizar query DB |
| Logs ERROR en Loki | Alertar equipo, abrir issue en GitHub |

---

## 📊 Monitoreo (IE1)

### Stack de observabilidad

| Herramienta | Puerto | Función |
|---|---|---|
| **Prometheus** | 9090 | Scrape de métricas `/actuator/prometheus` cada 15s |
| **Loki** | 3100 | Almacena logs JSON de Spring Boot |
| **Promtail** | — | Recolecta logs de `./logs/*.log` y los envía a Loki |
| **Tempo** | 3200/4318 | Almacena trazas distribuidas OpenTelemetry |
| **Grafana** | 3000 | Dashboard unificado: métricas + logs + trazas |

### Métricas expuestas por Spring Boot

- `http_server_requests_seconds_*` — latencia y RPS por endpoint
- `jvm_memory_used_bytes` / `jvm_memory_max_bytes` — heap JVM
- `process_cpu_usage` — CPU del proceso
- `hikaricp_connections_*` — pool de conexiones MySQL
- `jvm_threads_live_threads` — threads activos

### Levantar monitoreo local

```bash
docker compose up -d
# Grafana → http://localhost:3000  (admin/admin)
# Prometheus → http://localhost:9090
```

---

## 🖥️ Dashboard Grafana (IE3)

Dashboard precargado automáticamente vía **provisioning** en:
`grafana/provisioning/` + `grafana/dashboards/usuario-devops.json`

### Métricas clave incluidas

| Panel | Métrica | Umbral |
|---|---|---|
| Disponibilidad | `up{job="usuario-service"}` | 0 = DOWN (rojo) |
| RPS | `rate(http_server_requests_seconds_count[1m])` | — |
| Tasa de errores | `rate(...{status=~"4\|5.."}[1m])` | > 0.5 = rojo |
| Latencia P95 | `histogram_quantile(0.95, ...)` | > 1000ms = rojo |
| CPU | `process_cpu_usage` | > 80% = amarillo |
| Memoria Heap | `jvm_memory_used_bytes` vs `max` | — |
| Conexiones DB | `hikaricp_connections_active` | — |
| Logs ERROR | Loki: `{app="usuario-service"} \|= "ERROR"` | — |
| Cobertura tests | `usuario_service_test_coverage_percent` | < 60% = rojo |

---

## ✅ Políticas de Cumplimiento (IE5)

### 1. SonarQube Quality Gate

Configurado en `sonar-project.properties`. El Quality Gate falla si:
- Cobertura de líneas < 60%
- Code Smells críticos > 0
- Vulnerabilidades de seguridad > 0
- Duplicación de código > 15%

### 2. OWASP Dependency Check

Escanea todas las dependencias Maven contra la base de datos CVE de NIST.
**Falla el build** si se detecta alguna vulnerabilidad con CVSS score ≥ 7.

### 3. Branch Protection en GitHub

Ver [`scripts/branch-protection.md`](scripts/branch-protection.md) para la configuración detallada.
Resumen: merge a `main` requiere:
- Pull Request con al menos 1 aprobación
- Todos los checks de CI aprobados
- Rama actualizada con main

### 4. Script de auditoría personalizado

```bash
chmod +x scripts/audit-compliance.sh
./scripts/audit-compliance.sh
```

Verifica: rama correcta, cantidad de tests, cobertura, secrets hardcodeados, buenas prácticas en Dockerfile, Actuator controlado.

---

## 🛑 Pipeline se detiene ante fallas (IE6)

### Escenarios demostrados

**Escenario A — Cobertura insuficiente:**
```
./mvnw verify -Pcoverage
→ [ERROR] Rules check: Line coverage 45% is below minimum 60%
→ BUILD FAILURE ← Pipeline detenido
```

**Escenario B — Quality Gate SonarQube:**
```
Quality Gate status: FAILED
❌ PIPELINE DETENIDO: Quality Gate falló. Revisa SonarQube.
→ exit 1 ← Job build-and-push no corre (needs: test-and-quality)
```

**Escenario C — Vulnerabilidad crítica OWASP:**
```
[ERROR] Dependency-check:check failed: 
  CVE-2024-XXXX (score: 9.8) found in log4j-core-2.14.1.jar
→ BUILD FAILURE ← Imagen Docker nunca se construye
```

**Escenario D — Secret hardcodeado (script auditoría):**
```
❌ POSIBLE SECRET HARDCODEADO: src/main/resources/application.properties:5
  password = "admin123"
❌ AUDITORÍA FALLIDA — Corrige los errores antes de continuar.
→ exit 1
```

---

## ☁️ Despliegue en AWS ECS (IE2)

### Secrets requeridos en GitHub

| Secret | Descripción |
|---|---|
| `AWS_ACCESS_KEY_ID` | Credencial AWS |
| `AWS_SECRET_ACCESS_KEY` | Credencial AWS |
| `AWS_SESSION_TOKEN` | Token de sesión (Lab AWS) |
| `AWS_REGION` | Región (ej: `us-east-1`) |
| `ECR_REPO_URL_USUARIO` | URL del repositorio ECR |
| `ECS_TASK_DEFINITION_NAME` | Nombre de la Task Definition |
| `ECS_SERVICE_NAME` | Nombre del servicio ECS |
| `ECS_CLUSTER_NAME` | Nombre del cluster ECS |
| `SONAR_TOKEN` | Token de SonarQube/SonarCloud |
| `SONAR_HOST_URL` | URL de SonarQube |
| `DOCKERHUB_USERNAME` | Usuario Docker Hub |
| `DOCKERHUB_TOKEN` | Token Docker Hub |

### Trigger de despliegue

El deploy a AWS ECS se activa **solo con push a `main`** y **solo si el job de calidad pasó**.

---

## 🗂️ Estructura del proyecto

```
usuario-devops/
├── .github/workflows/
│   ├── ci.yml          ← Tests + Quality Gate + Docker Push
│   └── deploy.yml      ← Deploy a AWS ECS
├── grafana/
│   ├── provisioning/   ← Datasources y dashboards automáticos
│   └── dashboards/     ← Dashboard JSON precargado
├── prometheus/
│   └── prometheus.yml  ← Scrape config para usuario-service
├── promtail/           ← Recolección de logs
├── tempo/              ← Trazas distribuidas
├── k8s/
│   └── back.yml        ← Manifests Kubernetes
├── scripts/
│   ├── audit-compliance.sh   ← Auditoría personalizada (IE5)
│   └── branch-protection.md  ← Configuración branch protection
├── src/                ← Código Spring Boot
├── docker-compose.yml  ← Stack completo de observabilidad
├── Dockerfile
└── pom.xml             ← JaCoCo + SonarQube + OWASP configurados
```
