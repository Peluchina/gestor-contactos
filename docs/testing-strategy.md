# Estrategia de Pruebas

## 1. Objetivo

Este documento define la estrategia de pruebas para el sistema **Gestor de Contactos**.

El objetivo es verificar que:

- las reglas de negocio se cumplan;
- los datos sean validados correctamente;
- los permisos por rol sean respetados;
- la autenticación y autorización funcionen correctamente;
- los contactos puedan crearse, consultarse, editarse, inactivarse, eliminarse y recuperarse según las reglas definidas;
- la información sensible esté protegida;
- las operaciones sobre la base de datos mantengan su integridad;
- las funcionalidades generadas o modificadas con ayuda de IA hayan sido verificadas antes de considerarse terminadas.

La estrategia prioriza pruebas simples, relevantes y mantenibles.

No se busca alcanzar cobertura artificialmente alta, sino cubrir especialmente las reglas de negocio y los escenarios críticos.

---

# 2. Principios

Las pruebas deben seguir estos principios:

1. Probar primero las reglas de negocio críticas.
2. Probar tanto escenarios válidos como inválidos.
3. Verificar permisos desde el servidor, no únicamente desde la interfaz.
4. No confiar exclusivamente en las validaciones del frontend.
5. Verificar las restricciones importantes también contra la base de datos.
6. Las pruebas deben ser reproducibles.
7. Cada funcionalidad importante debe tener criterios claros de aceptación.
8. Las pruebas generadas por IA deben ser revisadas por el desarrollador.
9. Una prueba no debe considerarse válida únicamente porque "pasa".
10. Los casos críticos deben ejecutarse nuevamente después de cambios importantes.

---

# 3. Niveles de prueba

El proyecto utilizará principalmente cuatro niveles:

```text
Pruebas unitarias
       ↓
Pruebas de integración
       ↓
Pruebas funcionales
       ↓
Pruebas manuales de aceptación
```

Cada nivel tiene un propósito diferente.

---

# 4. Pruebas unitarias

Las pruebas unitarias verifican componentes individuales sin depender de una base de datos real.

Se utilizarán principalmente:

- JUnit
- Mockito
- Spring Boot Test cuando sea necesario

Las pruebas unitarias se enfocarán especialmente en la capa de servicio y validaciones.

## Componentes principales

Se deben probar, entre otros:

```text
ContactService
UserService
validadores
reglas de documentos
reglas de teléfonos
reglas de correos
reglas de nombres
reglas de dirección
reglas de fotografías
reglas de autorización
```

---

# 5. Reglas de negocio prioritarias

Las siguientes reglas tienen prioridad alta para las pruebas.

## 5.1 DNI

Debe aceptar exactamente 8 dígitos.

### Válido

```text
12345678
```

### Inválidos

```text
1234567
123456789
1234567A
12 345678
1234-5678
```

---

# 6. Carnet de Extranjería

Debe aceptar exactamente 11 dígitos numéricos.

### Válido

```text
12345678901
```

### Inválidos

```text
1234567890
123456789012
1234567890A
```

---

# 7. RUC

Debe aceptar exactamente 11 dígitos numéricos.

### Válido

```text
20123456789
```

### Inválidos

```text
2012345678
201234567890
2012345678A
```

La validación de longitud y caracteres forma parte de la validación mínima del sistema.

---

# 8. Nombres y apellidos

Deben permitir:

- letras;
- espacios;
- caracteres acentuados;
- `Ñ` y `ñ`.

### Válidos

```text
Elissa
María
José Luis
Muñoz
Álvarez
```

### Inválidos

```text
Elissa123
Juan@
Pedro_123
Maria#
```

También deben rechazarse valores vacíos o compuestos únicamente por espacios.

---

# 9. Razón social

La razón social no debe utilizar la misma validación restrictiva que los nombres.

Debe permitir valores como:

```text
Comercial Los Andes S.A.C.
Minería del Sur S.A.
Empresa 123 S.A.C.
```

Debe rechazarse una razón social vacía o compuesta únicamente por espacios.

---

# 10. Dirección

La dirección debe:

- ser obligatoria;
- tener entre 5 y 200 caracteres;
- aceptar números;
- aceptar letras y caracteres habituales de una dirección;
- rechazar valores únicamente compuestos por espacios.

Ejemplos válidos:

```text
Av. Javier Prado Este 1234
Jr. Los Álamos 245, Dpto. 301
Calle Lima 456
```

