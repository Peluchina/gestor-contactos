# Reglas de negocio — Gestor de Contactos

## 1. Reglas generales de contactos

### BR-01 — Unicidad del contacto

Cada persona o empresa debe registrarse como un único contacto dentro del sistema.

No se debe crear un registro separado para la misma persona o empresa cuando esta tenga más de un tipo comercial.

Por ejemplo, una empresa que sea simultáneamente cliente y proveedor debe existir como un solo contacto.

```text
Comercial ABC S.A.C.
├── CLIENTE
└── PROVEEDOR
```

### BR-02 — Tipo de persona

Cada contacto debe pertenecer exactamente a uno de los siguientes tipos:

- `NATURAL`
- `JURIDICA`

El tipo de persona determina los datos de identificación que debe registrar el contacto.

### BR-03 — Tipos comerciales

Un contacto puede tener uno o ambos tipos comerciales:

- `CLIENTE`
- `PROVEEDOR`

Por lo tanto, son válidas las siguientes situaciones:

| Tipo de persona | Tipo comercial |
|---|---|
| NATURAL | CLIENTE |
| NATURAL | PROVEEDOR |
| NATURAL | CLIENTE + PROVEEDOR |
| JURIDICA | CLIENTE |
| JURIDICA | PROVEEDOR |
| JURIDICA | CLIENTE + PROVEEDOR |

No existe una exclusión entre `CLIENTE` y `PROVEEDOR`.

### BR-04 — Datos obligatorios según tipo de persona

Para una **persona natural** son obligatorios:

- Nombres.
- Apellidos.
- Tipo de documento.
- Número de documento.
- Dirección.
- Al menos un teléfono.
- Al menos un correo electrónico.

Para una **persona jurídica** son obligatorios:

- Razón social.
- RUC.
- Dirección.
- Al menos un teléfono.
- Al menos un correo electrónico.

La fotografía es opcional para ambos tipos.

### BR-05 — Identidad inmutable

Después de crear un contacto, los datos que identifican a la persona o empresa no pueden modificarse.

Para una persona natural son inmutables:

- Nombres.
- Apellidos.
- Tipo de documento.
- Número de documento.

Para una persona jurídica son inmutables:

- Razón social.
- RUC.

Estos datos deben mostrarse bloqueados en la pantalla de edición.

### BR-06 — Datos editables

Después de crear un contacto pueden modificarse:

- Dirección.
- Teléfonos.
- Correos electrónicos.
- Fotografía.

La posibilidad de modificar estos datos depende además del rol del usuario.

---

# 2. Reglas de identificación

## BR-07 — Documentos según tipo de persona

Los documentos permitidos dependen del tipo de persona.

### Persona natural

Puede utilizar:

- `DNI`
- `CARNET_EXTRANJERIA`

### Persona jurídica

Debe utilizar:

- `RUC`

No se permiten combinaciones como:

```text
Persona natural + RUC
Persona jurídica + DNI
Persona jurídica + CARNET_EXTRANJERIA
```

## BR-08 — DNI

El DNI debe:

- contener exactamente 8 dígitos numéricos;
- no contener letras;
- no contener espacios;
- no contener caracteres especiales.

Ejemplo válido:

```text
12345678
```

Ejemplos inválidos:

```text
1234567
123456789
1234567A
12 345678
```

## BR-09 — Carnet de Extranjería

El Carnet de Extranjería debe:

- contener exactamente 11 dígitos numéricos;
- no contener letras;
- no contener espacios;
- no contener caracteres especiales.

Ejemplo válido:

```text
12345678901
```

## BR-10 — RUC

El RUC debe:

- contener exactamente 11 dígitos numéricos;
- no contener letras;
- no contener espacios;
- no contener caracteres especiales.

Ejemplo:

```text
20123456789
```

El sistema debe validar la estructura del RUC y su unicidad.

La validación de existencia o situación real del RUC ante SUNAT no forma parte del alcance inicial, salvo que posteriormente se defina una integración específica.

## BR-11 — Unicidad de la identificación

La identificación debe ser única dentro del sistema.

Para una persona natural:

```text
tipo_persona + tipo_documento + numero_documento
```

Para una persona jurídica:

```text
tipo_persona + tipo_documento + numero_documento
```

En la práctica, el documento identifica a un único contacto.

Por ejemplo:

```text
RUC 20123456789
```

solamente puede pertenecer a un contacto.

Ese contacto puede ser:

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

pero nunca deben existir dos contactos diferentes con el mismo RUC.

### BR-12 — Identificación independiente del tipo comercial

La identificación de una persona o empresa no depende de si actualmente es cliente o proveedor.

Por ejemplo, si existe:

```text
Comercial ABC S.A.C.
RUC: 20123456789
Tipo comercial: PROVEEDOR
```

y posteriormente la empresa también se convierte en cliente, se debe modificar el tipo comercial del mismo contacto:

```text
Comercial ABC S.A.C.
RUC: 20123456789
Tipo comercial:
    PROVEEDOR
    CLIENTE
```

No se debe crear otro contacto.

### BR-13 — Contacto con ambos tipos comerciales

Un contacto que sea simultáneamente cliente y proveedor debe compartir:

- identidad;
- dirección;
- teléfonos;
- correos;
- fotografía;
- auditoría.

No deben duplicarse estos datos por cada tipo comercial.

## BR-14 — Validación de identificación

La validación de identificación debe realizarse:

1. En el frontend para mejorar la experiencia del usuario.
2. En el backend para garantizar la integridad del sistema.
3. En la base de datos mediante restricciones de unicidad.

El frontend nunca debe considerarse una medida de seguridad suficiente.

---

# 3. Reglas de nombres, apellidos y razón social

## BR-15 — Nombres y apellidos de persona natural

Los nombres y apellidos:

- son obligatorios;
- solo pueden contener letras y espacios;
- deben permitir caracteres con tilde;
- deben permitir `Ñ` y `ñ`;
- deben permitir nombres y apellidos compuestos.

Ejemplos válidos:

```text
María
José Luis
Juan Pérez
María José García López
Ñusta
```

Ejemplos inválidos:

```text
Juan123
Pedro@
Ana#
Carlos_01
```

## BR-16 — Espacios en nombres y apellidos

No se deben permitir:

- valores vacíos;
- valores compuestos únicamente por espacios;
- espacios innecesarios al inicio o al final.

El sistema debe normalizar los espacios innecesarios antes de guardar la información.

## BR-17 — Razón social

La razón social es obligatoria para las personas jurídicas.

Debe tener entre:

```text
2 y 200 caracteres
```

No puede estar compuesta únicamente por espacios.

Puede contener letras, números, espacios y caracteres normalmente utilizados en denominaciones empresariales.

Ejemplos:

```text
Comercial Los Andes S.A.C.
Tecnología e Innovación Perú S.R.L.
Servicios Generales ABC E.I.R.L.
```

La razón social no debe tratarse como un nombre personal, por lo que no se aplicará la misma restricción de "solo letras" utilizada para nombres y apellidos.

## BR-18 — Normalización de razón social

Antes de almacenar la razón social, el sistema debe:

- eliminar espacios al inicio;
- eliminar espacios al final;
- normalizar espacios consecutivos.

---

# 4. Reglas de dirección

## BR-19 — Dirección obligatoria

La dirección es obligatoria para todos los contactos, tanto personas naturales como jurídicas.

## BR-20 — Longitud

La dirección debe tener entre:

```text
5 y 200 caracteres
```

No se considera válida una dirección compuesta únicamente por espacios.

## BR-21 — Caracteres permitidos

La dirección puede contener:

- letras;
- letras con tildes;
- `Ñ` y `ñ`;
- números;
- espacios;
- guion `-`;
- numeral `#`;
- barra `/`;
- punto `.`;
- coma `,`.

Ejemplos válidos:

```text
Av. Javier Prado Este 1234
Jr. Los Álamos 245, Dpto. 301
Calle 5 Mz. A Lt. 12
Av. Perú 123 - Interior 4
```

## BR-22 — Normalización de dirección

Antes de almacenar la dirección, el sistema debe:

- eliminar espacios al inicio;
- eliminar espacios al final;
- normalizar espacios consecutivos.

La dirección debe tratarse como texto plano.

No debe interpretarse ni ejecutarse como HTML o JavaScript.

---

# 5. Reglas de teléfonos

## BR-23 — Cantidad mínima

Todo contacto debe tener al menos un teléfono.

Un contacto puede registrar múltiples teléfonos independientemente de que sea cliente, proveedor o ambos.

