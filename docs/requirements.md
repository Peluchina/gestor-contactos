# Gestor de Contactos — Requisitos del Sistema

## 1. Descripción general

El sistema **Gestor de Contactos** permitirá administrar información de personas naturales y jurídicas que mantienen una relación comercial con la organización como clientes, proveedores o ambos.

El sistema será una aplicación web monolítica con autenticación, autorización por roles, persistencia en una base de datos relacional y almacenamiento seguro de fotografías.

El desarrollo será realizado con apoyo de un agente de inteligencia artificial. El agente podrá proponer código y soluciones técnicas, pero no podrá modificar ni inventar reglas de negocio sin validación previa.

---

# 2. Objetivo

Desarrollar una aplicación web que permita:

- Registrar contactos.
- Consultar contactos.
- Buscar contactos.
- Editar información permitida.
- Administrar clientes.
- Administrar proveedores.
- Asociar un mismo contacto como cliente y proveedor.
- Activar e inactivar contactos.
- Eliminar contactos mediante eliminación lógica.
- Recuperar contactos eliminados.
- Administrar usuarios.
- Controlar el acceso mediante roles.
- Mantener auditoría de las operaciones.
- Proteger información personal y archivos.
- Mantener la sesión autenticada de forma persistente.

---

# 3. Alcance

El sistema contempla:

1. Autenticación de usuarios.
2. Gestión de usuarios.
3. Gestión de contactos.
4. Gestión de clientes.
5. Gestión de proveedores.
6. Gestión de teléfonos.
7. Gestión de correos electrónicos.
8. Gestión de fotografías.
9. Gestión de estados.
10. Eliminación lógica.
11. Recuperación de contactos.
12. Auditoría.
13. Control de acceso por roles.
14. Persistencia de sesión.
15. Validaciones de negocio y seguridad.

---

# 4. Roles del sistema

El sistema tendrá exactamente los siguientes roles:

- `ADMINISTRADOR`
- `VENDEDOR`
- `COMPRAS`
- `ALMACENERO`

Cada usuario tendrá un único rol.

---

# 5. Tipos de persona

Cada contacto debe pertenecer exactamente a uno de los siguientes tipos de persona:

- `NATURAL`
- `JURIDICA`

## 5.1 Persona natural

Una persona natural debe registrar:

- Nombres.
- Apellidos.
- Tipo de documento.
- Número de documento.
- Dirección.
- Uno o más teléfonos.
- Uno o más correos electrónicos.
- Fotografía opcional.

Los tipos de documento permitidos son:

- `DNI`
- `CARNET_EXTRANJERIA`

## 5.2 Persona jurídica

Una persona jurídica debe registrar:

- Razón social.
- RUC.
- Dirección.
- Uno o más teléfonos.
- Uno o más correos electrónicos.
- Fotografía opcional.

El tipo de documento para una persona jurídica será:

- `RUC`

---

# 6. Identidad del contacto

La identidad representa los datos que identifican a la persona o empresa.

## 6.1 Persona natural

La identidad está formada por:

- Nombres.
- Apellidos.
- Tipo de documento.
- Número de documento.

Estos datos son obligatorios y no podrán modificarse después de crear el contacto.

## 6.2 Persona jurídica

La identidad está formada por:

- Razón social.
- RUC.

Estos datos son obligatorios y no podrán modificarse después de crear el contacto.

## 6.3 Unicidad

Una misma identidad no podrá registrarse en más de un contacto.

El sistema debe impedir la creación de duplicados aunque el contacto existente se encuentre inactivo.

La existencia de un contacto con la misma identidad no debe solucionarse creando otro registro.

---

# 7. Tipos comerciales

Un contacto puede desempeñar una o ambas funciones comerciales:

- `CLIENTE`
- `PROVEEDOR`

Estas funciones son independientes.

Por lo tanto, un mismo contacto puede ser:

- solamente cliente;
- solamente proveedor;
- cliente y proveedor.

## 7.1 Regla de no duplicación

Si una persona o empresa ya existe como cliente y posteriormente debe registrarse como proveedor, no se debe crear otro contacto.

Se debe utilizar el mismo contacto y agregar la relación comercial correspondiente.

De la misma forma, un proveedor existente puede convertirse también en cliente sin duplicar su identidad.

