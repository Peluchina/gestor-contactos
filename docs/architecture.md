# Gestor de Contactos — Arquitectura del Sistema

## 1. Objetivo

Este documento define la arquitectura técnica del sistema **Gestor de Contactos**.

La arquitectura debe permitir implementar los requisitos funcionales y las reglas de negocio definidas en:

```text
docs/requirements.md
docs/business-rules.md
```

La solución prioriza:

- Simplicidad.
- Mantenibilidad.
- Seguridad.
- Separación de responsabilidades.
- Facilidad de pruebas.
- Facilidad de comprensión y explicación.
- Desarrollo rápido dentro del plazo del reto.

La aplicación será un **monolito web** desarrollado con Java y Spring Boot.

---

# 2. Decisión arquitectónica

Se utilizará una arquitectura monolítica por capas.

La aplicación estará compuesta por un único proyecto Spring Boot que contiene:

- Backend.
- Lógica de negocio.
- Acceso a datos.
- Seguridad.
- Vistas HTML.
- Recursos estáticos.

No se utilizará una arquitectura de microservicios.

## 2.1 Justificación

El reto requiere una aplicación funcional en un tiempo limitado y no requiere distribución de servicios.

Un monolito permite:

- Reducir complejidad.
- Evitar comunicación entre servicios.
- Simplificar autenticación.
- Simplificar despliegue.
- Mantener una única base de datos.
- Facilitar las pruebas.
- Facilitar la explicación de la solución.

La arquitectura debe ser suficientemente estructurada para mantener separación de responsabilidades sin introducir patrones innecesarios.

---

# 3. Stack tecnológico

| Tecnología | Uso |
|---|---|
| Java 17 | Lenguaje |
| Spring Boot | Framework principal |
| Spring MVC | Controladores web |
| Spring Security | Autenticación y autorización |
| Spring Data JPA | Persistencia |
| Hibernate | Implementación JPA |
| MySQL | Base de datos |
| Thymeleaf | Vistas HTML |
| Maven | Gestión de dependencias y construcción |
| Bean Validation | Validaciones |
| JUnit | Pruebas |
| Mockito | Pruebas unitarias |
| Git | Control de versiones |

---

# 4. Arquitectura por capas

La aplicación seguirá la siguiente estructura conceptual:

```text
┌─────────────────────────────────────────────┐
│                  Navegador                  │
│              HTML / CSS / JS                │
└──────────────────────┬──────────────────────┘
                       │ HTTP
                       ▼
┌─────────────────────────────────────────────┐
│                Controller                   │
│        Recepción de solicitudes HTTP        │
└──────────────────────┬──────────────────────┘
                       │
                       ▼
┌─────────────────────────────────────────────┐
│                  Service                    │
│           Lógica de negocio                 │
└──────────────────────┬──────────────────────┘
                       │
                       ▼
┌─────────────────────────────────────────────┐
│                Repository                    │
│             Acceso a datos                  │
└──────────────────────┬──────────────────────┘
                       │
                       ▼
┌─────────────────────────────────────────────┐
│                   MySQL                     │
│              Base de datos                  │
└─────────────────────────────────────────────┘
```

Existen componentes adicionales que atraviesan estas capas:

```text
Security
Validation
Exception Handling
File Storage
Audit
```

---

# 5. Estructura del proyecto

La estructura inicial será:

```text
contact-manager/
│
├── AGENTS.md
│
├── docs/
│   ├── requirements.md
│   ├── business-rules.md
│   ├── architecture.md
│   ├── database.md
│   └── testing-strategy.md
│
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── com/
│   │   │       └── gestorcontactos/
│   │   │           │
│   │   │           ├── config/
│   │   │           ├── controller/
│   │   │           ├── dto/
│   │   │           ├── entity/
│   │   │           ├── exception/
│   │   │           ├── repository/
│   │   │           ├── security/
│   │   │           ├── service/
│   │   │           └── validation/
│   │   │
│   │   └── resources/
│   │       ├── templates/
│   │       │   ├── login.html
│   │       │   ├── dashboard.html
│   │       │   ├── contacts/
│   │       │   └── users/
│   │       │
│   │       ├── static/
│   │       │   ├── css/
│   │       │   └── js/
│   │       │
│   │       └── application.yml
│   │
│   └── test/
│
├── uploads/
│
├── .env.example
├── .gitignore
├── pom.xml
└── README.md
```