## BR-24 — Tipos de teléfono

Cada teléfono debe indicar uno de los siguientes tipos:

- `CELULAR`
- `FIJO`

## BR-25 — Teléfono celular

Un teléfono celular debe:

- contener exactamente 9 dígitos;
- comenzar con `9`.

Ejemplo:

```text
987654321
```

No se debe almacenar el código de país `+51` dentro del número nacional.

## BR-26 — Teléfono fijo

Los teléfonos fijos deben manejar:

- código de área numérico de 2 o 3 dígitos;
- número local numérico de exactamente 7 dígitos.

El código de área y el número local deben almacenarse separadamente.
No se almacena `+51` como parte del número nacional.

## BR-27 — Duplicidad de teléfonos

Un mismo teléfono no puede registrarse dos veces dentro del mismo contacto.

La validación debe realizarse en el backend.

---

# 6. Reglas de correos electrónicos

## BR-28 — Cantidad mínima

Todo contacto debe tener al menos un correo electrónico.

Un contacto puede registrar múltiples correos.

## BR-29 — Formato

Cada correo debe cumplir con un formato válido de correo electrónico.

Ejemplo:

```text
usuario@dominio.com
```

## BR-30 — Normalización

Los correos deben almacenarse en minúsculas.

Por ejemplo:

```text
ELISSA@EMAIL.COM
```

debe almacenarse como:

```text
elissa@email.com
```

## BR-31 — Correos duplicados

Un mismo correo no puede registrarse dos veces dentro del mismo contacto.

---

# 7. Reglas de fotografía

## BR-32 — Fotografía opcional

La fotografía es opcional tanto para personas naturales como jurídicas.

## BR-33 — Formatos permitidos

Se permiten únicamente:

- JPEG/JPG
- PNG
- WebP

## BR-34 — Tamaño máximo

El archivo original no debe superar los:

```text
2 MB
```

## BR-35 — Dimensiones

La fotografía debe tener como máximo:

```text
800 x 800 píxeles
```

Si la imagen supera estas dimensiones, puede redimensionarse manteniendo su proporción.

No se debe deformar la imagen.

## BR-36 — Validación del archivo

El sistema debe validar el contenido real del archivo y no confiar únicamente en:

- extensión;
- nombre del archivo;
- `Content-Type` enviado por el navegador.

Debe rechazarse un archivo que:

- no sea realmente una imagen;
- esté corrupto;
- sea un archivo ejecutable;
- sea un PDF renombrado como imagen;
- supere las restricciones establecidas.

## BR-37 — Nombre del archivo

El nombre final del archivo debe ser generado por el servidor.

No se debe utilizar directamente el nombre proporcionado por el usuario.

La fotografía debe almacenarse fuera de los recursos estáticos del código fuente, por ejemplo:

```text
uploads/
```

La carpeta de archivos cargados no debe incluirse en Git.

---

# 8. Reglas de tipos comerciales

## BR-38 — Cliente

Un contacto puede tener el tipo comercial `CLIENTE`.

Cuando un contacto sea cliente, puede ser gestionado según los permisos correspondientes al rol `VENDEDOR`.

## BR-39 — Proveedor

Un contacto puede tener el tipo comercial `PROVEEDOR`.

Cuando un contacto sea proveedor, puede ser gestionado según los permisos correspondientes al rol `COMPRAS`.

## BR-40 — Cliente y proveedor simultáneamente

Un contacto puede tener simultáneamente:

```text
CLIENTE + PROVEEDOR
```

Esto no implica crear dos contactos.

Ejemplo:

```text
Contacto:
    Comercial ABC S.A.C.
    RUC: 20123456789

Tipos comerciales:
    CLIENTE
    PROVEEDOR
```

## BR-41 — Agregar un tipo comercial

Si un contacto existente adquiere un nuevo tipo comercial, se debe agregar ese tipo al mismo contacto.

No se debe crear un nuevo registro de persona o empresa.

## BR-42 — Eliminación de un tipo comercial

Si posteriormente se requiere retirar un tipo comercial, se debe retirar solamente la relación comercial correspondiente.

No se debe eliminar el contacto si todavía posee otro tipo comercial.

Por ejemplo:

```text
Antes:
CLIENTE + PROVEEDOR

Se elimina PROVEEDOR

Después:
CLIENTE
```