---

# 8. Gestión de contactos

El sistema debe permitir:

- Crear contactos.
- Consultar contactos.
- Buscar contactos.
- Visualizar el detalle de un contacto.
- Editar información permitida.
- Cambiar el estado según permisos.
- Eliminar lógicamente contactos.
- Recuperar contactos eliminados según permisos.

---

# 9. Datos editables

Una vez creado un contacto, los datos de identidad no podrán modificarse.

## 9.1 No editables

Persona natural:

- Nombres.
- Apellidos.
- Tipo de documento.
- Número de documento.

Persona jurídica:

- Razón social.
- RUC.

## 9.2 Editables

Podrán modificarse:

- Dirección.
- Teléfonos.
- Correos electrónicos.
- Fotografía.
- Relaciones comerciales permitidas por el rol y flujo de negocio.

---

# 10. Nombres y apellidos

Para personas naturales:

- Son obligatorios.
- Solo deben permitir letras y espacios.
- Deben aceptar caracteres con tilde.
- Deben aceptar `Ñ` y `ñ`.
- No deben permitir números.
- No deben permitir caracteres especiales.
- No deben permitir valores compuestos únicamente por espacios.

Se deben normalizar espacios al inicio, al final y espacios consecutivos.

---

# 11. Razón social

Para personas jurídicas:

- Es obligatoria.
- Debe tener entre 2 y 200 caracteres.
- No puede estar formada únicamente por espacios.
- Puede contener letras, números, espacios y signos corporativos habituales.

La validación de razón social no debe utilizar la misma regla de "solo letras" de nombres y apellidos.

---

# 12. Documentos de identidad

## 12.1 DNI

Debe contener exactamente:

- 8 dígitos numéricos.

## 12.2 Carnet de Extranjería

Debe contener exactamente:

- 11 dígitos numéricos.

## 12.3 RUC

Debe contener exactamente:

- 11 dígitos numéricos.

No se deben permitir:

- letras;
- espacios;
- caracteres especiales.

Las combinaciones permitidas son:

| Tipo de persona | Documento |
|---|---|
| NATURAL | DNI |
| NATURAL | CARNET_EXTRANJERIA |
| JURIDICA | RUC |

---

# 13. Dirección

La dirección es obligatoria.

Debe:

- Tener entre 5 y 200 caracteres.
- No estar formada únicamente por espacios.
- Permitir letras.
- Permitir números.
- Permitir tildes y `Ñ`.
- Permitir espacios.
- Permitir caracteres habituales de direcciones como `-`, `#`, `/`, `.`, `,`.

Ejemplos válidos:

- `Av. Javier Prado Este 1234`
- `Jr. Los Álamos 245, Dpto. 301`
- `Calle 5 Mz. A Lt. 12`
- `Av. Perú 123 - Interior 4`

---

# 14. Teléfonos

Cada contacto debe tener al menos un teléfono.

Cada teléfono debe indicar su tipo:

- `CELULAR`
- `FIJO`

## 14.1 Celular

Debe:

- Tener exactamente 9 dígitos.
- Empezar con `9`.

## 14.2 Teléfono fijo

Debe almacenar por separado:

- Código de área.
- Número local.

No se debe almacenar el código internacional `+51` como parte del número nacional.

El sistema debe evitar teléfonos duplicados dentro del mismo contacto.

Los teléfonos son editables.

---

# 15. Correos electrónicos

Cada contacto debe tener al menos un correo electrónico.

El sistema debe:

- Validar formato de correo.
- Convertir el correo a minúsculas.
- Evitar correos duplicados dentro del mismo contacto.

Los correos electrónicos son editables.

---

# 16. Fotografía

La fotografía es opcional.

Formatos permitidos:

- JPEG/JPG
- PNG
- WebP

Restricciones:

- Tamaño máximo: 2 MB.
- Dimensiones máximas: 800 × 800 píxeles.
- El sistema debe validar el contenido real del archivo.
- No debe confiar únicamente en la extensión.
- Debe rechazar archivos corruptos o que no correspondan a una imagen válida.
- Si las dimensiones exceden el máximo, podrá redimensionarse manteniendo la proporción.
- No se debe deformar la imagen.

El nombre del archivo no debe ser utilizado directamente para determinar el nombre final almacenado.