La estructura puede evolucionar durante la implementación si existe una razón técnica justificada, pero no debe introducirse complejidad innecesaria.

---

# 6. Responsabilidad de cada paquete

## 6.1 `config`

Contendrá configuraciones generales de la aplicación.

Ejemplos:

```text
SecurityConfig
WebConfig
```

No debe contener lógica de negocio.

---

# 7. `controller`

Será responsable de:

- Recibir solicitudes HTTP.
- Obtener parámetros del formulario.
- Validar DTOs mediante Bean Validation.
- Invocar servicios.
- Preparar datos para las vistas.
- Redirigir o mostrar resultados.

Los controllers no deben contener lógica de negocio compleja.

Ejemplo conceptual:

```text
ContactController
    ↓
ContactService
    ↓
ContactRepository
```

---

# 8. `dto`

Los DTO se utilizarán para transportar información entre la interfaz y la capa de aplicación.

No se deben utilizar directamente las entidades JPA como modelo de entrada de formularios.

Ejemplos:

```text
ContactRequest
ContactResponse
UserCreateRequest
UserUpdateRequest
PasswordResetRequest
```

Los DTO permiten:

- Controlar qué campos puede enviar el usuario.
- Evitar exposición directa de entidades.
- Aplicar validaciones específicas.
- Evitar que campos de auditoría sean manipulados desde formularios.

---

# 9. `entity`

Contendrá las entidades persistentes utilizadas por JPA.

Entidades principales:

```text
User
Contact
ContactPhone
ContactEmail
```

Posiblemente se utilizarán entidades adicionales si el modelo de base de datos lo requiere.

Las entidades representan datos persistidos y relaciones de base de datos.

No deben contener lógica relacionada con HTTP o presentación.

---

# 10. `repository`

Contendrá las interfaces Spring Data JPA utilizadas para acceder a la base de datos.

Ejemplos:

```text
UserRepository
ContactRepository
ContactPhoneRepository
ContactEmailRepository
```

Los repositories serán responsables del acceso a datos.

La lógica de negocio no debe implementarse directamente en los repositories.

---

# 11. `service`

Contendrá la lógica de aplicación y las reglas necesarias para ejecutar las operaciones.

Ejemplos:

```text
AuthService
ContactService
UserService
FileStorageService
```

Los servicios serán responsables de coordinar:

- Validaciones de negocio.
- Repositories.
- Seguridad.
- Auditoría.
- Persistencia.
- Manejo de archivos.

Ejemplo:

```text
ContactController
        │
        ▼
ContactService
        │
        ├── validar reglas
        ├── verificar duplicados
        ├── verificar permisos
        ├── actualizar entidades
        └── guardar mediante Repository
```

---

# 12. `security`

Contendrá los componentes relacionados con Spring Security.

Responsabilidades:

- Autenticación.
- Autorización.
- Gestión de sesión.
- Carga de usuarios.
- Password encoding.
- Restricción de rutas.
- Manejo de acceso denegado.

Componentes previstos:

```text
SecurityConfig
CustomUserDetailsService
CustomUserDetails
```

No se utilizará JWT.

---

# 13. `validation`

Contendrá validaciones específicas del dominio que no puedan expresarse únicamente mediante anotaciones estándar.

Ejemplos:

```text
DocumentValidator
PhoneValidator
ContactValidator
```

Las validaciones deben estar alineadas con `business-rules.md`.

Ejemplos de reglas:

- DNI de 8 dígitos.
- CE de 11 dígitos.
- RUC de 11 dígitos.
- Celular de 9 dígitos comenzando en 9.
- Nombres únicamente con letras y espacios.
- Al menos un teléfono.
- Al menos un correo.
- Documento único.

---

# 14. `exception`

Contendrá excepciones y manejo centralizado de errores.

Ejemplos:

```text
BusinessException
ResourceNotFoundException
DuplicateResourceException
UnauthorizedOperationException
```

Podrá utilizarse un handler global para controlar errores de forma consistente.

Ejemplo conceptual:

```text
@ControllerAdvice
GlobalExceptionHandler
```

El sistema no debe mostrar stack traces al usuario.

---

# 15. Modelo de dominio

El centro del sistema será la entidad `Contact`.

Un contacto representa a una única persona o empresa.

```text
                    ┌──────────────────┐
                    │     Contact      │
                    ├──────────────────┤
                    │ id               │
                    │ personType       │
                    │ names            │
                    │ surnames         │
                    │ legalName        │
                    │ documentType     │
                    │ documentNumber   │
                    │ address          │
                    │ status           │
                    │ createdAt        │
                    │ createdBy        │
                    │ updatedAt        │
                    │ updatedBy        │
                    │ deletedAt        │
                    │ deletedBy        │
                    └────────┬─────────┘
                             │
                ┌────────────┴────────────┐
                │                         │
                ▼                         ▼
       ┌────────────────┐       ┌────────────────┐
       │  ContactPhone  │       │  ContactEmail  │
       └────────────────┘       └────────────────┘
```

La relación comercial será independiente de la identidad del contacto.

Un contacto puede representar:

```text
CLIENTE
PROVEEDOR
CLIENTE + PROVEEDOR
```

---

# 16. Tipo de persona

Se utilizará un enum para representar el tipo de persona:

```text
NATURAL
JURIDICA
```

La aplicación deberá aplicar reglas diferentes según este valor.

## NATURAL

Utiliza:

```text
names
surnames
documentType = DNI | CARNET_EXTRANJERIA
documentNumber
```

## JURIDICA

Utiliza:

```text
legalName
documentType = RUC
documentNumber
```

No se debe permitir que una persona jurídica tenga DNI o CE.

No se debe permitir que una persona natural tenga RUC.

---

# 17. Tipo comercial

Los tipos comerciales serán:

```text
CLIENTE
PROVEEDOR
```

No deben utilizarse como tipos de persona.

La diferencia conceptual es:

```text
Tipo de persona
    NATURAL / JURIDICA

Tipo comercial
    CLIENTE / PROVEEDOR
```

Esto permite que:

```text
Persona natural
    └── CLIENTE + PROVEEDOR

Persona jurídica
    └── CLIENTE

Persona natural
    └── PROVEEDOR
```

---

# 18. Modelado de la relación comercial

La relación comercial no debe duplicar el contacto.

Se utilizará una relación entre el contacto y sus funciones comerciales.

Conceptualmente:

```text
Contact
   │
   ├── CLIENTE
   │
   └── PROVEEDOR
```

En base de datos se recomienda una tabla intermedia:

```text
contact_commercial_type
```

con una estructura conceptual similar a:

```text
contact_id
commercial_type
```

Esto permite que un mismo contacto tenga ambas relaciones.

Ejemplo:

```text
Contact
id = 10
nombre = Juan Pérez

contact_commercial_type

10 | CLIENTE
10 | PROVEEDOR
```

No se debe crear otro `Contact` para representar al mismo Juan Pérez.

---

# 19. Teléfonos

La relación será:

```text
Contact 1 ─────────── N ContactPhone
```

Un contacto debe tener uno o más teléfonos.

Cada teléfono tendrá información suficiente para diferenciar:

```text
CELULAR
FIJO
```

Los números no deben duplicarse dentro del mismo contacto.

---

# 20. Correos

La relación será:

```text
Contact 1 ─────────── N ContactEmail
```

Un contacto debe tener uno o más correos.

Los correos deben almacenarse normalizados en minúsculas.

No se deben permitir correos duplicados dentro del mismo contacto.

---

# 21. Usuarios y roles

La relación conceptual será:

```text
User
 │
 └── Role
```

Cada usuario tendrá exactamente un rol.

Roles:

```text
ADMINISTRADOR
VENDEDOR
COMPRAS
ALMACENERO
```

El rol se utilizará para autorización.

---

# 22. Estados

## Contacto

```text
ACTIVO
INACTIVO
```

## Usuario

```text
ACTIVO
INACTIVO
```

El contacto eliminado no debe representarse simplemente como:

```text
status = ELIMINADO
```

La eliminación se manejará mediante campos de eliminación lógica:

```text
deletedAt
deletedBy
```

Esto permite diferenciar:

```text
ACTIVO
INACTIVO
ELIMINADO
```

sin mezclar estados funcionales con el estado de eliminación.

---

# 23. Eliminación lógica

Los contactos no serán eliminados físicamente.

Cuando un administrador elimine un contacto:

```text
deletedAt = fecha actual
deletedBy = usuario actual
```

Las consultas normales deben filtrar registros eliminados.

Conceptualmente:

```text
WHERE deleted_at IS NULL
```

La recuperación eliminará la marca de eliminación:

```text
deletedAt = null
deletedBy = null
```

y establecerá:

```text
status = INACTIVO
```

La recuperación debe realizarse exclusivamente mediante el servicio autorizado.

---

# 24. Auditoría

La auditoría de contactos utilizará los siguientes campos:

```text
createdAt
createdBy

updatedAt
updatedBy

deletedAt
deletedBy
```

El usuario debe obtenerse de la sesión autenticada.

Nunca se debe confiar en un `createdBy` o `deletedBy` enviado por el navegador.

Conceptualmente:

```text
Usuario autenticado
        │
        ▼
Spring Security
        │
        ▼
ContactService
        │
        ▼
Audit fields
```

---

# 25. Seguridad

La aplicación utilizará Spring Security.

El flujo será:

```text
Browser
   │
   ▼
Login
   │
   ▼
Spring Security
   │
   ├── verificar usuario
   ├── verificar contraseña
   └── crear sesión
          │
          ▼
   Aplicación protegida
```

Las contraseñas se almacenarán mediante un algoritmo de hashing seguro, utilizando el mecanismo recomendado por Spring Security.

No se almacenarán contraseñas en texto plano.

---

# 26. Autenticación basada en sesión

La aplicación utilizará sesión HTTP.

No se utilizará JWT.

La sesión permitirá:

- Mantener autenticación al navegar.
- Mantener autenticación al recargar la página.
- Cumplir el requisito de persistencia definido para el reto.
- Cerrar sesión explícitamente.

El logout debe invalidar la sesión.

Cuando una cuenta se desactive o cambie de rol, sus sesiones activas deben expirar. Para ello se utilizará el
`SessionRegistry` en memoria de Spring Security; no se almacenarán sesiones en Redis ni en otro servicio externo.

## 26.1 Administrador inicial

En una base de datos sin usuarios, el primer administrador se puede crear al iniciar la aplicación mediante las
variables de entorno `INITIAL_ADMIN_USERNAME` e `INITIAL_ADMIN_PASSWORD`. La creación solo se ejecuta si la tabla
`users` está vacía. La contraseña se codifica con BCrypt y nunca se registra en logs. Si no hay usuarios ni valores
configurados, la aplicación falla al iniciar con una instrucción para configurar estas variables.

Estas variables deben configurarse localmente o en el entorno de ejecución y nunca guardarse con valores reales en
archivos versionados.

---

# 27. Autorización

La autorización tendrá dos niveles.

## 27.1 Nivel de interfaz

La interfaz mostrará únicamente las opciones correspondientes al rol.

