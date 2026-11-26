# AGENTS.md

## 1. Propósito

Este archivo contiene las instrucciones que deben seguir los agentes de IA durante el desarrollo del proyecto **Gestor de Contactos**.

El agente debe utilizar este documento junto con la documentación ubicada en `docs/`.

La documentación funcional y técnica del proyecto es la fuente de verdad.

Antes de implementar una funcionalidad, el agente debe revisar los documentos relevantes.

---

# 2. Fuente de verdad

Los siguientes archivos tienen prioridad para tomar decisiones:

```text
docs/requirements.md
docs/business-rules.md
docs/architecture.md
docs/database.md
docs/testing-strategy.md
```

Cada documento tiene una responsabilidad específica:

| Documento | Responsabilidad |
|---|---|
| `requirements.md` | Requisitos funcionales y no funcionales |
| `business-rules.md` | Reglas de negocio |
| `architecture.md` | Arquitectura y decisiones técnicas |
| `database.md` | Modelo y reglas de persistencia |
| `testing-strategy.md` | Estrategia de pruebas |

Si una implementación propuesta contradice estos documentos, el agente debe detenerse y señalar la contradicción antes de modificar la arquitectura o las reglas.

---

# 3. Regla principal

El agente debe:

> Implementar lo solicitado respetando la arquitectura y las reglas existentes, evitando introducir complejidad que no sea necesaria.

El agente no debe cambiar una decisión previamente definida únicamente porque exista otra alternativa técnicamente posible.

Si considera que una decisión debe cambiarse, debe:

1. explicar el problema;
2. explicar la alternativa;
3. indicar las consecuencias;
4. esperar aprobación antes de cambiar la decisión.

---

# 4. Objetivo del proyecto

El proyecto es una aplicación monolítica de gestión de contactos.

Debe permitir:

- autenticación de usuarios;
- gestión de contactos;
- gestión de clientes;
- gestión de proveedores;
- gestión de usuarios por parte del administrador;
- control de permisos;
- múltiples teléfonos;
- múltiples correos;
- fotografía opcional;
- auditoría;
- activación e inactivación;
- eliminación lógica;
- recuperación.

---

# 5. Stack tecnológico obligatorio

La implementación debe utilizar:

```text
Java 17
Spring Boot
Spring Security
Spring Data JPA / Hibernate
MySQL
Thymeleaf
Maven
JUnit
Mockito
Git
```

La aplicación será un monolito.

No se deben introducir tecnologías adicionales sin una justificación clara.

---

# 6. Tecnologías y patrones que NO deben introducirse innecesariamente

No utilizar:

```text
Microservicios
JWT
API Gateway
Kafka
Redis
CQRS
Event Sourcing
Service Discovery
Eureka
Docker como requisito arquitectónico
Múltiples bases de datos
Stored Procedures
```

Estas tecnologías pueden existir como alternativas válidas en otros proyectos, pero no forman parte de la arquitectura definida para este reto.

Si aparece una necesidad real que justifique alguna de ellas, primero debe explicarse y solicitar aprobación.

---

# 7. Arquitectura

La arquitectura definida es:

```text
Browser
   ↓
Controller
   ↓
Service
   ↓
Repository
   ↓
MySQL
```

Las responsabilidades deben mantenerse separadas.

## Controller

Responsable de:

- recibir solicitudes;
- validar entrada básica;
- llamar a servicios;
- devolver vistas o respuestas correspondientes.

No debe contener reglas de negocio complejas.

## Service

Responsable de:

- reglas de negocio;
- autorización funcional;
- coordinación de operaciones;
- transacciones;
- modificación de entidades.

## Repository

Responsable de:

- acceso a datos;
- consultas JPA;
- persistencia.

No debe contener reglas de negocio.

## Entity

Representa el modelo persistente.

No debe utilizarse directamente como objeto de entrada de formularios cuando exista riesgo de exponer campos que no deben modificarse.

## DTO

Debe utilizarse para controlar los datos que entran y salen de la aplicación.

No exponer entidades JPA directamente cuando un DTO sea necesario para proteger información o controlar el contrato.

---

# 8. Estructura de paquetes

Mantener una estructura similar a:

```text
src/main/java/
└── com/.../
    ├── config/
    ├── controller/
    ├── dto/
    ├── entity/
    ├── exception/
    ├── repository/
    ├── security/
    ├── service/
    └── validation/
```

No crear paquetes adicionales sin necesidad.

Si una clase pertenece claramente a una de estas responsabilidades, debe ubicarse en el paquete correspondiente.

---

# 9. Reglas de dominio

## Tipos de persona

Solo existen:

```text
NATURAL
JURIDICA
```

## Tipos comerciales

Solo existen:

```text
CLIENTE
PROVEEDOR
```

Un contacto puede tener:

```text
CLIENTE
```

o:

```text
PROVEEDOR
```

o ambos:

```text
CLIENTE + PROVEEDOR
```

No se deben crear dos contactos para representar dos relaciones comerciales de la misma identidad.

---

# 10. Identidad de contacto

La identidad es inmutable después de la creación.

## Natural

No se pueden modificar:

```text
Nombres
Apellidos
Tipo de documento
Número de documento
```

## Jurídica

No se pueden modificar:

```text
Razón social
Tipo de documento
RUC
```

Si una pantalla o endpoint permite modificar estos datos, debe corregirse.

---

# 11. Validación de documentos

## DNI

Exactamente:

```text
8 dígitos
```

## Carnet de Extranjería

Exactamente:

```text
11 dígitos
```

## RUC

Exactamente:

```text
11 dígitos
```

Los documentos se almacenan como texto.

No convertirlos a tipos numéricos.

---

# 12. Validación de nombres

Los nombres y apellidos de personas naturales deben permitir:

- letras;
- espacios;
- acentos;
- `Ñ` y `ñ`.

No permitir:

- números;
- caracteres especiales no permitidos.

Se debe normalizar:

- espacios iniciales;
- espacios finales;
- múltiples espacios consecutivos.

---

# 13. Razón social

La razón social no debe utilizar la validación de nombres.

Puede contener:

- letras;
- números;
- espacios;
- puntuación empresarial común.

Ejemplos:

```text
Comercial Los Andes S.A.C.
Empresa 123 S.A.C.
Minería del Sur S.A.
```

---

# 14. Dirección

La dirección es obligatoria.

Debe tener entre:

```text
5 y 200 caracteres
```

Debe almacenarse como texto y no como HTML.

No permitir que el usuario introduzca contenido que posteriormente sea interpretado como HTML o JavaScript.

---

# 15. Teléfonos

Debe existir al menos un teléfono por contacto.

Tipos:

```text
CELULAR
FIJO
```

## Celular

Debe:

```text
tener 9 dígitos
comenzar con 9
```

No almacenar `+51` como parte del número nacional.

## Fijo

Debe separar:

```text
area_code
phone_number
```

No inventar reglas de longitud provincial que no estén definidas en la documentación.

---

# 16. Correos

Debe existir al menos un correo.

Los correos deben:

- tener formato válido;
- normalizarse a minúsculas;
- no repetirse dentro del contacto.

---

# 17. Fotografías

La fotografía es opcional.

Permitidos:

```text
JPEG
JPG
PNG
WebP
```

Máximo:

```text
2 MB
```

Máximo:

```text
800x800 px
```

El agente debe validar el contenido real del archivo y no confiar únicamente en la extensión.

Nunca utilizar directamente el nombre del archivo enviado por el usuario.

Generar un nombre único del lado servidor.

Las fotografías deben almacenarse fuera del código fuente.

La ruta de la fotografía se almacena en la base de datos.

---

# 18. Estados

Los estados funcionales son:

```text
ACTIVO
INACTIVO
```

Solo:

```text
ADMINISTRADOR
```

puede activar o inactivar contactos.

No crear:

```text
ELIMINADO
```

como estado.

La eliminación se representa mediante:

```text
deleted_at
deleted_by
```

---

# 19. Eliminación lógica

La eliminación de contactos debe ser lógica.

No utilizar:

```text
DELETE FROM contacts
```

para la eliminación funcional normal.

En su lugar:

```text
deleted_at
deleted_by
```

deben registrar la eliminación.

Los contactos eliminados deben quedar fuera de las consultas normales.

---

# 20. Recuperación

Solo el administrador puede recuperar contactos eliminados.

