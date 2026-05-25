---
applyTo: "**"
---
# Harness de Ejecución — LIS-InventoryPlatform

## ANTES DE GENERAR CUALQUIER CÓDIGO
1. Analiza los archivos existentes en el repo antes de escribir algo nuevo
2. Revisa el módulo /iam como referencia de patrones ya usados
3. No inventes dependencias, imports o clases que no existan en el proyecto
4. Si necesitas información que no encuentras en el repo, pregunta antes de asumir
5. Respeta los nombres de paquetes, clases y métodos ya existentes

## BACKEND — Monolito Modular

Orden obligatorio de implementación por capa:
1. entity/     → clase @Entity con atributos y relaciones JPA
2. dto/        → clases Request y Response (nunca exponer entity)
3. repository/ → interface extendiendo JpaRepository
4. service/    → lógica de negocio con @Service y @Transactional
5. controller/ → endpoints @RestController bajo /api/v1/{modulo}/

Si la US requiere un módulo nuevo:
- Crear paquete bajo com.lis.inventory/{modulo}/
- Replicar exactamente la estructura de /iam
- Registrar el módulo en copilot-instructions.md

Reglas:
- Siempre usar DTOs en controller, nunca entities
- Excepciones personalizadas van en shared/
- Validaciones con @Valid en los DTOs
- Documentar endpoints con JavaDoc

## UI — ReactJS + Bootstrap

Orden de implementación:
1. /services   → llamada Axios al endpoint del Backend
2. /hooks      → lógica de estado si aplica
3. /components → componentes Bootstrap reutilizables
4. /pages      → página que orquesta los componentes

Reglas:
- Componentes en PascalCase
- Manejo de errores en cada llamada Axios
- Usar componentes Bootstrap nativos
- Props tipadas con PropTypes