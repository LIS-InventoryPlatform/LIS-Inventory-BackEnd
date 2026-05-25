# LIS-InventoryPlatform — Backend

Proyecto: Software de Inventario
Metodología: Scrum
Stack: Java + Spring Boot + PostgreSQL + REST
ORM: JPA / Hibernate (Spring Data JPA)
Arquitectura: Monolito Modular

Paquete base: com.lis.inventory

Estructura de módulos:
src/main/java/com/lis/inventory/
├── {modulo}/
│   ├── controller/
│   ├── dto/
│   ├── entity/
│   ├── repository/
│   └── service/
├── shared/
└── LisInventoryApplication.java

Módulos actuales:
- iam: autenticación y gestión de usuarios

Roles del sistema:
- SUPER_ADMIN, JEFE, AUXILIAR

Convenciones:
- Nuevos módulos siguen exactamente la misma estructura de /iam
- DTOs para request/response (nunca exponer entidades directamente)
- Endpoints versionados: /api/v1/{modulo}/...
- Manejo de excepciones con @ControllerAdvice en shared/
- @Transactional en la capa service/

Responde siempre en español.