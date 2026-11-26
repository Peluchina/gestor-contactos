# Diseño de Base de Datos

## 1. Objetivo

Este documento define el diseño de la base de datos del sistema **Gestor de Contactos**.

La base de datos debe permitir:

- Gestionar usuarios y roles.
- Registrar contactos naturales y jurídicos.
- Asociar contactos con uno o ambos tipos comerciales: `CLIENTE` y/o `PROVEEDOR`.
- Registrar múltiples teléfonos y correos electrónicos por contacto.
- Registrar una dirección y una fotografía opcional.
- Mantener el estado activo/inactivo.
- Implementar eliminación lógica y recuperación.
- Mantener información de auditoría.
- Garantizar la unicidad de la identidad de los contactos.
- Garantizar que no existan teléfonos o correos duplicados dentro de un mismo contacto.

La solución utiliza una única base de datos relacional MySQL.

---

## 2. Motor y configuración

### Motor

- MySQL 8.x o superior.
- Charset: `utf8mb4`.
- Collation recomendada: `utf8mb4_0900_ai_ci`.

El charset `utf8mb4` permite almacenar correctamente:

- caracteres acentuados;
- `Ñ` / `ñ`;
- caracteres Unicode.

### Base de datos

Nombre recomendado:

```text
gestor_contactos
```

La configuración de conexión no debe estar almacenada directamente en el código fuente.

Ejemplo conceptual:

```text
DB_URL
DB_USERNAME
DB_PASSWORD
```

Los valores reales deben configurarse mediante variables de entorno o mecanismos equivalentes.

---

# 3. Modelo relacional

La base de datos estará compuesta principalmente por las siguientes tablas:

```text
users
   |
   | 1
   |
   | N
contacts
   |----------------------|
   |                      |
   | 1                    | N
   |                      |
   v                      v
contact_phones       contact_emails
   |
   |
   |
contacts
   |
   | N
   |
   v
contact_commercial_types
```

La relación comercial se representa mediante una tabla independiente porque un mismo contacto puede ser simultáneamente:

```text
CLIENTE
PROVEEDOR
```

No se debe crear un contacto duplicado para representar cada relación comercial.

---

# 4. Tabla `users`

Almacena los usuarios que pueden acceder al sistema.

## Campos

| Campo | Tipo | Null | Descripción |
|---|---|---:|---|
| `id` | BIGINT | NO | Identificador único |
| `username` | VARCHAR(50) | NO | Nombre de usuario |
| `password_hash` | VARCHAR(255) | NO | Contraseña almacenada mediante hash |
| `role` | VARCHAR(30) | NO | Rol del usuario |
| `enabled` | BOOLEAN | NO | Indica si puede iniciar sesión |
| `created_at` | DATETIME | NO | Fecha y hora de creación |
| `created_by` | BIGINT | SÍ | Usuario que creó el registro |
| `updated_at` | DATETIME | SÍ | Fecha y hora de última modificación |
| `updated_by` | BIGINT | SÍ | Usuario que modificó el registro |

### Restricciones

`username` debe ser único.

El sistema debe tratar el username sin distinguir mayúsculas/minúsculas.

Ejemplo:

```text
Elissa
elissa
ELISSA
```

deben considerarse el mismo usuario.

La contraseña nunca se almacena en texto plano.

### Roles válidos

```text
ADMINISTRADOR
VENDEDOR
COMPRAS
ALMACENERO
```

El rol se almacena como texto y se representa mediante un enum en Java.

---

# 5. Tabla `contacts`

Es la tabla principal del sistema.

Representa tanto personas naturales como personas jurídicas.

## Campos

| Campo | Tipo | Null | Descripción |
|---|---|---:|---|
| `id` | BIGINT | NO | Identificador único |
| `person_type` | VARCHAR(15) | NO | Tipo de persona |
| `first_name` | VARCHAR(100) | SÍ | Nombres |
| `last_name` | VARCHAR(100) | SÍ | Apellidos |
| `document_type` | VARCHAR(25) | NO | Tipo de documento |
| `document_number` | VARCHAR(11) | NO | Número de documento |
| `legal_name` | VARCHAR(200) | SÍ | Razón social |
| `address` | VARCHAR(200) | NO | Dirección |
| `photo_path` | VARCHAR(500) | SÍ | Ruta de la fotografía |
| `status` | VARCHAR(10) | NO | Estado del contacto |
| `created_at` | DATETIME | NO | Fecha/hora de creación |
| `created_by` | BIGINT | NO | Usuario creador |
| `updated_at` | DATETIME | SÍ | Fecha/hora de actualización |
| `updated_by` | BIGINT | SÍ | Usuario modificador |
| `deleted_at` | DATETIME | SÍ | Fecha/hora de eliminación lógica |
| `deleted_by` | BIGINT | SÍ | Usuario que eliminó |