El contacto continúa existiendo como cliente.

La eliminación completa del contacto continúa siendo una operación independiente y exclusiva del administrador.

---

# 9. Reglas de estados

## BR-43 — Estados de contacto

Un contacto no eliminado puede encontrarse en uno de estos estados:

- `ACTIVO`
- `INACTIVO`

El estado pertenece al contacto completo y no se duplica por cada tipo comercial.

## BR-44 — Activación e inactivación

Solamente el rol `ADMINISTRADOR` puede cambiar el estado de un contacto.

Se permiten ambas transiciones:

```text
ACTIVO -> INACTIVO
INACTIVO -> ACTIVO
```

## BR-45 — Usuarios no administradores

Los roles:

- `VENDEDOR`
- `COMPRAS`
- `ALMACENERO`

no pueden activar ni inactivar contactos.

---

# 10. Eliminación y recuperación

## BR-46 — Eliminación lógica

La eliminación de contactos debe ser lógica.

No se debe eliminar físicamente el registro de la base de datos.

Al eliminar un contacto se debe registrar:

```text
deleted_at
deleted_by
```

## BR-47 — Permiso de eliminación

Solamente `ADMINISTRADOR` puede eliminar contactos.

Los demás roles no pueden eliminarlos.

## BR-48 — Contactos eliminados

Los contactos eliminados:

- no deben aparecer en las consultas normales;
- no deben aparecer en los listados habituales;
- no deben poder editarse como contactos normales;
- no deben considerarse disponibles para crear otro contacto con la misma identificación.

## BR-49 — Recuperación

Solamente `ADMINISTRADOR` puede recuperar un contacto eliminado.

## BR-50 — Estado después de recuperar

Cuando un contacto eliminado sea recuperado, debe regresar al estado:

```text
INACTIVO
```

No debe recuperarse automáticamente como `ACTIVO`.

Flujo:

```text
ACTIVO
   |
   v
INACTIVO
   |
   v
ELIMINADO
   |
   v
RECUPERADO
   |
   v
INACTIVO
```

---

# 11. Reglas de usuarios

## BR-51 — Roles

Cada usuario debe tener exactamente uno de los siguientes roles:

- `ADMINISTRADOR`
- `VENDEDOR`
- `COMPRAS`
- `ALMACENERO`

## BR-52 — Usuario único

El nombre de usuario debe ser único.

Se recomienda manejar la comparación de forma insensible a mayúsculas/minúsculas.

Por ejemplo:

```text
elissa
```

y:

```text
ELISSA
```

deben considerarse el mismo usuario.

## BR-53 — Nombre de usuario

El username debe:

- ser obligatorio;
- tener entre 4 y 50 caracteres;
- no contener espacios.

Ejemplos válidos:

```text
elissa
admin
juan.perez
maria123
```

## BR-54 — Contraseñas

Las contraseñas:

- nunca deben almacenarse en texto plano;
- deben almacenarse mediante un algoritmo de hash seguro;
- nunca deben enviarse ni mostrarse nuevamente en texto plano.

El sistema debe utilizar el mecanismo de hashing recomendado por Spring Security, como BCrypt.

## BR-55 — Estado de usuario

Los usuarios tendrán un estado lógico mediante:

```text
enabled = true
enabled = false
```

Conceptualmente:

```text
true  -> ACTIVO
false -> INACTIVO
```

Un usuario inactivo no puede iniciar sesión.

## BR-56 — Usuarios sin eliminación física

Los usuarios no deben eliminarse físicamente.

Para impedir su acceso se debe desactivar su cuenta.

## BR-57 — Administración de usuarios

Solamente `ADMINISTRADOR` puede:

- crear usuarios;
- modificar roles;
- activar usuarios;
- inactivar usuarios;
- restablecer contraseñas;
- consultar usuarios.

## BR-58 — Autoprotección del administrador

Un administrador:

- no puede desactivarse a sí mismo;
- no puede eliminar su propio rol de administrador.

Esto evita dejar el sistema sin un administrador operativo.

## BR-59 — Restablecimiento de contraseña

El administrador puede realizar un restablecimiento de contraseña mediante una acción independiente.

La contraseña actual del usuario no debe mostrarse.

---

# 12. Reglas de roles y permisos

## BR-60 — Administrador

El `ADMINISTRADOR` tiene permisos completos sobre contactos.