---

# 11. Teléfonos

Debe existir al menos un teléfono.

## Celular

Debe:

- tener 9 dígitos;
- comenzar con `9`;
- no almacenar `+51` como número nacional.

### Válido

```text
987654321
```

### Inválidos

```text
98765432
887654321
9876543210
+51987654321
987-654-321
```

## Fijo

Debe almacenarse separando:

```text
código de área
número local
```

Ejemplo:

```text
Código: 01
Número: 4567890
```

---

# 12. Correos electrónicos

Debe existir al menos un correo electrónico.

Los correos deben:

- tener formato válido;
- almacenarse en minúsculas;
- no repetirse dentro del mismo contacto.

### Válido

```text
elissa@example.com
```

### Inválidos

```text
elissa
elissa@
@example.com
elissa@com
```

---

# 13. Fotografía

La fotografía es opcional.

Si se proporciona, debe verificarse:

- extensión permitida;
- tipo MIME;
- contenido real del archivo;
- tamaño máximo de 2 MB;
- dimensiones máximas de 800x800 px.

Formatos permitidos:

```text
JPG
JPEG
PNG
WebP
```

Debe rechazarse un archivo que simplemente haya cambiado su extensión.

Ejemplo:

```text
archivo.exe -> archivo.jpg
```

no debe considerarse una imagen válida.

---

# 14. Identidad única

Debe existir una prueba específica para garantizar que no se pueda registrar dos veces la misma identidad.

Ejemplo:

```text
Contacto 1
DNI: 12345678
```

Posteriormente:

```text
Contacto 2
DNI: 12345678
```

debe ser rechazado.

La regla también debe mantenerse si el primer contacto está eliminado lógicamente.

---

# 15. Contacto con dos relaciones comerciales

Debe verificarse que un mismo contacto pueda tener:

```text
CLIENTE
PROVEEDOR
```

sin crear un segundo contacto.

Resultado esperado:

```text
1 contacto
2 relaciones comerciales
```

No:

```text
2 contactos
```

---

# 16. Teléfonos duplicados

Debe rechazarse un teléfono repetido dentro del mismo contacto.

Ejemplo:

```text
987654321
987654321
```

no debe permitirse.

---

# 17. Correos duplicados

Debe rechazarse un correo repetido dentro del mismo contacto.

Ejemplo:

```text
elissa@example.com
elissa@example.com
```

tampoco debe permitirse.

La comparación debe realizarse de forma normalizada.

Por ejemplo:

```text
ELISSA@example.com
elissa@example.com
```

deben considerarse el mismo correo.

---

# 18. Identidad inmutable

Una vez creado un contacto, las siguientes propiedades no deben poder modificarse:

### Natural

```text
Nombres
Apellidos
Tipo de documento
Número de documento
```

### Jurídica

```text
Razón social
Tipo de documento
RUC
```

Las pruebas deben comprobar que una solicitud de modificación de estos campos sea rechazada o ignorada según la implementación definida.

---

# 19. Campos editables

Debe permitirse modificar:

```text
Dirección
Teléfonos
Correos
Fotografía
```

La modificación debe actualizar:

```text
updated_at
updated_by
```

---

# 20. Estados

Debe verificarse:

```text
ACTIVO -> INACTIVO
INACTIVO -> ACTIVO
```

Solo un administrador puede realizar estas operaciones.

Un vendedor, usuario de compras o almacenero no debe poder cambiar el estado aunque intente realizar la operación directamente contra el endpoint.

---

# 21. Eliminación lógica

La eliminación de un contacto debe:

- ser realizada únicamente por un administrador;
- establecer `deleted_at`;
- establecer `deleted_by`;
- mantener el registro en la base de datos;
- ocultarlo de las consultas normales.

Debe comprobarse que la eliminación no borre físicamente el registro.

---

# 22. Recuperación

Un administrador debe poder recuperar un contacto eliminado.

Después de recuperarlo:

```text
deleted_at = NULL
deleted_by = NULL
status = INACTIVO
```

La recuperación no debe activar automáticamente el contacto.

---

# 23. Auditoría

Las pruebas deben comprobar que el usuario autenticado sea utilizado como responsable de la operación.

Por ejemplo:

```text
created_by
updated_by
deleted_by
```

no deben depender de valores enviados desde el frontend.

Debe verificarse que un usuario no pueda modificar la auditoría enviando manualmente otro ID.