---

# 6. Tipo de persona

El campo:

```text
person_type
```

acepta:

```text
NATURAL
JURIDICA
```

## Persona natural

Debe utilizar:

```text
person_type = NATURAL
```

y almacenar:

```text
first_name
last_name
document_type
document_number
```

Los documentos permitidos son:

```text
DNI
CARNET_EXTRANJERIA
```

## Persona jurídica

Debe utilizar:

```text
person_type = JURIDICA
```

y almacenar:

```text
legal_name
document_type
document_number
```

El documento debe ser:

```text
RUC
```

Los campos que no corresponden al tipo de persona deben permanecer en `NULL`.

La validación condicional de estos campos corresponde principalmente a la capa de aplicación.

---

# 7. Identidad del contacto

La identidad se determina mediante:

```text
document_type + document_number
```

Por lo tanto, debe existir una restricción única:

```text
UNIQUE(document_type, document_number)
```

Esto evita que la misma identidad sea registrada dos veces.

Ejemplo:

```text
DNI + 12345678
```

solo puede pertenecer a un contacto.

La restricción también aplica a contactos eliminados lógicamente.

Por lo tanto:

```text
Contacto A
DNI: 12345678
ELIMINADO
```

no permite crear:

```text
Contacto B
DNI: 12345678
```

Si un contacto existente tiene otro tipo comercial, debe reutilizarse el mismo contacto.

---

# 8. Documentos

## Persona natural

### DNI

Debe contener exactamente:

```text
8 dígitos numéricos
```

Ejemplo:

```text
12345678
```

### Carnet de Extranjería

Debe contener exactamente:

```text
11 dígitos numéricos
```

## Persona jurídica

### RUC

Debe contener exactamente:

```text
11 dígitos numéricos
```

La base de datos almacena el documento como `VARCHAR` y no como número.

Esto evita problemas relacionados con ceros iniciales y permite representar correctamente el valor de identidad.

La validación de longitud y contenido se realiza en la aplicación.

---

# 9. Tabla `contact_phones`

Permite registrar múltiples teléfonos para un contacto.

## Campos

| Campo | Tipo | Null | Descripción |
|---|---|---:|---|
| `id` | BIGINT | NO | Identificador |
| `contact_id` | BIGINT | NO | Contacto asociado |
| `phone_type` | VARCHAR(10) | NO | Tipo de teléfono |
| `area_code` | VARCHAR(5) | NO | Código de área |
| `phone_number` | VARCHAR(9) | NO | Número telefónico |

### Tipos

```text
CELULAR
FIJO
```

### Celular

Debe contener:

```text
9 dígitos
```

y comenzar con:

```text
9
```

Para un celular:

```text
area_code = ''
```

El número se almacena sin `+51`.

Ejemplo:

```text
phone_type: CELULAR
area_code: ''
phone_number: 987654321
```

### Teléfono fijo

Se almacenan separadamente:

```text
area_code
phone_number
```

Ejemplo:

```text
phone_type: FIJO
area_code: 01
phone_number: 4567890
```

No se almacena:

```text
+51
```

como parte del número nacional.

---

# 10. Unicidad de teléfonos

Un mismo contacto no puede tener dos veces el mismo teléfono.

Se recomienda una restricción:

```text
UNIQUE(contact_id, phone_type, area_code, phone_number)
```

La validación también debe realizarse en la aplicación para proporcionar un mensaje de error comprensible al usuario.

Los teléfonos de diferentes contactos pueden coincidir si las reglas de negocio lo permiten.

---

# 11. Tabla `contact_emails`

Permite registrar uno o varios correos electrónicos por contacto.

## Campos

