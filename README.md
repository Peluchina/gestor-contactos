# Gestor de Contactos

Aplicación web monolítica para administrar contactos que pueden relacionarse como clientes, proveedores o ambos. Incluye autenticación, permisos por rol, auditoría y persistencia en MySQL.

## Objetivo

Registrar y consultar personas naturales y jurídicas, mantener sus datos de contacto y gestionar sus relaciones comerciales sin duplicar una misma identidad.

## Funcionalidades implementadas

- Inicio de sesión con usuarios almacenados en MySQL y contraseñas codificadas con BCrypt.
- Gestión administrativa de usuarios: creación, consulta, cambio de rol, habilitación e inhabilitación y restablecimiento de contraseña.
- Creación y consulta de contactos naturales y jurídicos.
- Gestión de clientes y proveedores, incluida la asociación de ambas relaciones al mismo contacto.
- Edición de dirección, teléfonos, correos y fotografía. La identidad del contacto no se puede cambiar desde la edición normal.
- Activación e inactivación, eliminación lógica y recuperación de contactos.
- Auditoría de creación, última modificación y eliminación.
- Validación de documentos, datos de contacto y fotografías en el servidor.

## Stack tecnológico

- Java 17
- Spring Boot 4.2.0, Spring MVC y Spring Security
- Spring Data JPA / Hibernate
- MySQL
- Thymeleaf
- Maven Wrapper
- JUnit y Mockito para pruebas

## Requisitos previos

- JDK 17.
- MySQL 8.x disponible localmente.
- Acceso a MySQL con el usuario `root` para preparar el esquema usado por la configuración actual.
- Una contraseña de MySQL disponible como variable de entorno `DB_PASSWORD`.

## Estructura general

```text
database/schema.sql                         Esquema inicial de MySQL
docs/                                        Requisitos, reglas y documentación técnica
src/main/java/.../config                    Configuración de Spring
src/main/java/.../controller                Controladores web
src/main/java/.../dto                       Objetos de entrada y presentación
src/main/java/.../entity                    Entidades JPA
src/main/java/.../repository                Repositorios
src/main/java/.../security                  Autenticación y sesiones
src/main/java/.../service                   Lógica de negocio
src/main/java/.../validation                Validaciones del servidor
src/main/resources/templates                Vistas Thymeleaf
src/main/resources/static                  CSS, JavaScript y recursos estáticos
src/test/java                               Pruebas
```

## Arquitectura

El proyecto es un monolito organizado por capas:

```text
Navegador → Controller → Service → Repository → MySQL
```

Los controladores coordinan las solicitudes web, los servicios aplican reglas de negocio y permisos, los repositorios acceden a los datos y Thymeleaf renderiza las vistas.

## Preparar MySQL

La configuración actual se conecta a `localhost:3306`, usa la base `gestor_contactos` y el usuario `root`. Estos valores sirven para el entorno local de desarrollo y no deben considerarse una configuración de producción.

Desde la raíz del repositorio, ejecuta el esquema inicial con el cliente de MySQL.
En PowerShell:

```powershell
Get-Content .\database\schema.sql | mysql -u root -p
```

En una terminal Unix:

```bash
mysql -u root -p < database/schema.sql
```

El script crea la base de datos con `utf8mb4` y las tablas requeridas. La aplicación tiene `spring.jpa.hibernate.ddl-auto=validate`: comprueba que el esquema exista y coincida con las entidades, pero no lo crea ni lo actualiza automáticamente.

## Variables de entorno

Configura estas variables en el sistema operativo, terminal o configuración de ejecución desde donde iniciarás la aplicación:

```text
DB_PASSWORD=
INITIAL_ADMIN_USERNAME=
INITIAL_ADMIN_PASSWORD=
```

`DB_PASSWORD` debe corresponder a la contraseña MySQL del usuario configurado (`root`). Las otras dos variables se usan para crear al primer administrador cuando la tabla `users` aún está vacía. Si ya hay usuarios, el administrador inicial no se vuelve a crear. Si no hay usuarios y falta alguna de esas dos variables, la aplicación no puede completar el inicio.

Spring Boot no carga automáticamente los archivos `.env`. Las variables deben estar disponibles en el entorno del proceso que inicia la aplicación; copiar `.env.example` a `.env`, por sí solo, no las configura.

La URL y el usuario MySQL permanecen fijados en `src/main/resources/application.properties`. No se utilizan variables `DB_URL` ni `DB_USERNAME`.

## Ejecutar la aplicación

Después de preparar el esquema y configurar las variables de entorno:

```powershell
.\mvnw.cmd spring-boot:run
```

En sistemas Unix, usa `./mvnw spring-boot:run`.