---

# 24. Usuarios

## Username

Debe:

- ser obligatorio;
- tener entre 4 y 50 caracteres;
- no contener espacios;
- ser único.

Ejemplos válidos:

```text
elissa
admin
juan.perez
maria123
```

Ejemplos inválidos:

```text
abc
juan perez
```

---

# 25. Contraseñas

Debe verificarse que:

- nunca se almacenen en texto plano;
- se almacene un hash;
- una contraseña incorrecta no permita iniciar sesión;
- una contraseña correcta permita iniciar sesión si el usuario está activo.

No se deben realizar pruebas que intenten recuperar la contraseña original desde la base de datos.

---

# 26. Usuarios inactivos

Un usuario con:

```text
enabled = false
```

no debe poder iniciar sesión.

Debe probarse tanto:

```text
login desde la interfaz
```

como el comportamiento de las rutas protegidas.

---

# 27. Sesión persistente

El sistema utiliza sesión HTTP de Spring Security.

Debe verificarse que:

1. el usuario inicia sesión;
2. navega por el sistema;
3. actualiza la página;
4. la sesión continúa válida;
5. al cerrar sesión, deja de tener acceso a las páginas protegidas.

También debe comprobarse el comportamiento de la sesión según la configuración de persistencia establecida para el proyecto.

La persistencia no debe implementarse utilizando JWT si el diseño definido utiliza sesión HTTP.

---

# 28. Autorización por roles

La autorización debe probarse en el servidor.

## Administrador

Debe poder:

- crear clientes;
- crear proveedores;
- consultar activos e inactivos;
- editar;
- activar;
- inactivar;
- eliminar;
- recuperar;
- administrar usuarios.

## Vendedor

Debe poder:

- crear clientes;
- consultar clientes;
- editar campos permitidos de clientes.

No debe poder:

- eliminar;
- recuperar;
- activar;
- inactivar;
- administrar usuarios;
- gestionar proveedores.

## Compras

Debe poder:

- crear proveedores;
- consultar proveedores;
- editar campos permitidos de proveedores.

No debe poder:

- eliminar;
- recuperar;
- activar;
- inactivar;
- administrar usuarios;
- gestionar clientes.

## Almacenero

Debe poder:

- consultar clientes activos;
- consultar proveedores activos.

No debe poder:

- crear;
- editar;
- eliminar;
- recuperar;
- activar;
- inactivar;
- administrar usuarios.

---

# 29. Protección de datos para almacenero

Debe verificarse que el almacenero no reciba información que no tiene permiso para visualizar.

Para una persona natural debe recibir únicamente:

```text
Nombres
Apellidos
Dirección
```

Para una persona jurídica:

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
Información de auditoría
```

La protección debe aplicarse en el servidor y no únicamente ocultando columnas mediante HTML/CSS.

---

# 30. Protección CSRF

Las operaciones que modifican información deben estar protegidas contra CSRF.

Se deben verificar especialmente:

```text
POST
PUT
PATCH
DELETE
```

según los endpoints utilizados por la aplicación.

No se debe desactivar CSRF simplemente para facilitar las pruebas o el desarrollo.

---

# 31. Acceso no autenticado

Un usuario no autenticado no debe poder acceder directamente a las páginas protegidas.

Ejemplo:

```text
/contactos
/usuarios
```

deben requerir autenticación.

El usuario debe ser redirigido al login o recibir la respuesta de seguridad correspondiente.

---

# 32. Acceso no autorizado

Un usuario autenticado pero sin permisos suficientes tampoco debe poder ejecutar operaciones restringidas.

Por ejemplo:

```text
VENDEDOR -> DELETE /contacts/{id}
```

debe ser rechazado.

No es suficiente ocultar el botón "Eliminar" en la interfaz.

---

# 33. Pruebas de integración

Las pruebas de integración verificarán la interacción entre:

```text
Service
Repository
JPA/Hibernate
MySQL
```

Estas pruebas permitirán comprobar:

- persistencia;
- relaciones;
- restricciones únicas;
- claves foráneas;
- transacciones;
- recuperación de información;
- eliminación lógica.

Cuando sea apropiado, se puede utilizar una base de datos de prueba aislada.

---

# 34. Pruebas de transacciones

Debe verificarse que una operación que involucra varias tablas no deje datos incompletos.

Por ejemplo, al crear un contacto:

```text
contacts
contact_phones
contact_emails
contact_commercial_types
```

Si falla el guardado de uno de los elementos, la operación completa debe revertirse.

Resultado esperado:

```text
ROLLBACK
```

y no un contacto parcialmente creado.

---

# 35. Pruebas funcionales

Las pruebas funcionales validarán el comportamiento de la aplicación desde el punto de vista del usuario.

Casos principales:

### Login

```text
Usuario correcto + contraseña correcta -> acceso
Usuario incorrecto -> rechazo
Contraseña incorrecta -> rechazo
Usuario inactivo -> rechazo
```

### Contactos

```text
Crear contacto válido -> éxito
Crear contacto inválido -> error de validación
Buscar contacto -> resultados correctos
Editar datos permitidos -> éxito
Editar identidad -> rechazo
```

### Estados

```text
Activar -> éxito para ADMIN
Inactivar -> éxito para ADMIN
Activar -> rechazo para otros roles
Inactivar -> rechazo para otros roles
```

### Eliminación

```text
Eliminar -> éxito para ADMIN
Eliminar -> rechazo para otros roles
Recuperar -> éxito para ADMIN
```

---

# 36. Pruebas manuales de aceptación

Además de las pruebas automatizadas, se realizará una validación manual de los principales flujos.

## Flujo 1: Administrador

```text
Login
   ↓