Puede:

- crear clientes;
- crear proveedores;
- agregar ambos tipos comerciales a un mismo contacto;
- consultar clientes;
- consultar proveedores;
- consultar contactos que sean cliente y proveedor;
- consultar activos e inactivos;
- editar datos permitidos;
- activar contactos;
- inactivar contactos;
- eliminar contactos;
- recuperar contactos eliminados;
- consultar auditoría;
- administrar usuarios.

## BR-61 — Vendedor

El `VENDEDOR` trabaja con la función comercial de cliente.

Puede:

- crear contactos como clientes;
- agregar `CLIENTE` a un contacto existente cuando la funcionalidad corresponda y las reglas de autorización lo permitan;
- consultar clientes activos;
- consultar clientes inactivos cuando sea necesario para validar duplicados;
- editar teléfonos;
- editar correos;
- editar dirección;
- editar fotografía.

No puede:

- gestionar la función de proveedor;
- activar contactos;
- inactivar contactos;
- eliminar contactos;
- recuperar contactos;
- administrar usuarios;
- modificar datos de identidad.

Las reglas anteriores aplican tanto a clientes naturales como jurídicos.

## BR-62 — Compras

El rol `COMPRAS` trabaja con la función comercial de proveedor.

Puede:

- crear contactos como proveedores;
- agregar `PROVEEDOR` a un contacto existente cuando la funcionalidad corresponda y las reglas de autorización lo permitan;
- consultar proveedores activos;
- consultar proveedores inactivos cuando sea necesario para validar duplicados;
- editar teléfonos;
- editar correos;
- editar dirección;
- editar fotografía.

No puede:

- gestionar la función de cliente;
- activar contactos;
- inactivar contactos;
- eliminar contactos;
- recuperar contactos;
- administrar usuarios;
- modificar datos de identidad.

Las reglas anteriores aplican tanto a proveedores naturales como jurídicos.

## BR-63 — Almacenero

El `ALMACENERO` tiene permisos de solo lectura.

Puede consultar:

- clientes activos;
- proveedores activos.

Para cada contacto solamente puede visualizar:

- nombres y apellidos, cuando sea persona natural;
- razón social, cuando sea persona jurídica;
- dirección.

Si un contacto es simultáneamente cliente y proveedor, puede aparecer en ambas consultas, pero continúa siendo un único contacto.

No puede visualizar:

- DNI;
- Carnet de Extranjería;
- RUC;
- teléfono;
- correo;
- fotografía;
- información de auditoría.

No puede:

- crear contactos;
- editar contactos;
- activar contactos;
- inactivar contactos;
- eliminar contactos;
- recuperar contactos;
- administrar usuarios.

---

# 13. Reglas de duplicidad y contactos inactivos

## BR-64 — Validación antes de crear

Antes de crear un contacto, el sistema debe verificar si ya existe un contacto con la misma identificación.

La búsqueda debe considerar contactos:

- activos;
- inactivos;
- eliminados lógicamente.

## BR-65 — Identificación existente con otro tipo comercial

Si ya existe un contacto con la misma identificación y el usuario desea registrarlo con otro tipo comercial, el sistema no debe crear un nuevo contacto.

Debe utilizarse el contacto existente.

Ejemplo:

```text
Contacto existente:
RUC 20123456789
PROVEEDOR
```

Si posteriormente se requiere que también sea cliente:

```text
RUC 20123456789
CLIENTE + PROVEEDOR
```

## BR-66 — Cliente inactivo

Si un `VENDEDOR` intenta crear un cliente cuya identificación ya pertenece a un contacto inactivo, el sistema debe impedir la creación de un nuevo contacto.

Debe informar que ya existe un contacto con esa identificación y que se encuentra inactivo.

El vendedor no puede crear un duplicado.

La activación del contacto corresponde al administrador.

## BR-67 — Proveedor inactivo

Si un usuario de `COMPRAS` intenta crear un proveedor cuya identificación ya pertenece a un contacto inactivo, el sistema debe impedir la creación de un nuevo contacto.

La activación corresponde al administrador.

## BR-68 — Contacto existente como otro tipo comercial

Si un contacto existente está activo y ya posee un tipo comercial diferente, el sistema puede permitir agregar el nuevo tipo comercial de acuerdo con los permisos del usuario.

Por ejemplo:

```text
Contacto:
Comercial ABC S.A.C.
Estado: ACTIVO
Tipo comercial: PROVEEDOR
```

Puede pasar a:

```text
Contacto:
Comercial ABC S.A.C.
Estado: ACTIVO
Tipo comercial:
    PROVEEDOR
    CLIENTE
```

No se crea un segundo registro.

---

# 14. Reglas de auditoría

## BR-69 — Creación

Al crear un contacto se debe registrar:

```text
created_by
created_at
```

## BR-70 — Modificación

Al modificar un contacto se debe registrar:

```text
updated_by
updated_at
```

## BR-71 — Eliminación

Al eliminar un contacto se debe registrar:

```text
deleted_by
deleted_at
```

## BR-72 — Usuario responsable

Los campos de auditoría relacionados con el usuario deben obtenerse del usuario autenticado.

El frontend no debe enviar valores para determinar quién realizó la operación.

El backend debe obtener esta información desde la sesión autenticada.

## BR-73 — Fecha y hora

Las fechas deben almacenarse como valores temporales en la base de datos, no como cadenas de texto.

La aplicación utilizará la zona horaria:

```text
America/Lima
```

La interfaz mostrará:

```text
dd/MM/yyyy HH:mm
```

utilizando formato de 24 horas.

---

# 15. Reglas de seguridad

## BR-74 — Autenticación

Las páginas protegidas solamente pueden ser utilizadas por usuarios autenticados.

## BR-75 — Autorización

La autorización debe comprobarse en el backend.

Ocultar botones en la interfaz no constituye una medida suficiente para impedir una operación no autorizada.

Aunque un botón no aparezca para un usuario, el backend debe rechazar igualmente una solicitud no autorizada realizada manualmente.

## BR-76 — Protección CSRF

Las operaciones que modifican información deben estar protegidas contra CSRF mediante Spring Security.

No se debe desactivar CSRF simplemente para facilitar el desarrollo.

## BR-77 — Contraseñas

Las contraseñas deben almacenarse únicamente mediante hash seguro.

Nunca deben almacenarse:

- en texto plano;
- en código fuente;
- en archivos de configuración versionados;
- en logs.

## BR-78 — Archivos

Los archivos cargados por los usuarios deben validarse antes de almacenarse.

El sistema no debe permitir que una carga de archivos permita ejecutar código en el servidor.

## BR-79 — Datos personales

Los datos personales de los contactos solamente deben mostrarse según los permisos correspondientes al rol autenticado.

## BR-80 — Secretos

Las credenciales de base de datos, claves y otros secretos no deben almacenarse en Git.

El proyecto debe proporcionar:

```text
.env.example
```

con valores de ejemplo y sin secretos reales.

---

# 16. Regla de sesión persistente

## BR-81 — Sesión autenticada

Una vez autenticado correctamente, el usuario debe poder:

- navegar entre las diferentes páginas permitidas;
- recargar el navegador;
- continuar utilizando el sistema sin iniciar sesión nuevamente.

La sesión debe mantenerse de acuerdo con el requisito de persistencia definido para el sistema.

## BR-82 — Logout

Al cerrar sesión:

- la sesión autenticada debe finalizar;
- el usuario debe ser redirigido al login;
- no debe poder acceder nuevamente a páginas protegidas utilizando la sesión cerrada.

---

# 17. Principio para el desarrollo con IA

## BR-83 — La IA no define las reglas de negocio

Codex puede proponer:

- código;
- estructura;
- implementaciones;
- pruebas;
- mejoras técnicas.

Sin embargo, no puede modificar unilateralmente las reglas definidas en este documento.

Si durante el desarrollo aparece una ambigüedad que afecta el comportamiento funcional, se debe detener la implementación de esa parte y definir primero la regla de negocio.

## BR-84 — Verificación del código generado

Todo código generado por IA debe ser revisado antes de incorporarse al proyecto.

La revisión debe comprobar como mínimo:

- cumplimiento de las reglas de negocio;
- seguridad;
- autorización por rol;
- validaciones;
- manejo de errores;
- integridad de datos;
- pruebas correspondientes.

## BR-85 — Cambios de reglas

Si una regla de negocio cambia durante el desarrollo, se debe actualizar primero este documento y posteriormente adaptar el código.

El código no debe convertirse en la única fuente de verdad de las reglas del sistema.