Después de recuperarlos:

```text
deleted_at = NULL
deleted_by = NULL
status = INACTIVO
```

Nunca activar automáticamente un contacto recuperado.

---

# 21. Auditoría

Las operaciones deben registrar:

```text
created_by
created_at

updated_by
updated_at

deleted_by
deleted_at
```

Los usuarios responsables deben obtenerse desde la sesión autenticada.

Nunca confiar en un `userId` enviado por el navegador para determinar quién realizó una operación.

---

# 22. Usuarios

Roles permitidos:

```text
ADMINISTRADOR
VENDEDOR
COMPRAS
ALMACENERO
```

Los usuarios tienen un estado habilitado/deshabilitado.

Un usuario deshabilitado no puede iniciar sesión.

Los usuarios no deben eliminarse físicamente.

---

# 23. Contraseñas

Nunca almacenar contraseñas en texto plano.

Utilizar el mecanismo seguro de Spring Security para generar y verificar hashes.

No registrar contraseñas en:

```text
logs
console
exceptions
DTOs
responses
```

No crear endpoints que devuelvan contraseñas.

---

# 24. Spring Security

Utilizar autenticación basada en sesión HTTP.

No implementar JWT.

Mantener CSRF habilitado.

Las páginas protegidas deben requerir autenticación.

Las operaciones restringidas deben verificar autorización en backend.

No confiar únicamente en botones ocultos en la interfaz.

---

# 25. Autorización

## ADMINISTRADOR

Puede:

- gestionar clientes;
- gestionar proveedores;
- consultar activos e inactivos;
- activar;
- inactivar;
- eliminar;
- recuperar;
- gestionar usuarios.

No puede:

- deshabilitarse a sí mismo;
- eliminar su propio rol de administrador.

## VENDEDOR

Puede:

- crear clientes;
- consultar clientes;
- editar campos permitidos de clientes.

No puede:

- gestionar proveedores;
- eliminar contactos;
- recuperar contactos;
- activar;
- inactivar;
- administrar usuarios.

## COMPRAS

Puede:

- crear proveedores;
- consultar proveedores;
- editar campos permitidos de proveedores.

No puede:

- gestionar clientes;
- eliminar contactos;
- recuperar contactos;
- activar;
- inactivar;
- administrar usuarios.

## ALMACENERO

Puede:

- consultar clientes activos;
- consultar proveedores activos.

No puede:

- crear;
- editar;
- eliminar;
- recuperar;
- activar;
- inactivar;
- administrar usuarios.

---

# 26. Protección de información

El almacenero solamente debe recibir la información permitida.

Para natural:

```text
Nombres
Apellidos
Dirección
```

Para jurídica:

```text
Razón social
Dirección
```

No debe recibir:

```text
DNI
CE
RUC
Teléfonos
Correos
Fotografía
Auditoría
```

Esta protección debe realizarse en el backend mediante DTOs, consultas o mecanismos equivalentes.

No basta con ocultar información mediante CSS o JavaScript.

---

# 27. Base de datos

Utilizar una única base de datos MySQL.

Tablas principales:

```text
users
contacts
contact_phones
contact_emails
contact_commercial_types
```

Mantener las restricciones de integridad definidas en:

```text
docs/database.md
```

La identidad:

```text
document_type + document_number
```

debe ser única.

---

# 28. JPA

Utilizar Spring Data JPA / Hibernate.

Preferir:

```text
JpaRepository
```

cuando sea suficiente.

No crear consultas SQL manuales innecesariamente.

Utilizar JPQL o consultas nativas únicamente cuando exista una razón clara.

No introducir JDBC/JdbcTemplate si JPA resuelve correctamente el caso.

---

# 29. Transacciones

Las operaciones que modifican múltiples entidades deben utilizar transacciones cuando corresponda.

Ejemplo:

```text
Crear Contact
    +
Crear Phones
    +
Crear Emails
    +
Crear Commercial Types
```

debe ejecutarse como una operación consistente.

Si falla una parte crítica, no debe quedar información parcialmente persistida.

---

# 30. Validaciones

Las validaciones deben existir del lado servidor.

Se pueden utilizar:

```text
Jakarta Bean Validation
```

y validaciones adicionales en servicios cuando las reglas sean de negocio.