| Campo | Tipo | Null | Descripción |
|---|---|---:|---|
| `id` | BIGINT | NO | Identificador |
| `contact_id` | BIGINT | NO | Contacto asociado |
| `email` | VARCHAR(254) | NO | Correo electrónico |

Los correos deben almacenarse normalizados en minúsculas.

Ejemplo:

```text
ELISSA@GMAIL.COM
```

se almacena como:

```text
elissa@gmail.com
```

---

# 12. Unicidad de correos

Un contacto no puede registrar dos veces el mismo correo.

Restricción:

```text
UNIQUE(contact_id, email)
```

La validación también se realiza en la aplicación.

---

# 13. Tabla `contact_commercial_types`

Esta tabla representa la relación comercial de un contacto.

## Campos

| Campo | Tipo | Null | Descripción |
|---|---|---:|---|
| `contact_id` | BIGINT | NO | Contacto |
| `commercial_type` | VARCHAR(15) | NO | Tipo comercial |

Tipos válidos:

```text
CLIENTE
PROVEEDOR
```

La clave primaria será compuesta:

```text
PRIMARY KEY(contact_id, commercial_type)
```

Esto garantiza que un contacto no pueda tener dos veces la misma relación comercial.

---

# 14. Ejemplo de relación comercial

Un contacto puede tener:

```text
contact_id = 15
commercial_type = CLIENTE
```

y además:

```text
contact_id = 15
commercial_type = PROVEEDOR
```

Esto representa:

```text
Contacto 15
├── CLIENTE
└── PROVEEDOR
```

No se debe crear otro registro en `contacts`.

---

# 15. Estado del contacto

El campo:

```text
status
```

acepta:

```text
ACTIVO
INACTIVO
```

La eliminación lógica no se representa mediante:

```text
ELIMINADO
```

Un contacto eliminado mantiene su estado anterior y se identifica mediante:

```text
deleted_at IS NOT NULL
```

y:

```text
deleted_by IS NOT NULL
```

Por ejemplo:

```text
status = INACTIVO
deleted_at = 2026-11-25 18:30:00
```

significa que el contacto está eliminado lógicamente y no debe aparecer en las consultas normales.

---

# 16. Eliminación lógica

Los contactos nunca se eliminan físicamente durante el funcionamiento normal del sistema.

Cuando un administrador elimina un contacto:

```text
deleted_at
deleted_by
```

se completan.

Los registros eliminados:

- no aparecen en consultas normales;
- no aparecen en las búsquedas habituales;
- mantienen su identidad;
- mantienen su información histórica;
- no pueden utilizarse para crear un nuevo contacto con la misma identidad.

El administrador puede recuperar un contacto eliminado.

Después de recuperarlo:

```text
status = INACTIVO
deleted_at = NULL
deleted_by = NULL
```

La recuperación no activa automáticamente el contacto.

---

# 17. Auditoría

Las tablas principales deben mantener información sobre quién realizó las operaciones.

Para contactos:

```text
created_by
created_at

updated_by
updated_at

deleted_by
deleted_at
```

Los usuarios utilizados para auditoría corresponden a usuarios autenticados del sistema.

El frontend nunca debe enviar directamente:

```text
created_by
updated_by
deleted_by
```

Estos valores deben obtenerse desde la sesión autenticada.

---

# 18. Relaciones de auditoría

Los campos de auditoría que representan usuarios tendrán una relación con:

```text
users.id
```

Por ejemplo:

```text
contacts.created_by -> users.id
contacts.updated_by -> users.id
contacts.deleted_by -> users.id
```

Esto permite conocer qué usuario realizó cada operación.

Los usuarios no deben eliminarse físicamente, por lo que las referencias históricas permanecen válidas.

---

# 19. Fotografías

La fotografía no se almacena directamente como BLOB en la base de datos.

La base de datos almacena únicamente la referencia:

```text
photo_path
```

Ejemplo:

```text
uploads/contacts/8f2c3d1a-7c4a-4f4a-9d21-photo.jpg
```

El archivo físico se almacena fuera del código fuente.

La aplicación debe validar:

- extensión;
- MIME type;
- contenido real del archivo;
- tamaño máximo;
- dimensiones;
- integridad de la imagen.

Formatos permitidos:

```text
JPEG
JPG
PNG
WebP
```

