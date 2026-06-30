# Branch Protection Rules — usuario-service

## Configuración en GitHub (IE5)

Estas reglas se aplican en **Settings → Branches → Add rule** del repositorio GitHub.

### Rama: `main` / `master`

| Regla | Valor | Motivo |
|-------|-------|--------|
| Require a pull request before merging | ✅ Enabled | Ningún commit directo a producción |
| Required approvals | 1 | Al menos un revisor |
| Dismiss stale pull request approvals | ✅ Enabled | Re-revisar si hay nuevos commits |
| Require status checks to pass | ✅ Enabled | Ver checks requeridos abajo |
| Require branches to be up to date | ✅ Enabled | Evitar conflictos silenciosos |
| Require conversation resolution | ✅ Enabled | Todos los comentarios resueltos |
| Restrict who can push | Admins only | Solo admins pueden forzar push |
| Allow force pushes | ❌ Disabled | Proteger historial |
| Allow deletions | ❌ Disabled | No borrar ramas protegidas |

### Status Checks Requeridos

Los siguientes checks del pipeline CI deben pasar antes de merge:

```
- Tests & Quality Gate / Ejecutar Tests con Cobertura (JaCoCo)
- Tests & Quality Gate / Verificar Quality Gate de SonarQube
- Tests & Quality Gate / OWASP Dependency Check (Seguridad)
```

### Aplicar via GitHub CLI

```bash
gh api repos/{owner}/{repo}/branches/main/protection \
  --method PUT \
  --field required_status_checks='{"strict":true,"contexts":["Tests & Quality Gate"]}' \
  --field enforce_admins=false \
  --field required_pull_request_reviews='{"required_approving_review_count":1}' \
  --field restrictions=null
```