No confiar únicamente en:

```text
HTML required
JavaScript
Angular validation
```

para garantizar reglas de negocio.

---

# 31. Manejo de errores

Los errores deben manejarse de manera controlada.

No mostrar al usuario:

```text
stack trace
SQL
password
información interna de infraestructura
```

Los mensajes deben ser claros y útiles.

Los errores técnicos deben registrarse de manera adecuada en logs sin exponer información sensible.

---

# 32. DTOs

Los DTOs deben utilizarse cuando sea necesario:

- controlar datos de entrada;
- proteger campos;
- controlar datos de salida;
- evitar exposición de entidades;
- representar diferentes vistas según el rol.

No utilizar un DTO genérico con todos los campos para todas las operaciones si eso permite modificar información que debería ser inmutable.

---

# 33. Thymeleaf

La interfaz utilizará Thymeleaf.

Los datos deben renderizarse mediante mecanismos seguros.

No insertar directamente contenido proporcionado por el usuario como HTML sin sanitización.

Utilizar correctamente el escape de contenido de Thymeleaf.

---

# 34. Archivos y secretos

No almacenar en Git:

```text
.env
contraseñas
claves privadas
credenciales
archivos reales de usuarios
uploads reales
```

Debe existir:

```text
.env.example
```

con valores de ejemplo.

Las rutas de archivos subidos deben estar fuera del código fuente.

---

# 35. Git

Los commits deben ser pequeños y significativos.

Ejemplos:

```text
feat: configurar proyecto Spring Boot
feat: implementar autenticación
feat: crear entidad Contact
feat: implementar creación de clientes
feat: implementar gestión de proveedores
test: agregar pruebas de validación de documentos
fix: corregir validación de teléfonos
docs: actualizar reglas de negocio
```

No realizar commits gigantes que mezclen funcionalidades no relacionadas.

---

# 36. Uso de IA

Codex es una herramienta de desarrollo, no la fuente de verdad del proyecto.

El agente debe:

1. leer la documentación;
2. analizar la tarea;
3. revisar el código existente;
4. implementar el cambio;
5. ejecutar pruebas;
6. revisar errores;
7. corregir;
8. verificar nuevamente;
9. informar qué fue modificado.

No asumir que una implementación generada automáticamente es correcta solamente porque compila.

---

# 37. No modificar requisitos silenciosamente

El agente no debe:

- cambiar roles;
- cambiar permisos;
- cambiar reglas de documentos;
- cambiar reglas de teléfonos;
- cambiar reglas de eliminación;
- cambiar el modelo de autenticación;
- cambiar la arquitectura;
- introducir nuevos estados;
- cambiar el comportamiento comercial;

sin autorización.

Si encuentra una contradicción entre documentos, debe detenerse y reportarla.

---

# 38. Trabajo incremental

Las funcionalidades deben implementarse de manera incremental.

Orden recomendado:

```text
1. Configuración inicial
2. Base de datos
3. Entidades
4. Repositories
5. Seguridad
6. Usuarios
7. Contactos
8. Clientes
9. Proveedores
10. Teléfonos
11. Correos
12. Fotografías
13. Estados
14. Eliminación y recuperación
15. Auditoría
16. Interfaz
17. Pruebas
18. Seguridad final
19. Documentación
```

No implementar todo el sistema en una única operación si esto dificulta la revisión.

---

# 39. Pruebas obligatorias

Antes de considerar una funcionalidad terminada, ejecutar las pruebas correspondientes.

Como mínimo:

```bash
mvn test
```

Para una modificación importante se recomienda:

```bash
mvn clean test
```

Las pruebas deben cubrir especialmente:

- reglas de negocio;
- autorización;
- autenticación;
- persistencia;
- validaciones;
- transacciones;
- eliminación lógica.

---

# 40. Verificación manual

Las pruebas automatizadas no sustituyen completamente las pruebas manuales.

Las funcionalidades principales deben verificarse mediante la interfaz:

```text
Login
Crear contacto
Editar contacto
Consultar
Activar
Inactivar
Eliminar
Recuperar
Gestionar usuarios
```

También deben verificarse intentos de acceso no autorizado.

---

# 41. Revisión después de cambios