Tamaño máximo:

```text
2 MB
```

Dimensiones máximas:

```text
800 x 800 px
```

El nombre físico del archivo debe generarse en el servidor.

No debe utilizarse directamente el nombre proporcionado por el usuario.

---

# 20. Dirección

La dirección se almacena como texto:

```text
VARCHAR(200)
```

Es obligatoria para todos los contactos.

Ejemplos válidos:

```text
Av. Javier Prado Este 1234
Jr. Los Álamos 245, Dpto. 301
Calle Lima 456
```

La validación de caracteres y longitud corresponde a la aplicación.

---

# 21. Normalización

La estructura evita almacenar información repetida.

Un contacto puede tener múltiples:

```text
teléfonos
correos
```

por lo que estos datos se almacenan en tablas independientes.

La relación comercial también se separa para permitir:

```text
CLIENTE
PROVEEDOR
```

o ambas.

La estructura resultante evita columnas como:

```text
phone1
phone2
phone3
email1
email2
email3
```

---

# 22. Índices

Se deben crear índices para las consultas utilizadas frecuentemente.

### `users`

```text
UNIQUE(username)
```

### `contacts`

```text
UNIQUE(document_type, document_number)
INDEX(status)
INDEX(person_type)
INDEX(deleted_at)
INDEX(first_name)
INDEX(last_name)
INDEX(legal_name)
```

### `contact_phones`

```text
INDEX(contact_id)
UNIQUE(contact_id, phone_type, area_code, phone_number)
```

### `contact_emails`

```text
INDEX(contact_id)
UNIQUE(contact_id, email)
```

### `contact_commercial_types`

```text
PRIMARY KEY(contact_id, commercial_type)
INDEX(commercial_type, contact_id)
```

Los índices deben estar orientados a las consultas reales del sistema y no agregarse indiscriminadamente.

---

# 23. Claves foráneas

Las relaciones principales serán:

```text
contacts.created_by
    -> users.id

contacts.updated_by
    -> users.id

contacts.deleted_by
    -> users.id

contact_phones.contact_id
    -> contacts.id

contact_emails.contact_id
    -> contacts.id

contact_commercial_types.contact_id
    -> contacts.id
```

Para los registros hijos de un contacto se recomienda impedir la eliminación física accidental del contacto mediante reglas de integridad referencial.

La eliminación funcional del contacto se realiza mediante soft delete.

---

# 24. Consistencia entre aplicación y base de datos

La aplicación debe validar las reglas de negocio antes de persistir información.

La base de datos debe actuar como segunda barrera para garantizar principalmente:

- claves primarias;
- claves foráneas;
- unicidad;
- integridad referencial.

La lógica condicional más compleja, como:

```text
NATURAL -> DNI/CE
JURIDICA -> RUC
```

se valida principalmente en la capa de servicio.

Esto evita convertir la base de datos en el lugar donde reside toda la lógica de negocio.

---

# 25. Transacciones

Las operaciones que modifican varias tablas deben ejecutarse dentro de una transacción.

Por ejemplo, crear un contacto puede involucrar:

```text
contacts
contact_phones
contact_emails
contact_commercial_types
```

La operación debe ser atómica.

Si ocurre un error al guardar alguno de los elementos, la transacción debe revertirse para evitar un contacto parcialmente creado.

Conceptualmente:

```text
BEGIN TRANSACTION

Crear contacto
Crear teléfonos
Crear correos
Crear relaciones comerciales

COMMIT
```

Si ocurre un error:

```text
ROLLBACK
```

---

# 26. Creación de un contacto

El flujo de persistencia esperado es:

```text
1. Validar datos
       ↓
2. Verificar identidad
       ↓
3. Crear contact
       ↓
4. Crear phones
       ↓
5. Crear emails
       ↓
6. Crear commercial types
       ↓
7. Registrar auditoría
       ↓
8. Commit
```

La operación debe ejecutarse dentro de una transacción.

---

# 27. Actualización de un contacto

Los campos de identidad no se modifican.

Para una persona natural son inmutables:

```text
first_name
last_name
document_type
document_number
```

Para una persona jurídica:

```text
legal_name
document_type
document_number
```

Los campos modificables son:

```text
address
phones
emails
photo
```

