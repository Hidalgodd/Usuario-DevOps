#!/bin/bash
# =============================================================
# audit-compliance.sh
# Script de auditoría de cumplimiento para usuario-service
# IE5: Políticas de calidad y seguridad automatizadas
# =============================================================

set -e

ERRORS=0
WARNINGS=0

echo "=================================================="
echo " 🔍 AUDITORÍA DE CUMPLIMIENTO - usuario-service"
echo "=================================================="

# ── 1. Verificar rama protegida ──────────────────────────────
echo ""
echo "📋 [1/6] Verificando rama de trabajo..."
BRANCH=$(git rev-parse --abbrev-ref HEAD)
if [ "$BRANCH" = "master" ] || [ "$BRANCH" = "main" ]; then
  echo "  ❌ ERROR: No se permite hacer commit directo en '$BRANCH'."
  echo "     Crea una rama feature/ o bugfix/ y abre un Pull Request."
  ERRORS=$((ERRORS + 1))
else
  echo "  ✅ Rama: $BRANCH (OK)"
fi

# ── 2. Verificar que existan tests ──────────────────────────
echo ""
echo "📋 [2/6] Verificando existencia de tests..."
TEST_COUNT=$(find src/test -name "*Test.java" | wc -l)
if [ "$TEST_COUNT" -lt 2 ]; then
  echo "  ❌ ERROR: Se requieren al menos 2 clases de test. Encontradas: $TEST_COUNT"
  ERRORS=$((ERRORS + 1))
else
  echo "  ✅ Tests encontrados: $TEST_COUNT clases (OK)"
fi

# ── 3. Verificar cobertura mínima con JaCoCo ────────────────
echo ""
echo "📋 [3/6] Verificando cobertura de tests (JaCoCo)..."
JACOCO_REPORT="target/site/jacoco/jacoco.xml"
if [ ! -f "$JACOCO_REPORT" ]; then
  echo "  ⚠️  ADVERTENCIA: No se encontró reporte JaCoCo. Ejecuta: ./mvnw verify -Pcoverage"
  WARNINGS=$((WARNINGS + 1))
else
  # Extraer cobertura de líneas del XML
  COVERED=$(grep -o 'type="LINE"[^/]*/>' "$JACOCO_REPORT" | grep -o 'covered="[0-9]*"' | head -1 | grep -o '[0-9]*')
  MISSED=$(grep -o 'type="LINE"[^/]*/>' "$JACOCO_REPORT" | grep -o 'missed="[0-9]*"' | head -1 | grep -o '[0-9]*')
  if [ -n "$COVERED" ] && [ -n "$MISSED" ]; then
    TOTAL=$((COVERED + MISSED))
    if [ "$TOTAL" -gt 0 ]; then
      COVERAGE=$((COVERED * 100 / TOTAL))
      if [ "$COVERAGE" -lt 60 ]; then
        echo "  ❌ ERROR: Cobertura de líneas: $COVERAGE% (mínimo requerido: 60%)"
        ERRORS=$((ERRORS + 1))
      else
        echo "  ✅ Cobertura de líneas: $COVERAGE% (OK)"
      fi
    fi
  else
    echo "  ⚠️  ADVERTENCIA: No se pudo leer cobertura del reporte JaCoCo"
    WARNINGS=$((WARNINGS + 1))
  fi
fi

# ── 4. Verificar secrets no hardcodeados ────────────────────
echo ""
echo "📋 [4/6] Escaneando secrets hardcodeados..."
PATTERNS=("password\s*=\s*['\"][^$][^'\"]{3,}" "secret\s*=\s*['\"][^$][^'\"]{5,}" "api.key\s*=\s*['\"][^$]")
FOUND_SECRETS=0
for PATTERN in "${PATTERNS[@]}"; do
  MATCHES=$(grep -rniE "$PATTERN" src/main/ --include="*.java" --include="*.properties" --include="*.yml" 2>/dev/null | grep -v "test\|example\|placeholder" || true)
  if [ -n "$MATCHES" ]; then
    echo "  ❌ POSIBLE SECRET HARDCODEADO:"
    echo "$MATCHES" | head -5
    FOUND_SECRETS=1
    ERRORS=$((ERRORS + 1))
  fi
done
if [ "$FOUND_SECRETS" -eq 0 ]; then
  echo "  ✅ No se encontraron secrets hardcodeados (OK)"
fi

# ── 5. Verificar Dockerfile buenas prácticas ────────────────
echo ""
echo "📋 [5/6] Verificando Dockerfile..."
if [ ! -f "Dockerfile" ]; then
  echo "  ❌ ERROR: No existe Dockerfile"
  ERRORS=$((ERRORS + 1))
else
  # Verificar que no se ejecute como root
  if grep -q "USER root" Dockerfile; then
    echo "  ❌ ERROR: Dockerfile usa USER root (riesgo de seguridad)"
    ERRORS=$((ERRORS + 1))
  else
    echo "  ✅ Dockerfile no usa root (OK)"
  fi
  # Verificar que use imagen base específica (no :latest en FROM)
  BASE_IMAGE=$(grep "^FROM" Dockerfile | head -1)
  if echo "$BASE_IMAGE" | grep -q ":latest"; then
    echo "  ⚠️  ADVERTENCIA: Dockerfile usa imagen :latest — fija una versión específica"
    WARNINGS=$((WARNINGS + 1))
  else
    echo "  ✅ Imagen base con versión fija (OK)"
  fi
fi

# ── 6. Verificar actuator expuesto solo lo necesario ────────
echo ""
echo "📋 [6/6] Verificando configuración de Actuator..."
EXPOSED=$(grep "management.endpoints.web.exposure.include" src/main/resources/application.properties 2>/dev/null || true)
if echo "$EXPOSED" | grep -q "\*"; then
  echo "  ❌ ERROR: Actuator expone todos los endpoints (*). Limita a: prometheus,health,info"
  ERRORS=$((ERRORS + 1))
else
  echo "  ✅ Actuator con endpoints controlados (OK)"
fi

# ── Resumen Final ────────────────────────────────────────────
echo ""
echo "=================================================="
echo " RESULTADO DE AUDITORÍA"
echo "=================================================="
echo "  Errores críticos : $ERRORS"
echo "  Advertencias     : $WARNINGS"
echo ""

if [ "$ERRORS" -gt 0 ]; then
  echo "  ❌ AUDITORÍA FALLIDA — Corrige los errores antes de continuar."
  echo "     El pipeline CI/CD se detendrá hasta que se resuelvan."
  exit 1
else
  echo "  ✅ AUDITORÍA APROBADA — El proyecto cumple las políticas de calidad."
  exit 0
fi