Después de modificar una funcionalidad existente, revisar si el cambio afecta:

```text
Controller
Service
Repository
DTO
Entity
Security
Database
Tests
UI
```

No asumir que una modificación localizada no puede afectar otras capas.

---

# 42. Código generado por IA

Cuando Codex genere código:

- leer el código;
- entender qué hace;
- comprobar que coincide con los requisitos;
- comprobar que no introduce permisos incorrectos;
- comprobar manejo de errores;
- comprobar seguridad;
- comprobar pruebas;
- eliminar código innecesario.

No aceptar código únicamente porque "funciona".

---

# 43. Simplicidad

Preferir:

```text
solución simple y clara
```

sobre:

```text
solución compleja y abstracta
```

No crear abstracciones, interfaces, patrones o capas únicamente para demostrar conocimiento técnico.

Cada componente nuevo debe tener una razón.

---

# 44. Código legible

Preferir:

- nombres descriptivos;
- métodos pequeños;
- responsabilidades claras;
- clases cohesivas;
- validaciones comprensibles;
- excepciones específicas cuando sea necesario.

Evitar:

- métodos gigantes;
- lógica duplicada;
- nombres ambiguos;
- comentarios que expliquen código obvio;
- código muerto;
- clases sin responsabilidad clara.

---

# 45. Nomenclatura

El código utilizará nombres en inglés.

Ejemplos:

```text
Contact
User
ContactPhone
ContactEmail
ContactService
ContactRepository
UserService
```

Los documentos del proyecto estarán en español.

Los comentarios pueden escribirse en español cuando ayuden a explicar una decisión relevante.

---

# 46. No sobreingeniería

El proyecto debe permanecer adecuado al alcance del reto.

No agregar funcionalidades solamente porque sean técnicamente interesantes.

Ejemplos de funcionalidades que no deben agregarse sin requerimiento:

```text
Notificaciones
Chat
Auditoría avanzada de cada campo
Multi-tenancy
Internacionalización completa
API pública
Microservicios
Sistema de eventos
```

---

# 47. Documentación

Si una decisión importante cambia, actualizar la documentación correspondiente.

Por ejemplo:

```text
Cambio de arquitectura -> architecture.md
Cambio de regla -> business-rules.md
Cambio de base de datos -> database.md
Cambio de estrategia de pruebas -> testing-strategy.md
```

No dejar documentación contradictoria con el código.

---

# 48. Antes de terminar una tarea

El agente debe verificar:

```text
[ ] Requisito entendido
[ ] Código existente revisado
[ ] Implementación realizada
[ ] Reglas de negocio respetadas
[ ] Seguridad revisada
[ ] Autorización revisada
[ ] Validaciones implementadas
[ ] Pruebas ejecutadas
[ ] Errores corregidos
[ ] Documentación actualizada si corresponde
[ ] Código innecesario eliminado
```

---

# 49. Formato de respuesta del agente

Al finalizar una tarea, Codex debe informar brevemente:

### Cambios realizados

Indicar los archivos principales modificados.

### Funcionalidad

Explicar qué se implementó.

### Pruebas

Indicar qué pruebas fueron ejecutadas y su resultado.

### Consideraciones

Indicar cualquier decisión relevante, limitación o punto que requiera revisión.

Ejemplo:

```text
Cambios:
- Contact.java
- ContactService.java
- ContactController.java

Implementado:
- creación de contactos naturales;
- validación DNI;
- validación de teléfonos;
- auditoría de creación.

Pruebas:
- mvn test -> OK
- pruebas de DNI -> OK
- pruebas de duplicidad -> OK

Consideraciones:
- No se modificó la identidad después de la creación.
```

---

# 50. Regla final

El agente debe recordar permanentemente:

> El objetivo no es generar la mayor cantidad de código posible. El objetivo es construir una solución correcta, segura, simple, mantenible y alineada con los requisitos definidos.

Antes de introducir una nueva tecnología, patrón o abstracción, debe preguntarse:

```text
¿Es necesario para cumplir el requisito?
¿Está definido en la arquitectura?
¿Hace el sistema más claro?
¿Puedo explicarlo en una entrevista?
¿Tiene una prueba que demuestre su necesidad?
```

Si la respuesta es no, debe preferirse la solución más simple.