El archivo debe almacenarse fuera del código fuente y fuera de `src/main/resources/static`.

La carpeta de archivos cargados debe estar excluida de Git.

---

# 17. Estado del contacto

Los estados visibles de un contacto son:

- `ACTIVO`
- `INACTIVO`

La eliminación no se considera un estado visible normal, ya que corresponde a una eliminación lógica.

---

# 18. Activación e inactivación

Solo el `ADMINISTRADOR` puede modificar el estado de un contacto.

Estados permitidos:

```text
ACTIVO -> INACTIVO
INACTIVO -> ACTIVO
```

Los demás roles no pueden activar ni inactivar contactos.

---

# 19. Eliminación lógica

La eliminación de contactos debe ser lógica.

No se debe eliminar físicamente el registro de la base de datos.

Al eliminar un contacto se debe registrar:

- Fecha de eliminación.
- Usuario que realizó la eliminación.

Los contactos eliminados:

- No deben aparecer en consultas normales.
- No deben poder registrarse nuevamente con la misma identidad.
- Deben conservar sus datos para fines de auditoría y recuperación.

Solo el `ADMINISTRADOR` puede eliminar contactos.

---

# 20. Recuperación

Solo el `ADMINISTRADOR` puede recuperar un contacto eliminado.

Al recuperar un contacto:

```text
ELIMINADO -> INACTIVO
```

La recuperación no debe activar automáticamente el contacto.

Para volver a estar activo deberá realizarse una activación posterior por parte del administrador.

---

# 21. Reglas para duplicados

Antes de registrar un contacto, el sistema debe verificar si ya existe la misma identidad.

## Caso 1: no existe

Se permite crear el contacto.

## Caso 2: existe activo

No se debe crear un duplicado.

Si se desea agregar otra relación comercial, debe utilizarse el contacto existente.

Ejemplo:

```text
Juan Pérez
CLIENTE
ACTIVO
```

Si posteriormente debe ser proveedor:

```text
Juan Pérez
CLIENTE + PROVEEDOR
ACTIVO
```

No se crea un segundo Juan Pérez.

## Caso 3: existe inactivo

No se debe crear un duplicado.

El sistema debe informar que el contacto existe pero se encuentra inactivo.

La activación corresponde al administrador.

## Caso 4: existe eliminado

No se debe crear otro contacto con la misma identidad.

El administrador podrá recuperar el contacto eliminado.

---

# 22. Auditoría

El sistema debe registrar información de auditoría.

Para los contactos se debe registrar:

- Usuario creador.
- Fecha y hora de creación.
- Usuario que realizó la última modificación.
- Fecha y hora de la última modificación.
- Usuario que realizó la eliminación.
- Fecha y hora de eliminación.

El usuario utilizado para auditoría debe obtenerse de la sesión autenticada.

No debe enviarse desde el formulario como un dato confiable.

Las fechas deben almacenarse como valores de fecha/hora en la base de datos.

Zona horaria:

```text
America/Lima
```

Formato de visualización:

```text
dd/MM/yyyy HH:mm
```

---

# 23. Usuarios

Solo el `ADMINISTRADOR` puede administrar usuarios.

Debe poder:

- Crear usuarios.
- Consultar usuarios.
- Editar roles.
- Activar usuarios.
- Inactivar usuarios.
- Restablecer contraseñas.

Los usuarios no deben eliminarse físicamente.

---

# 24. Usuario

Cada usuario tendrá:

- ID.
- Username.
- Contraseña almacenada de forma segura.
- Rol.
- Estado.
- Fecha de creación.
- Usuario creador.

---

# 25. Username

El username:

- Es obligatorio.
- Debe ser único.
- No debe contener espacios.
- Debe tener entre 4 y 50 caracteres.
- Debe manejarse de forma consistente respecto a mayúsculas/minúsculas.

Ejemplos:

```text
elissa
admin
juan.perez
maria123
```

El username no debe poder modificarse desde la edición normal del usuario.

---

# 26. Contraseñas

Las contraseñas:

- Nunca deben almacenarse en texto plano.
- Deben almacenarse mediante un algoritmo de hash seguro.
- No deben mostrarse en pantalla.
- No deben aparecer en logs.
- No deben incluirse en respuestas de API o formularios.