Crear contacto natural
   ↓
Agregar teléfono
   ↓
Agregar correo
   ↓
Guardar
   ↓
Editar dirección
   ↓
Inactivar
   ↓
Eliminar
   ↓
Recuperar
```

Se debe comprobar el estado de cada operación.

---

# 37. Flujo 2: Vendedor

```text
Login como VENDEDOR
        ↓
Crear CLIENTE
        ↓
Consultar CLIENTE
        ↓
Editar teléfono/correo/dirección
        ↓
Intentar eliminar
        ↓
Intentar inactivar
```

Los dos últimos intentos deben ser rechazados.

---

# 38. Flujo 3: Compras

```text
Login como COMPRAS
        ↓
Crear PROVEEDOR
        ↓
Consultar PROVEEDOR
        ↓
Editar datos permitidos
        ↓
Intentar eliminar
        ↓
Intentar inactivar
```

Las operaciones no autorizadas deben ser rechazadas.

---

# 39. Flujo 4: Almacenero

```text
Login como ALMACENERO
        ↓
Consultar CLIENTES
        ↓
Consultar PROVEEDORES
        ↓
Ver nombre/razón social + dirección
        ↓
Intentar acceder a información restringida
        ↓
Intentar crear/editar/eliminar
```

Las operaciones restringidas deben ser rechazadas.

---

# 40. Pruebas de seguridad

Se verificarán como mínimo:

- contraseñas almacenadas como hash;
- CSRF activo;
- autorización en backend;
- acceso protegido a páginas;
- acceso protegido a operaciones;
- archivos subidos validados;
- nombres de archivos generados por el servidor;
- secretos fuera del repositorio;
- datos sensibles no expuestos innecesariamente;
- errores sin stack traces para el usuario final.

---

# 41. Pruebas de archivos

Además de probar archivos válidos, se deben probar archivos maliciosos o inválidos.

Casos:

```text
PDF renombrado como JPG
EXE renombrado como PNG
imagen corrupta
archivo mayor a 2 MB
imagen mayor a 800x800
extensión no permitida
archivo vacío
```

Todos deben ser rechazados.

---

# 42. Pruebas de base de datos

Se debe comprobar:

- unicidad de username;
- unicidad de identidad;
- unicidad de teléfono dentro del contacto;
- unicidad de correo dentro del contacto;
- integridad de claves foráneas;
- persistencia de relaciones comerciales;
- persistencia de auditoría;
- eliminación lógica.

Las restricciones importantes no deben depender únicamente del frontend.

---

# 43. Pruebas de regresión

Cada cambio importante debe volver a verificar las funcionalidades relacionadas.

Por ejemplo, si se modifica:

```text
ContactService
```

se deben volver a ejecutar al menos:

```text
crear contacto
editar contacto
consultar contacto
eliminar contacto
recuperar contacto
validar identidad
```

Si se modifica Spring Security:

```text
login
logout
roles
autorización
CSRF
acceso a páginas
```

deben volver a verificarse.

---

# 44. Criterio de aceptación

Una funcionalidad se considera terminada cuando:

1. cumple el requisito correspondiente;
2. cumple las reglas de negocio relacionadas;
3. tiene validaciones del lado servidor;
4. respeta los permisos;
5. no rompe funcionalidades existentes;
6. tiene pruebas automatizadas cuando corresponde;
7. fue verificada manualmente cuando corresponde;
8. no introduce secretos;
9. no genera errores críticos;
10. la implementación es comprensible para el desarrollador.

---

# 45. Cobertura

La cobertura de código será utilizada como indicador, no como único criterio de calidad.

Se priorizarán:

- servicios;
- validadores;
- reglas de autorización;
- operaciones críticas;
- manejo de errores;
- persistencia de operaciones importantes.

No se buscará cubrir artificialmente cada getter, setter o código trivial.

---

# 46. Pruebas y desarrollo asistido por IA

El proyecto utiliza IA como herramienta de desarrollo.

Cuando Codex genere código, el desarrollador debe verificarlo antes de considerarlo terminado.

El flujo será:

```text
Requisito
   ↓
