---
applyTo: "**"
---
# Harness de Testing — LIS-InventoryPlatform

Cuando recibas una User Story implementada, genera los tests correspondientes:

## BACKEND — Spring Boot

Por cada capa del módulo genera:

**service/ → Tests unitarios con JUnit 5 + Mockito**
- Happy path de cada método público
- Casos de error y excepciones
- Mockear repository con @MockBean

**controller/ → Tests de integración con MockMvc**
- Un test por escenario Gherkin de la US
- Validar status HTTP y estructura del response
- Testear roles: SUPER_ADMIN, JEFE, AUXILIAR

**repository/ → Tests con @DataJpaTest**
- Solo si hay queries personalizadas (@Query)

Ubicación: src/test/java/com/lis/inventory/{modulo}/

## UI — ReactJS

**Componentes → Tests con React Testing Library**
- Renderizado correcto del componente
- Interacciones del usuario (click, input)
- Mockear llamadas Axios

Cobertura mínima:
- Happy path: obligatorio
- Error de API: obligatorio
- Edge cases de la US: obligatorio