---
applyTo: "**"
---
# Harness de Testing — LIS-InventoryPlatform

## ANTES DE GENERAR CUALQUIER TEST
1. Analiza las clases existentes en el repo antes de escribir algo nuevo
2. Revisa los tests existentes en src/test/ como referencia de patrones ya usados
3. No dupliques clases de test que ya existan, agrégale los métodos faltantes
4. No inventes dependencias, imports o clases que no existan en el proyecto
5. Si necesitas información que no encuentras en el repo, pregunta antes de asumir
6. Respeta los nombres de paquetes, clases y métodos ya existentes

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