La relación comercial puede modificarse mediante las operaciones autorizadas.

---

# 28. Estado y eliminación

El modelo separa claramente:

### Estado funcional

```text
ACTIVO
INACTIVO
```

### Eliminación lógica

```text
deleted_at
deleted_by
```

Por lo tanto:

```text
ACTIVO + deleted_at NULL
```

representa un contacto activo.

```text
INACTIVO + deleted_at NULL
```

representa un contacto inactivo.

```text
ACTIVO/INACTIVO + deleted_at NOT NULL
```

representa un contacto eliminado lógicamente.

---

# 29. Usuarios y seguridad

La base de datos nunca debe almacenar:

```text
password
```

en texto plano.

Debe almacenar únicamente:

```text
password_hash
```

El hash será generado mediante el mecanismo seguro proporcionado por Spring Security.

Las credenciales de conexión de MySQL tampoco deben almacenarse en el repositorio.

---

# 30. Zona horaria

La aplicación utilizará:

```text
America/Lima
```

como zona horaria funcional.

La base de datos almacenará valores de fecha/hora como datos temporales, no como strings.

La presentación al usuario deberá utilizar:

```text
dd/MM/yyyy HH:mm
```

Ejemplo:

```text
25/11/2026 18:30
```

La representación visual no debe determinar el tipo de dato almacenado.

---

# 31. Esquema conceptual final

```text
users
 ├── id PK
 ├── username UNIQUE
 ├── password_hash
 ├── role
 ├── enabled
 ├── created_at
 ├── created_by FK
 ├── updated_at
 └── updated_by FK


contacts
 ├── id PK
 ├── person_type
 ├── first_name
 ├── last_name
 ├── document_type
 ├── document_number
 ├── legal_name
 ├── address
 ├── photo_path
 ├── status
 ├── created_at
 ├── created_by FK
 ├── updated_at
 ├── updated_by FK
 ├── deleted_at
 └── deleted_by FK


contact_phones
 ├── id PK
 ├── contact_id FK
 ├── phone_type
 ├── area_code
 └── phone_number


contact_emails
 ├── id PK
 ├── contact_id FK
 └── email


contact_commercial_types
 ├── contact_id PK/FK
 └── commercial_type PK
```

---

# 32. Decisiones de diseño

Se adoptan las siguientes decisiones:

1. Una sola base de datos MySQL.
2. Un único monolito Spring Boot.
3. Un solo registro de contacto por identidad.
4. Tipos comerciales independientes de la identidad.
5. Relación muchos-a-muchos conceptual entre contactos y tipos comerciales, implementada mediante tabla intermedia.
6. Teléfonos y correos en tablas independientes.
7. Eliminación lógica mediante `deleted_at` y `deleted_by`.
8. Estado `ACTIVO/INACTIVO` independiente de la eliminación.
9. Contraseñas almacenadas únicamente como hash.
10. Fotografías almacenadas fuera de la base de datos.
11. La identidad del contacto no se puede modificar.
12. La lógica de negocio se valida principalmente en la aplicación.
13. La base de datos mantiene restricciones de integridad y unicidad.
14. Las operaciones que modifican varias tablas utilizan transacciones.
15. No se utilizan microservicios ni múltiples bases de datos.
16. No se introducen procedimientos almacenados sin una necesidad concreta.

---

# 33. Evolución

Este diseño está pensado para resolver el alcance actual del reto sin introducir complejidad innecesaria.

Si el sistema evolucionara posteriormente podrían incorporarse:

- historial detallado de cambios;
- catálogos administrables;
- búsqueda avanzada;
- paginación;
- almacenamiento de archivos externo;
- recuperación de contraseña;
- nuevos tipos comerciales;
- nuevos tipos de contacto.

Estas funcionalidades no forman parte de la implementación mínima actual salvo que sean requeridas explícitamente.

---

# 34. Fuente de verdad

La implementación de la base de datos debe mantenerse alineada con:

```text
docs/requirements.md
docs/business-rules.md
docs/architecture.md
docs/database.md
```

Ante una modificación de las reglas funcionales, primero debe actualizarse la documentación correspondiente y posteriormente adaptar la implementación.

La base de datos no debe introducir reglas que contradigan los documentos de requisitos y reglas de negocio.