El restablecimiento de contraseña debe realizarse mediante una operación específica.

El administrador no debe visualizar la contraseña actual de otro usuario.

---

# 27. Estado del usuario

El usuario tendrá un estado lógico:

- ACTIVO.
- INACTIVO.

Un usuario inactivo:

- No puede iniciar sesión.
- No debe poder acceder al sistema.

No se deben eliminar físicamente usuarios.

---

# 28. Reglas especiales del administrador

El administrador:

- No puede desactivarse a sí mismo.
- No puede quitarse su propio rol de `ADMINISTRADOR`.
- Puede administrar otros usuarios.
- Puede activar e inactivar contactos.
- Puede eliminar y recuperar contactos.

Cuando un administrador desactive otro usuario, se recomienda invalidar sus sesiones activas para impedir que continúe utilizando el sistema.

---

# 29. Permisos del ADMINISTRADOR

El `ADMINISTRADOR` tiene acceso completo al sistema.

Puede:

- Crear clientes.
- Crear proveedores.
- Crear contactos cliente/proveedor.
- Asociar ambas relaciones comerciales.
- Consultar contactos activos e inactivos.
- Editar datos permitidos.
- Activar contactos.
- Inactivar contactos.
- Eliminar contactos.
- Recuperar contactos eliminados.
- Consultar información de auditoría.
- Administrar usuarios.
- Activar e inactivar usuarios.
- Cambiar roles.
- Restablecer contraseñas.

---

# 30. Permisos del VENDEDOR

El `VENDEDOR` trabaja con la función comercial de clientes.

Puede:

- Crear clientes.
- Consultar clientes activos.
- Consultar clientes inactivos para validar duplicados.
- Editar teléfonos.
- Editar correos.
- Editar dirección.
- Editar fotografía.
- Agregar la relación `CLIENTE` a un contacto existente cuando el flujo autorizado lo permita.

No puede:

- Administrar proveedores.
- Activar contactos.
- Inactivar contactos.
- Eliminar contactos.
- Recuperar contactos eliminados.
- Administrar usuarios.
- Modificar la identidad del contacto.

---

# 31. Permisos de COMPRAS

El rol `COMPRAS` trabaja con la función comercial de proveedores.

Puede:

- Crear proveedores.
- Consultar proveedores activos.
- Consultar proveedores inactivos para validar duplicados.
- Editar teléfonos.
- Editar correos.
- Editar dirección.
- Editar fotografía.
- Agregar la relación `PROVEEDOR` a un contacto existente cuando el flujo autorizado lo permita.

No puede:

- Administrar clientes.
- Activar contactos.
- Inactivar contactos.
- Eliminar contactos.
- Recuperar contactos eliminados.
- Administrar usuarios.
- Modificar la identidad del contacto.

---

# 32. Permisos de ALMACENERO

El `ALMACENERO` tiene acceso de solo lectura.

Puede consultar:

- Clientes activos.
- Proveedores activos.

Solo debe visualizar:

Para persona natural:

- Nombres.
- Apellidos.
- Dirección.

Para persona jurídica:

- Razón social.
- Dirección.

No puede visualizar:

- DNI.
- Carnet de Extranjería.
- RUC.
- Teléfonos.
- Correos.
- Fotografía.
- Información de auditoría.

No puede:

- Crear contactos.
- Editar contactos.
- Activar contactos.
- Inactivar contactos.
- Eliminar contactos.
- Recuperar contactos.
- Administrar usuarios.

---

# 33. Visibilidad según rol

| Funcionalidad | ADMIN | VENDEDOR | COMPRAS | ALMACENERO |
|---|---:|---:|---:|---:|
| Crear cliente | Sí | Sí | No | No |
| Crear proveedor | Sí | No | Sí | No |
| Ver clientes activos | Sí | Sí | No | Sí |
| Ver proveedores activos | Sí | No | Sí | Sí |
| Consultar inactivos | Sí | Sí, para duplicados | Sí, para duplicados | No |
| Editar datos permitidos | Sí | Sí, clientes | Sí, proveedores | No |
| Activar/inactivar | Sí | No | No | No |
| Eliminar | Sí | No | No | No |
| Recuperar | Sí | No | No | No |
| Gestionar usuarios | Sí | No | No | No |
| Ver auditoría | Sí | No | No | No |

