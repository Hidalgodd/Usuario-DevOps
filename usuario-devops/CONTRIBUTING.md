# Guía de Contribución - Usuario Service v1.0

¡Gracias por sumarte al desarrollo de este microservicio! Para mantener el código limpio y el pipeline CI/CD funcionando, seguimos las siguientes normas:

---

## 🌿 Flujo de Trabajo (Git Flow)

1. No subir cambios directamente a `main` o `master`.
2. Crear una rama de trabajo con nombres descriptivos y los siguientes prefijos:
   - `feature/` para nuevas funcionalidades.
   - `fix/` para corregir errores.
   - `docs/` para documentación.
   - `refactor/` para mejoras de código.

---

## 🏗️ Estándares de Código (Clean Code)

Respete la arquitectura de **3 capas**:

- **Controller**: Solo recibe peticiones HTTP y entrega respuestas.
- **Service**: Aquí reside toda la **Lógica de Negocio**.
- **Repository**: Solo gestiona el acceso a los datos (JPA).

### Reglas de Oro

- **Lombok**: Use `@Data`, `@AllArgsConstructor` y `@NoArgsConstructor`.
- **Validaciones**: Use `@NotBlank`, `@Min`, `@Valid`.
- **Idiomas**: Mantenga los nombres de variables y métodos en español.
- **Secrets**: Jamás hardcodear contraseñas o tokens en el código. Use variables de entorno.

---

## 📝 Formato de Commits

Usamos mensajes de commit claros:

- ✅ `feat: agregar endpoint de registro de usuario`
- ✅ `fix: corregir error 500 al autenticar con token expirado`
- ✅ `docs: actualizar README con instrucciones de despliegue AWS`
- ❌ `update: cambios varios` (Evite mensajes genéricos).

---

## 🧪 Pruebas antes de enviar

Antes de solicitar la integración de su código (Pull Request), asegúrese de:

1. Que el proyecto **compile** correctamente (`./mvnw clean compile`).
2. Que los **tests pasen** y la cobertura sea ≥ 60% (`./mvnw verify -Pcoverage`).
3. Que el **script de auditoría** no reporte errores (`./scripts/audit-compliance.sh`).
4. Probar los endpoints en **Postman**.
5. Verificar que el archivo `README.md` esté actualizado.

---

## ⚙️ Pipeline CI/CD

Cada Pull Request ejecutará automáticamente:

1. Tests unitarios con reporte de cobertura JaCoCo.
2. Análisis estático en SonarQube (Quality Gate).
3. Escaneo de vulnerabilidades OWASP.

**El merge está bloqueado si alguno de estos pasos falla.**

---

**¡A programar con lógica!** 🚀