Implementación propuesta por IA
   ↓
Revisión del código
   ↓
Pruebas automatizadas
   ↓
Pruebas manuales
   ↓
Corrección
   ↓
Verificación final
   ↓
Commit
```

La IA no se considera una fuente de verdad.

La fuente de verdad será:

```text
requirements.md
business-rules.md
architecture.md
database.md
```

---

# 47. Qué debe revisarse en código generado por IA

Antes de aceptar código generado por Codex se debe comprobar:

### Funcionalidad

¿Hace exactamente lo solicitado?

### Reglas de negocio

¿Respeta todas las restricciones?

### Seguridad

¿Permite saltarse permisos?

### Datos

¿Guarda correctamente la información?

### Errores

¿Qué ocurre si la entrada es inválida?

### Transacciones

¿Puede dejar datos parcialmente guardados?

### Mantenibilidad

¿El código es comprensible?

### Sobreingeniería

¿Agrega componentes innecesarios?

### Consistencia

¿Sigue la arquitectura definida?

---

# 48. Registro de decisiones y correcciones de IA

Cuando una propuesta de IA sea modificada o descartada por el desarrollador, debe registrarse de manera resumida en la documentación del proyecto cuando sea relevante.

Ejemplo:

```text
Codex propuso utilizar JWT para autenticación.

La propuesta fue descartada porque la arquitectura definida utiliza
Spring Security con sesión HTTP y no requiere una API stateless.

Se implementó autenticación basada en sesión.
```

Esto demuestra que la IA fue utilizada como herramienta y que las decisiones técnicas fueron revisadas.

---

# 49. Evidencia de verificación

Para las funcionalidades críticas se debe conservar evidencia razonable de que fueron verificadas.

Puede incluir:

- pruebas automatizadas;
- resultados de Maven;
- capturas de pantalla;
- pruebas manuales;
- consultas SQL de verificación;
- historial de commits.

No es necesario documentar cada acción realizada durante el desarrollo.

Se deben conservar principalmente las verificaciones relevantes para los criterios de evaluación.

---

# 50. Ejecución de pruebas

El proyecto debe permitir ejecutar las pruebas mediante Maven.

Comando esperado:

```bash
mvn test
```

La ejecución debe finalizar correctamente antes de considerar estable una modificación importante.

Cuando sea necesario ejecutar todo el proceso de construcción:

```bash
mvn clean test
```

---

# 51. Definición de terminado

Una funcionalidad podrá considerarse **Done** cuando:

```text
[ ] Requisito identificado
[ ] Implementación realizada
[ ] Código revisado
[ ] Validación backend implementada
[ ] Permisos verificados
[ ] Pruebas unitarias correspondientes
[ ] Pruebas de integración correspondientes
[ ] Prueba manual del flujo principal
[ ] Regresión ejecutada
[ ] Sin secretos ni datos sensibles
[ ] Documentación actualizada si corresponde
[ ] Commit realizado
```

---

# 52. Resumen

La estrategia de pruebas se centra en las partes que representan mayor riesgo para el proyecto:

```text
Reglas de negocio
       +
Seguridad
       +
Autorización
       +
Integridad de datos
       +
Persistencia
       +
Validación
       +
Verificación del código generado por IA
```

El objetivo no es únicamente demostrar que la aplicación "funciona", sino demostrar que funciona **de acuerdo con los requisitos, de forma segura y verificable**.