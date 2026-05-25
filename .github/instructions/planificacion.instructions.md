---
applyTo: "**"
---
# Harness de Planificación — LIS-InventoryPlatform

## ANTES DE GENERAR LA US
1. Verifica si ya existe funcionalidad similar en el repo
2. No asumas módulos o endpoints que no estén creados
3. Si el requerimiento es ambiguo, pregunta antes de continuar
4. Revisa el módulo /iam como referencia de funcionalidades ya implementadas

## 1. USER STORY
- ID: LIS-US-[número]
- Título:
- Como [Auxiliar | Jefe | SuperAdmin], quiero [acción], para [beneficio].
- Módulo afectado: [iam | nuevo módulo | ambos]
- Story Points: [1 | 2 | 3 | 5 | 8]

## 2. CRITERIOS DE ACEPTACIÓN

**Escenario 1 — Happy path:**
Given [contexto]
When [acción]
Then [resultado esperado]

**Escenario 2 — Error o validación:**
Given [contexto]
When [acción incorrecta]
Then [mensaje o comportamiento esperado]

**Escenario 3 — Edge case:**
Given [contexto límite]
When [acción]
Then [resultado esperado]

## 3. SUBTAREAS TÉCNICAS

**UI — ReactJS:**
- [ ] tarea (Xh)

**Backend — Spring Boot (Monolito Modular):**
Módulo: [nombre del módulo]
- [ ] entity/: (Xh)
- [ ] dto/: (Xh)
- [ ] repository/: (Xh)
- [ ] service/: (Xh)
- [ ] controller/: (Xh)

Estimación total: Xh

## 4. ALERTAS
Ambigüedades, dependencias entre módulos o riesgos. Omitir si no hay.