---

# 34. Autenticación

El sistema debe requerir autenticación para acceder a las funcionalidades protegidas.

Las páginas internas no deben ser accesibles sin iniciar sesión.

Las credenciales incorrectas deben producir un mensaje genérico, por ejemplo:

```text
Usuario o contraseña incorrectos.
```

No se debe revelar si el username o la contraseña fue incorrecta.

---

# 35. Sesión persistente

El sistema debe mantener la sesión autenticada de forma persistente.

La sesión debe:

- Mantenerse al recargar la página.
- Permitir continuar utilizando el sistema posteriormente.
- Cumplir el requisito de persistencia hasta que el usuario cierre sesión, considerando la configuración de seguridad y expiración necesaria para el reto.

No se utilizará JWT para este requisito.

Se utilizará autenticación basada en sesión HTTP de Spring Security.

Cerrar sesión debe invalidar la sesión autenticada.

---

# 36. Autorización

Las restricciones de acceso deben aplicarse en el backend.

No es suficiente ocultar botones o menús en la interfaz.

El servidor debe validar:

- Usuario autenticado.
- Rol.
- Permiso de la operación.
- Estado del recurso.
- Reglas de negocio.

Un usuario no autorizado que intente acceder directamente a una URL protegida debe recibir una respuesta de acceso denegado.

---

# 37. Protección CSRF

Las operaciones que modifiquen información deben estar protegidas contra ataques CSRF.

No se debe desactivar CSRF simplemente para facilitar el desarrollo.

---

# 38. Validación

Las validaciones deben realizarse en el servidor.

La interfaz puede realizar validaciones adicionales para mejorar la experiencia del usuario, pero nunca debe considerarse la única barrera de seguridad.

El backend debe validar:

- Campos obligatorios.
- Formatos.
- Longitudes.
- Documentos.
- Teléfonos.
- Correos.
- Archivos.
- Relaciones comerciales.
- Estados.
- Permisos.
- Duplicados.

---

# 39. Seguridad de archivos

Los archivos cargados deben:

- Validarse por contenido real.
- Tener tipos permitidos.
- Tener límite de tamaño.
- Tener nombres generados por el servidor.
- Almacenarse fuera del código fuente.
- No ejecutarse como contenido del servidor.
- Estar fuera de `src/main/resources/static`.

Los archivos cargados no deben incluirse en el repositorio Git.

---

# 40. Protección de datos personales

La aplicación debe proteger la información personal de los contactos.

Los datos sensibles o restringidos no deben exponerse a roles que no tengan autorización.

Especialmente:

- Documentos.
- Correos.
- Teléfonos.
- Fotografías.
- Información de auditoría.

El rol `ALMACENERO` solo podrá visualizar los datos expresamente autorizados.

---

# 41. Secretos y configuración

Las credenciales y secretos no deben almacenarse directamente en el código fuente.

La configuración sensible debe utilizar variables de entorno o mecanismos equivalentes.

El repositorio debe incluir un archivo de ejemplo como:

```text
.env.example
```

No debe contener credenciales reales.

Los archivos con secretos deben estar incluidos en `.gitignore`.

---

# 42. Base de datos

El sistema utilizará una base de datos relacional.

Tecnología prevista:

- MySQL.

La base de datos debe garantizar integridad mediante:

- claves primarias;
- claves foráneas;
- restricciones de unicidad;
- restricciones apropiadas de nulabilidad;
- relaciones consistentes.

---

# 43. Persistencia

Los datos deben mantenerse después de reiniciar la aplicación.

No se debe utilizar almacenamiento temporal como mecanismo principal de persistencia.

Las fotografías deben almacenarse mediante almacenamiento de archivos y la base de datos debe mantener únicamente la referencia necesaria al archivo.

---

# 44. Manejo de errores

El sistema debe manejar los errores de manera controlada.

No debe mostrar al usuario:

- Stack traces.
- Credenciales.
- Variables de entorno.
- Detalles internos de infraestructura.
- Información sensible de la base de datos.

Los errores deben registrarse mediante mecanismos de logging apropiados.

---

# 45. Arquitectura

El sistema será una aplicación monolítica.

Tecnologías previstas:

- Java 17.
- Spring Boot.
- Spring Security.
- Spring Data JPA.
- MySQL.
- Thymeleaf.
- Maven.

La solución debe mantenerse simple y apropiada para el alcance del reto.

No se requiere implementar:

- Microservicios.
- Kafka.
- Redis.
- JWT.
- CQRS.
- Event Sourcing.
- Arquitecturas distribuidas.

La arquitectura debe priorizar:

- claridad;
- mantenibilidad;
- seguridad;
- separación de responsabilidades;
- facilidad de explicación durante la evaluación.

---

# 46. Interfaz

La aplicación debe proporcionar como mínimo:

## Login

- Username.
- Contraseña.
- Inicio de sesión.
- Mensaje de error seguro.

## Dashboard

La navegación debe variar según el rol.

## Contactos

- Listado.
- Búsqueda.
- Registro.
- Detalle.
- Edición.
- Activación/inactivación cuando corresponda.
- Eliminación cuando corresponda.
- Recuperación para administrador.

## Usuarios

Disponible únicamente para administrador:

- Listado.
- Crear.
- Editar rol.
- Activar.
- Inactivar.
- Restablecer contraseña.

---

# 47. Navegación por rol

## ADMINISTRADOR

```text
Inicio
Contactos
  ├── Clientes
  └── Proveedores
Usuarios
Mi perfil
Cerrar sesión
```

## VENDEDOR

```text
Inicio
Clientes
Mi perfil
Cerrar sesión
```

## COMPRAS

```text
Inicio
Proveedores
Mi perfil
Cerrar sesión
```

## ALMACENERO

```text
Inicio
Contactos
  ├── Clientes
  └── Proveedores
Mi perfil
Cerrar sesión
```

---

# 48. Requisitos funcionales

## RF-01 — Autenticación

El sistema debe permitir iniciar sesión mediante username y contraseña.

## RF-02 — Gestión de usuarios

El administrador debe poder crear y administrar usuarios.

## RF-03 — Roles

El sistema debe implementar los roles:

- ADMINISTRADOR
- VENDEDOR
- COMPRAS
- ALMACENERO

## RF-04 — Gestión de clientes

El sistema debe permitir gestionar contactos con relación comercial CLIENTE según los permisos del usuario.

## RF-05 — Gestión de proveedores

El sistema debe permitir gestionar contactos con relación comercial PROVEEDOR según los permisos del usuario.

## RF-06 — Registro de contactos

El sistema debe permitir registrar personas naturales y jurídicas.

## RF-07 — Documentos

El sistema debe validar DNI, Carnet de Extranjería y RUC según el tipo de persona.

## RF-08 — Identidad inmutable

La identidad del contacto no debe poder modificarse después de su creación.

## RF-09 — Dirección

El sistema debe registrar y permitir modificar la dirección según permisos.

## RF-10 — Teléfonos

El sistema debe permitir registrar uno o más teléfonos y modificarlos.

## RF-11 — Correos

El sistema debe permitir registrar uno o más correos y modificarlos.

## RF-12 — Fotografía

El sistema debe permitir cargar una fotografía opcional con las restricciones definidas.

## RF-13 — Estado

El sistema debe manejar los estados ACTIVO e INACTIVO.

## RF-14 — Activación e inactivación

Solo el administrador puede cambiar el estado del contacto.

## RF-15 — Eliminación lógica

Solo el administrador puede eliminar lógicamente un contacto.

## RF-16 — Recuperación

Solo el administrador puede recuperar un contacto eliminado.

## RF-17 — Administración

El administrador debe tener acceso completo a las funcionalidades administrativas.

## RF-18 — VENDEDOR

El vendedor debe poder gestionar la función comercial de clientes de acuerdo con sus permisos.

## RF-19 — COMPRAS

El usuario de compras debe poder gestionar la función comercial de proveedores de acuerdo con sus permisos.

## RF-20 — ALMACENERO

El almacenero debe tener acceso de solo lectura a los datos permitidos.

## RF-21 — Unicidad

El sistema debe impedir duplicar la identidad de una persona o empresa.

## RF-22 — Auditoría de creación

El sistema debe registrar quién y cuándo creó el contacto.

## RF-23 — Auditoría de modificación

El sistema debe registrar quién y cuándo modificó el contacto.