Ejemplo:

```text
ADMINISTRADOR
    Usuarios
    Contactos
    Activar
    Inactivar
    Eliminar
```

El vendedor no verá las opciones administrativas.

## 27.2 Nivel backend

La autorización real siempre será aplicada en el servidor.

Por ejemplo:

```text
POST /contacts/10/delete
```

no debe ser suficiente para eliminar un contacto.

El backend debe verificar:

```text
usuario autenticado
        +
rol ADMINISTRADOR
        +
contacto válido
        +
operación permitida
```

Ocultar un botón nunca se considerará una medida de seguridad suficiente.

---

# 28. Protección CSRF

Spring Security mantendrá habilitada la protección CSRF.

Los formularios Thymeleaf deberán incluir el token correspondiente cuando sea necesario.

No se deshabilitará CSRF para evitar errores durante el desarrollo.

---

# 29. Flujo de creación de contacto

El flujo general será:

```text
Usuario
   │
   ▼
Formulario
   │
   ▼
ContactRequest
   │
   ▼
Validación
   │
   ▼
ContactService
   │
   ├── validar permisos
   ├── validar reglas
   ├── verificar documento
   ├── verificar duplicado
   ├── crear Contact
   ├── crear teléfonos
   ├── crear correos
   ├── crear relación comercial
   └── registrar auditoría
          │
          ▼
   ContactRepository
          │
          ▼
        MySQL
```

---

# 30. Flujo de duplicados

Antes de crear un contacto:

```text
Formulario
    │
    ▼
Validación
    │
    ▼
Buscar documento
    │
    ├── No existe
    │      │
    │      ▼
    │   Crear contacto
    │
    └── Existe
           │
           ├── ACTIVO
           │      │
           │      ▼
           │   Usar contacto existente
           │
           ├── INACTIVO
           │      │
           │      ▼
           │   No crear duplicado
           │
           └── ELIMINADO
                  │
                  ▼
             No crear duplicado
```

Si el contacto existente puede recibir una nueva relación comercial, se utilizará el mismo registro.

---

# 31. Flujo de edición

La edición seguirá el siguiente proceso:

```text
Usuario
   │
   ▼
Formulario de edición
   │
   ▼
ContactRequest
   │
   ▼
ContactService
   │
   ├── verificar autenticación
   ├── verificar rol
   ├── verificar contacto
   ├── proteger identidad
   ├── validar dirección
   ├── validar teléfonos
   ├── validar emails
   ├── procesar fotografía
   └── actualizar auditoría
          │
          ▼
       MySQL
```

Los campos de identidad no deben actualizarse.

Aunque un usuario manipule el HTML del formulario para enviar un nuevo documento, el backend debe rechazarlo o ignorarlo.

---

# 32. Flujo de eliminación

```text
Administrador
      │
      ▼
Eliminar contacto
      │
      ▼
ContactService
      │
      ├── verificar rol
      ├── verificar existencia
      ├── registrar deletedBy
      ├── registrar deletedAt
      └── marcar eliminación lógica
              │
              ▼
             MySQL
```

No se debe realizar:

```text
DELETE FROM contact
```

para una eliminación normal del contacto.

---

# 33. Flujo de recuperación

```text
Administrador
      │
      ▼
Recuperar contacto
      │
      ▼
ContactService
      │
      ├── verificar rol
      ├── localizar eliminado
      ├── limpiar deletedAt
      ├── limpiar deletedBy
      └── establecer INACTIVO
              │
              ▼
             MySQL
```

---

# 34. Gestión de fotografías

El flujo será:

```text
Formulario
    │
    ▼
MultipartFile
    │
    ▼
FileStorageService
    │
    ├── validar tamaño
    ├── validar MIME/content
    ├── validar imagen
    ├── validar dimensiones
    ├── generar nombre seguro
    └── guardar archivo
           │
           ▼
        uploads/
```