Abre [http://localhost:8080/login](http://localhost:8080/login). El primer administrador se crea al iniciar la aplicación si la tabla de usuarios está vacía y se configuraron sus dos variables.

## Contactos y reglas principales

### Personas naturales y jurídicas

- Las personas naturales registran nombres, apellidos y DNI o Carnet de Extranjería.
- Las personas jurídicas registran razón social y RUC.
- La identidad documental es única y no se modifica mediante la edición normal.
- Se validan DNI de 8 dígitos, Carnet de Extranjería de 11 y RUC de 11.
- La razón social admite letras, números y puntuación corporativa; sus espacios iniciales, finales y consecutivos se normalizan.
- La dirección es obligatoria y tiene entre 5 y 200 caracteres.

### Clientes, proveedores y asociación comercial

Un contacto puede ser `CLIENTE`, `PROVEEDOR` o ambos. La asociación cruzada agrega la relación que falta al contacto existente, sin crear otro contacto para la misma identidad.

### Teléfonos y correos

- Cada contacto debe tener al menos un teléfono y un correo.
- Celular: exactamente 9 dígitos, comenzando con 9.
- Fijo: código de área separado de 2 o 3 dígitos y número local de exactamente 7 dígitos.
- No se almacena `+51` como parte del número nacional.
- Los correos se validan, normalizan a minúsculas y no se repiten dentro del contacto.

### Fotografías

La fotografía es opcional. Se aceptan JPEG/JPG, PNG y WebP, hasta 2 MB. El servidor comprueba el contenido real, asigna un nombre único y limita las dimensiones a 800 × 800 píxeles mediante redimensionado proporcional. Los archivos se guardan en `uploads/`, fuera del código fuente; esa carpeta está excluida de Git. WebP se convierte a PNG al almacenarse.

### Estados, eliminación y recuperación

Los estados son `ACTIVO` e `INACTIVO`. Solo `ADMINISTRADOR` puede cambiar el estado, eliminar lógicamente o recuperar un contacto. La recuperación limpia `deleted_at` y `deleted_by` y deja el contacto `INACTIVO`.

### Auditoría

Los contactos registran creación, última modificación y eliminación junto con los usuarios responsables. Las horas se generan con `America/Lima`; en el detalle se muestran como `dd/MM/yyyy HH:mm`. La auditoría de contactos está disponible para administradores.

## Roles y permisos

| Rol | Acceso implementado |
|---|---|
| `ADMINISTRADOR` | Gestiona clientes, proveedores, contactos, estados, eliminaciones, recuperaciones y usuarios. |
| `VENDEDOR` | Crea y gestiona contactos con relación de cliente; puede agregar la relación cliente a un contacto existente. |
| `COMPRAS` | Crea y gestiona contactos con relación de proveedor; puede agregar la relación proveedor a un contacto existente. |
| `ALMACENERO` | Consulta contactos activos y recibe solo nombre o razón social y dirección. |

Los permisos se verifican en el servidor además de limitar las opciones de la interfaz.

## Seguridad y sesión

- Spring Security autentica con formulario y sesión HTTP.
- BCrypt protege las contraseñas almacenadas.
- Las rutas protegidas requieren autenticación y las operaciones restringidas verifican el rol.
- CSRF permanece habilitado para solicitudes que modifican información.
- El cierre de sesión invalida la sesión HTTP.
- No hay una función “remember-me”; no se promete conservar el inicio de sesión al cerrar el navegador o reiniciar el servidor.
- Las cargas se validan por contenido y se almacenan fuera de `src/main/resources/static`.

La configuración de conexión actual es local y usa `root`, `useSSL=false` y `allowPublicKeyRetrieval=true`. Debe revisarse y sustituirse por una configuración apropiada antes de cualquier despliegue productivo.

## Pruebas

Ejecuta la suite con:

```powershell
.\mvnw.cmd clean test
```

En sistemas Unix, usa `./mvnw clean test`.

Las pruebas cubren servicios, validaciones, controladores, seguridad, usuarios, sesiones y fotografías. `ContactPersistenceTest` requiere `DB_PASSWORD` y una instancia MySQL disponible con el esquema preparado. Si no se configura `DB_PASSWORD`, esa prueba se omite; por tanto, una ejecución local sin MySQL no verifica persistencia real.

## Documentación del proyecto

- [Requisitos](docs/requirements.md)
- [Reglas de negocio](docs/business-rules.md)
- [Arquitectura](docs/architecture.md)
- [Diseño de base de datos](docs/database.md)
- [Estrategia de pruebas](docs/testing-strategy.md)
- [Instrucciones para agentes](AGENTS.md)

## Desarrollo asistido por Codex/IA

Codex se utiliza como apoyo de implementación; no es la fuente de verdad. `AGENTS.md` y la documentación de `docs/` guían el trabajo. Las propuestas de IA deben revisarse para confirmar reglas, permisos, seguridad, manejo de datos, transacciones y mantenibilidad antes de aceptarlas.

Antes de considerar un cambio listo para commit, el flujo del proyecto requiere revisar el código afectado, ejecutar y corregir las pruebas, verificar las funcionalidades relevantes y revisar el diff. Los commits deben ser pequeños y describir un cambio significativo. Las decisiones o correcciones relevantes a propuestas de IA se registran cuando corresponda; no se asume que una propuesta generada sea correcta solo porque compila.

## Mejoras futuras razonables

Estas son posibilidades, no funcionalidades disponibles actualmente:

- Permitir configurar la URL y el usuario de MySQL para facilitar otros entornos; hoy están fijados para desarrollo local.
- Ejecutar `ContactPersistenceTest` de forma regular en un entorno con MySQL disponible, además de mantener las pruebas que pueden correr sin esa base.