## RF-24 — Auditoría de eliminación

El sistema debe registrar quién y cuándo eliminó el contacto.

## RF-25 — Recuperación auditada

La recuperación de un contacto eliminado debe conservar la información histórica necesaria para auditoría.

## RF-26 — Sesión persistente

El sistema debe mantener la sesión autenticada conforme al requisito de persistencia definido.

## RF-27 — Cierre de sesión

El usuario debe poder cerrar sesión y finalizar su sesión autenticada.

---

# 49. Requisitos no funcionales

## RNF-01 — Seguridad de contraseñas

Las contraseñas deben almacenarse utilizando hashing seguro.

## RNF-02 — Autorización backend

Los permisos deben validarse en el servidor.

## RNF-03 — Protección de rutas

Las funcionalidades protegidas deben requerir autenticación y autorización.

## RNF-04 — Protección CSRF

Las operaciones modificadoras deben estar protegidas contra CSRF.

## RNF-05 — Validación server-side

Los datos deben validarse en el backend.

## RNF-06 — Seguridad de archivos

Las fotografías deben validarse, limitarse y almacenarse de forma segura.

## RNF-07 — Protección de datos personales

Los datos deben ser visibles únicamente para los roles autorizados.

## RNF-08 — Protección de secretos

Las credenciales y secretos no deben almacenarse en el repositorio.

## RNF-09 — Manejo seguro de errores

Los errores internos no deben exponerse al usuario.

## RNF-10 — Zona horaria

La aplicación debe trabajar con la zona horaria `America/Lima`.

## RNF-11 — Base de datos relacional

La persistencia debe realizarse en MySQL.

## RNF-12 — Integridad

La base de datos debe mantener relaciones e información consistente.

## RNF-13 — Arquitectura monolítica

La solución debe implementarse como una aplicación monolítica.

## RNF-14 — Tecnologías

La solución debe utilizar Java 17, Spring Boot, Spring Security, Spring Data JPA, Thymeleaf, MySQL y Maven.

## RNF-15 — Pruebas

Las reglas críticas deben contar con pruebas automatizadas.

Como mínimo se deben priorizar pruebas para:

- autenticación;
- autorización;
- validación de documentos;
- duplicados;
- permisos por rol;
- eliminación lógica;
- recuperación;
- validación de teléfonos;
- validación de correos.

## RNF-16 — Archivos cargados

Los archivos subidos por usuarios deben estar excluidos del repositorio Git.

---

# 50. Criterios generales de aceptación

El sistema será considerado funcionalmente aceptable cuando:

1. Un usuario registrado pueda iniciar sesión.
2. Un usuario inactivo no pueda iniciar sesión.
3. Las páginas protegidas no sean accesibles sin autenticación.
4. Los permisos se validen en backend.
5. Un contacto pueda ser natural o jurídico.
6. Una persona natural pueda registrarse con DNI o Carnet de Extranjería.
7. Una persona jurídica pueda registrarse con RUC.
8. Los documentos sean validados según el tipo de persona.
9. No puedan existir dos contactos con la misma identidad.
10. Un contacto pueda ser cliente, proveedor o ambos.
11. Agregar una nueva relación comercial no genere un duplicado.
12. Los datos de identidad no puedan modificarse después del registro.
13. Un contacto tenga al menos un teléfono.
14. Un contacto tenga al menos un correo.
15. Las fotografías respeten las restricciones establecidas.
16. Solo el administrador pueda activar o inactivar contactos.
17. Solo el administrador pueda eliminar contactos.
18. La eliminación sea lógica.
19. Solo el administrador pueda recuperar contactos eliminados.
20. La recuperación deje el contacto en estado INACTIVO.
21. Se registre quién creó, modificó y eliminó un contacto.
22. El almacenero solo vea la información permitida.
23. El vendedor no pueda administrar proveedores.
24. Compras no pueda administrar clientes.
25. Los usuarios puedan ser administrados únicamente por el administrador.
26. El administrador no pueda desactivarse a sí mismo.
27. Las contraseñas no se almacenen en texto plano.
28. CSRF permanezca habilitado.
29. Los secretos no se encuentren en el repositorio.
30. La sesión permanezca autenticada de acuerdo con el requisito de persistencia.
31. El cierre de sesión invalide la sesión.
32. Los datos permanezcan almacenados después de reiniciar la aplicación.
33. Existan pruebas automatizadas para las reglas críticas.
34. El sistema pueda ejecutarse siguiendo las instrucciones del README.