La base de datos almacenará únicamente la referencia al archivo.

No se guardará el contenido binario de la fotografía en la base de datos salvo que posteriormente exista una decisión técnica explícita para ello.

---

# 35. Seguridad de fotografías

No se confiará únicamente en:

```text
filename
extension
Content-Type
```

El servidor deberá realizar validaciones adicionales.

El nombre final será generado por el servidor.

Ejemplo conceptual:

```text
8f2c1b5a-9f42-4c3b-ae12-image.jpg
```

El nombre original del usuario no debe utilizarse directamente como nombre de almacenamiento.

---

# 36. Configuración

La configuración sensible se realizará mediante variables de entorno.

Ejemplos:

```text
DB_URL
DB_USERNAME
DB_PASSWORD
```

La aplicación no debe contener credenciales reales dentro del código.

Se proporcionará:

```text
.env.example
```

como referencia para configurar el entorno.

---

# 37. Manejo de errores

Se utilizará un mecanismo centralizado para manejar errores.

Ejemplo:

```text
@ControllerAdvice
GlobalExceptionHandler
```

Tipos de error esperados:

```text
404 - recurso no encontrado
400 - datos inválidos
403 - acceso denegado
409 - conflicto / duplicado
500 - error interno
```

Los mensajes destinados al usuario deben ser comprensibles.

No se deben exponer detalles internos.

---

# 38. Transacciones

Las operaciones que modifiquen varias entidades relacionadas deben ejecutarse dentro de una transacción.

Ejemplo:

Crear un contacto puede involucrar:

```text
Contact
ContactPhone
ContactEmail
CommercialType
Audit
```

Si una parte crítica falla, la operación debe poder revertirse para evitar información parcialmente guardada.

Se utilizará:

```text
@Transactional
```

en los servicios donde corresponda.

---

# 39. Consistencia de datos

La consistencia debe protegerse tanto en la aplicación como en la base de datos.

La aplicación validará las reglas de negocio.

La base de datos proporcionará restricciones de integridad.

Ejemplos:

- PK.
- FK.
- UNIQUE.
- NOT NULL.
- Índices.

La aplicación no debe depender únicamente de validaciones Java para garantizar unicidad.

---

# 40. Control de concurrencia

Cuando exista riesgo de que dos solicitudes intenten registrar simultáneamente la misma identidad, la base de datos debe contar con restricciones de unicidad apropiadas.

El servicio debe manejar correctamente el conflicto generado por la base de datos.

Esto evita depender únicamente de:

```text
SELECT → no existe → INSERT
```

porque dos solicitudes concurrentes podrían obtener el mismo resultado antes del `INSERT`.

---

# 41. Consultas y filtrado

Las consultas de contactos deben respetar el contexto del usuario.

Ejemplo conceptual:

```text
ADMINISTRADOR
    → activos + inactivos

VENDEDOR
    → clientes activos
    → inactivos para validación de duplicados

COMPRAS
    → proveedores activos
    → inactivos para validación de duplicados

ALMACENERO
    → clientes activos
    → proveedores activos
```

Los contactos eliminados no deben aparecer en las consultas normales.

---

# 42. Separación entre entidad y presentación

Las entidades JPA no deben utilizarse directamente como objetos de formulario.

El flujo recomendado será:

```text
HTML Form
   ↓
Request DTO
   ↓
Service
   ↓
Entity
   ↓
Repository
   ↓
Database
```

Para devolver información:

```text
Database
   ↓
Entity
   ↓
Service
   ↓
Response DTO
   ↓
Controller
   ↓
View
```

Esto reduce el riesgo de exponer campos internos.

---

# 43. Thymeleaf

Thymeleaf será utilizado para generar las vistas HTML desde el servidor.

Ventajas para este proyecto:

- Mantiene frontend y backend dentro del mismo proyecto.
- Evita una aplicación Angular independiente.
- Simplifica autenticación basada en sesión.
- Reduce configuración de CORS.
- Reduce complejidad del despliegue.
- Es suficiente para el alcance del reto.

Se podrán utilizar JavaScript y CSS para mejorar la experiencia de usuario sin convertir la aplicación en una SPA.

---

# 44. Seguridad de las vistas

Las vistas deben:

- Mostrar únicamente opciones permitidas.
- Escapar correctamente valores provenientes de usuarios.
- Evitar insertar HTML no confiable.
- Utilizar protección CSRF.
- No mostrar información restringida.

La seguridad de la interfaz será complementaria a la seguridad del backend.

---

# 45. Pruebas

La arquitectura debe permitir probar las reglas críticas independientemente de la interfaz.

Se contemplan:

## Pruebas unitarias

Para:

- Validadores.
- Servicios.
- Reglas de duplicados.
- Estados.
- Permisos.

## Pruebas de integración

Para:

- Repositories.
- Base de datos.
- Operaciones de contacto.
- Autenticación.
- Autorización.

## Pruebas de seguridad

Para verificar que:

- usuarios no autenticados no accedan;
- roles incorrectos reciban acceso denegado;
- usuarios no puedan ejecutar operaciones administrativas.

---

# 46. Principales componentes

El diseño inicial contempla:

```text
config/
    SecurityConfig
    WebConfig

controller/
    AuthController
    DashboardController
    ContactController
    UserController

dto/
    ContactRequest
    ContactResponse
    UserCreateRequest
    UserUpdateRequest
    PasswordResetRequest

entity/
    User
    Contact
    ContactPhone
    ContactEmail

repository/
    UserRepository
    ContactRepository
    ContactPhoneRepository
    ContactEmailRepository

security/
    CustomUserDetails
    CustomUserDetailsService

service/
    AuthService
    ContactService
    UserService
    FileStorageService

validation/
    DocumentValidator
    PhoneValidator
    ContactValidator

exception/
    BusinessException
    ResourceNotFoundException
    DuplicateResourceException
    GlobalExceptionHandler
```

Los nombres podrán ajustarse durante la implementación si la solución final requiere una organización mejor, pero deben mantenerse las responsabilidades separadas.

---

# 47. Principio de simplicidad

No se deben introducir patrones arquitectónicos únicamente para hacer que el proyecto parezca más complejo.

No se utilizarán inicialmente:

- Microservicios.
- CQRS.
- Event Sourcing.
- Kafka.
- Redis.
- JWT.
- API Gateway.
- Service Discovery.
- Arquitecturas distribuidas.
- Múltiples bases de datos.

Estas tecnologías no son necesarias para cumplir los requisitos actuales.

Si durante el desarrollo aparece una necesidad real que justifique una modificación arquitectónica, debe documentarse antes de introducirla.

---

# 48. Seguridad como responsabilidad transversal

La seguridad no estará limitada a `SecurityConfig`.

Debe existir en todas las capas relevantes:

```text
Controller
    ↓
validación de entrada

Service
    ↓
reglas de negocio + autorización

Repository
    ↓
acceso controlado a datos

Database
    ↓
integridad y restricciones
```

Ejemplo:

```text
Ocultar botón "Eliminar"
```

no es suficiente.

Debe existir además:

```text
Endpoint protegido
        +
Service verifica ADMINISTRADOR
        +
Base de datos mantiene integridad
```

---

# 49. Flujo general de una solicitud

```text
┌──────────────┐
│   Browser    │
└──────┬───────┘
       │ HTTP
       ▼
┌──────────────┐
│ Spring       │
│ Security     │
└──────┬───────┘
       │ autenticado/autorizado
       ▼
┌──────────────┐
│ Controller   │
└──────┬───────┘
       │ DTO
       ▼
┌──────────────┐
│ Service      │
│              │
│ reglas       │
│ validación   │
│ auditoría    │
└──────┬───────┘
       │ Entity
       ▼
┌──────────────┐
│ Repository   │
└──────┬───────┘
       │
       ▼
┌──────────────┐
│    MySQL     │
└──────────────┘
```