---

# 51. Desarrollo asistido por IA

El proyecto será desarrollado con apoyo de un agente de inteligencia artificial.

El agente podrá:

- Generar código.
- Proponer estructuras.
- Generar pruebas.
- Proponer consultas.
- Ayudar a detectar errores.
- Sugerir mejoras técnicas.

Sin embargo, el agente no debe:

- Inventar reglas de negocio.
- Cambiar requisitos sin autorización.
- Eliminar restricciones por conveniencia.
- Introducir dependencias innecesarias.
- Implementar funcionalidades fuera del alcance sin aprobación.

La persona desarrolladora debe revisar y comprender el código generado.

Las decisiones importantes deben quedar documentadas.

El repositorio debe conservar los archivos de instrucciones utilizados por el agente.

---

# 52. Git y trazabilidad

El desarrollo debe realizarse utilizando Git, manteniendo un historial de cambios claro, pequeño y comprensible.

Los commits deben representar cambios concretos y relacionados con una funcionalidad, corrección, prueba o documentación.

Se recomienda utilizar mensajes de commit en español y mantener una estructura consistente.

Ejemplos:

```text
feat: implementar registro de contactos
feat: agregar autorización por roles
feat: implementar eliminación lógica
test: agregar pruebas de validación de contactos
fix: evitar documentos duplicados
fix: corregir permisos del vendedor
docs: actualizar reglas de negocio
docs: actualizar documentación de arquitectura
refactor: separar lógica de validación de contactos
```

Los commits deben evitar mensajes genéricos como:

```text
cambios
avance
correcciones
final
prueba
cosas
```

Cada commit debe permitir identificar rápidamente qué se modificó.

El historial de Git debe mostrar la evolución del proyecto y servir como evidencia del proceso de desarrollo.

No se deben incluir en el repositorio:

- Dependencias compiladas.
- Archivos temporales.
- Fotografías cargadas por usuarios.
- Contraseñas.
- Tokens.
- Claves privadas.
- Archivos `.env` que contengan secretos reales.
- Archivos generados que no sean necesarios para ejecutar o construir el proyecto.

Debe incluirse un archivo `.gitignore` apropiado para evitar la incorporación accidental de estos archivos.

El repositorio público debe contener únicamente archivos necesarios para comprender, ejecutar, probar y evaluar el proyecto.

Los commits deben ser pequeños y significativos, evitando realizar un único commit gigante con todo el proyecto terminado.
---

# 53. Documentación

El proyecto debe incluir un `README.md` que explique como mínimo:

- Descripción del proyecto.
- Tecnologías utilizadas.
- Requisitos previos.
- Configuración de MySQL.
- Configuración de variables de entorno.
- Ejecución del proyecto.
- Usuario de prueba.
- Roles.
- Reglas de negocio principales.
- Arquitectura.
- Persistencia de sesión.
- Medidas de seguridad.
- Manejo de fotografías.
- Pruebas.
- Flujo de desarrollo asistido por IA.
- Propuestas realizadas por la IA que fueron aceptadas.
- Propuestas realizadas por la IA que fueron modificadas o descartadas.
- Decisiones técnicas.
- Mejoras futuras.

---

# 54. Principio de fuente de verdad

Este documento define los requisitos funcionales y no funcionales del sistema.

Las reglas específicas de negocio se detallan en:

```text
docs/business-rules.md
```

En caso de conflicto:

1. Una regla de negocio explícita tiene prioridad sobre una implementación generada por IA.
2. El código debe adaptarse a los requisitos.
3. No se debe cambiar una regla de negocio únicamente para facilitar la implementación.

Antes de implementar funcionalidades ambiguas que puedan modificar el comportamiento del negocio, se debe revisar y actualizar la documentación correspondiente.

---

# 55. Estado del documento

Este documento representa la versión consolidada de los requisitos antes del diseño de la arquitectura.

El siguiente paso será definir:

```text
docs/architecture.md
```

La arquitectura deberá derivarse de estos requisitos y de las reglas de negocio definidas en `docs/business-rules.md`.