La respuesta seguirá el flujo inverso:

```text
MySQL
  ↓
Repository
  ↓
Service
  ↓
DTO
  ↓
Controller
  ↓
Thymeleaf
  ↓
Browser
```

---

# 50. Decisiones arquitectónicas importantes

## 50.1 Monolito

Se eligió un monolito porque el alcance no requiere distribución.

## 50.2 Thymeleaf

Se eligió Thymeleaf para mantener frontend y backend dentro de una única aplicación.

## 50.3 Spring Security + sesión

Se eligió autenticación basada en sesión porque el sistema es una aplicación web tradicional y el requisito exige persistencia de sesión.

## 50.4 JPA

Se eligió Spring Data JPA para reducir código repetitivo de persistencia y mantener un modelo de datos orientado a entidades.

## 50.5 DTO

Se utilizarán DTO para controlar la entrada y salida de información.

## 50.6 Eliminación lógica

Se utilizará soft delete para conservar información de auditoría y permitir recuperación.

## 50.7 Almacenamiento de fotografías fuera de la BD

Se almacenarán archivos en el sistema de archivos y únicamente su referencia en la base de datos, evitando aumentar innecesariamente el tamaño de las tablas.

## 50.8 Roles

La autorización se implementará mediante roles de Spring Security y verificaciones adicionales de negocio cuando sea necesario.

---

# 51. Principio de trazabilidad

Cada requisito importante debe poder relacionarse con:

```text
Requisito
    ↓
Regla de negocio
    ↓
Componente
    ↓
Implementación
    ↓
Prueba
```

Ejemplo:

```text
RF-21 Unicidad
       ↓
business-rules.md
       ↓
ContactService
       ↓
ContactRepository + restricción DB
       ↓
Prueba de duplicados
```

Esto permite demostrar durante la evaluación que las decisiones técnicas se derivan de los requisitos y no fueron implementadas arbitrariamente.

---

# 52. Evolución de la arquitectura

La arquitectura definida en este documento representa la estructura inicial del proyecto.

Durante el desarrollo podrán realizarse cambios cuando exista una necesidad técnica real.

Todo cambio significativo deberá:

1. Identificar el problema.
2. Evaluar alternativas.
3. Seleccionar la solución.
4. Actualizar la documentación correspondiente.
5. Implementar el cambio.
6. Agregar o actualizar pruebas.

La documentación debe mantenerse sincronizada con la implementación.

---

# 53. Fuente de verdad

La arquitectura debe respetar:

```text
docs/requirements.md
docs/business-rules.md
```

El orden de referencia será:

```text
Requisitos
    ↓
Reglas de negocio
    ↓
Arquitectura
    ↓
Implementación
    ↓
Pruebas
```

El código no debe modificar silenciosamente los requisitos ni las reglas de negocio.

Si durante la implementación aparece una contradicción o ambigüedad que afecte al comportamiento funcional, debe resolverse antes de implementar la decisión.

---

# 54. Resultado esperado

Al finalizar la implementación se espera obtener una aplicación monolítica que permita:

- Autenticar usuarios.
- Aplicar permisos por rol.
- Administrar clientes.
- Administrar proveedores.
- Mantener un único contacto para clientes/proveedores simultáneos.
- Gestionar personas naturales y jurídicas.
- Validar documentos.
- Gestionar teléfonos y correos.
- Gestionar fotografías.
- Activar e inactivar contactos.
- Eliminar lógicamente contactos.
- Recuperar contactos.
- Administrar usuarios.
- Mantener auditoría.
- Mantener sesión persistente.
- Proteger datos personales.
- Ejecutar pruebas automatizadas.
- Mantener un código organizado y explicable.

La solución debe ser suficientemente robusta para la evaluación, pero evitar complejidad que no aporte valor al alcance del proyecto.
