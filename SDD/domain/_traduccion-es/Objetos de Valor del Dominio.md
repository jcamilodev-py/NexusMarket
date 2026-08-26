> ⚠️ **DOCUMENTO NO ENTREGABLE — SOLO PARA COMPRENSIÓN PERSONAL**
>
> Esta es la traducción completa al español de `SDD/domain/Domain Value Objects.md`.
> El entregable es **la versión en inglés**; este archivo no se entrega.
>
> **Qué está traducido:** todo el contenido, incluidos los nombres de los Objetos de Valor
> (`OrderStatus` → `EstadoPedido`) y los códigos de los catálogos (`PAID` → `PAGADO`).
>
> **Qué NO está traducido:** los tipos de dato de Java (`String`), los códigos ISO 4217 de
> las monedas (COP, USD, EUR), que son estándar internacional, y las referencias a la
> especificación (`DOMINIO 6`, `Sección 11`, `OBJ-11`), que ya están en español.
>
> El glosario de equivalencias está al final de `Modelo de Dominio.md`.

---

# Objetos de Valor del Dominio

## Introducción

Los Objetos de Valor representan conceptos inmutables dentro del dominio de NexusMarket.

A diferencia de las Entidades, los Objetos de Valor no tienen identidad propia. Están definidos enteramente por sus valores y se usan para encapsular conceptos de negocio controlados, mejorar la expresividad del dominio y evitar el uso de valores primitivos o de cadenas literales dispersas por toda la aplicación.

El dominio del marketplace usa Objetos de Valor para catálogos de negocio como roles, estados, tipos de producto, tipos de movimiento, tipos de operación, tipos de entidad afectada y monedas.

Todos los catálogos de negocio heredan de `CatálogoDominio`.

Donde la especificación nombra un catálogo pero solo da ejemplos parciales de sus valores — por ejemplo "Activo, Bloqueado, etc." — o nombra el concepto sin listar valores en absoluto, los valores restantes se infieren del contexto de negocio, reflejando cómo el modelo de referencia bancario fue construido a partir de prosa y no de tablas exhaustivas. Cada catálogo en esa situación declara su justificación en su propia sección, de modo que el razonamiento viaje dentro del documento y no en una lista de revisión aparte.

---

# Jerarquía de Objetos de Valor

```text
CatálogoDominio (Abstracto)
├── RolSistema
├── EstadoUsuario
├── EstadoComercialComprador
├── TipoProducto
├── EstadoProducto
├── EstadoInventario
├── TipoMovimientoInventario
├── EstadoPedido
├── EstadoPago
├── EstadoEnvío
├── EstadoDevolución
├── EstadoReembolso
├── TipoOperación
├── TipoEntidadAfectada
└── Moneda
```

---

# CatálogoDominio (Abstracto)

## Descripción

Representa un catálogo de negocio genérico usado a lo largo del dominio de NexusMarket.

`CatálogoDominio` provee una estructura consistente para los valores de negocio controlados que requieren un código, un nombre legible por humanos y una descripción de negocio.

Todos los valores de negocio controlados heredan de esta clase, garantizando una estructura consistente en toda la aplicación.

Esta clase no puede instanciarse directamente.

## Atributos

| Atributo    | Tipo   | Descripción                                                     |
| ----------- | ------ | --------------------------------------------------------------- |
| código      | String | Identificador único de negocio del valor del catálogo.          |
| nombre      | String | Nombre legible por humanos mostrado dentro de la aplicación.    |
| descripción | String | Definición de negocio del valor del catálogo.                   |

## Características

* Inmutable.
* La igualdad se determina por valor y no por identidad de objeto.
* Los valores del catálogo son controlados por el dominio.
* Los valores del catálogo no deben representarse mediante cadenas arbitrarias a lo largo de la aplicación.
* Cada valor del catálogo debe tener un `código` único.

---

# RolSistema

## Descripción

Representa las responsabilidades y permisos asignados a un participante dentro del marketplace.

El rol es una característica de `Usuario` porque representa lo que el participante significa dentro del sistema y las responsabilidades asociadas a él. Cada usuario ostenta exactamente un rol, y ese rol es lo que determina qué especialización de `Usuario` aplica.

El atributo `rol` está por lo tanto definido en `Usuario` y es heredado por sus especializaciones.

## Hereda De

`CatálogoDominio`

## Valores Permitidos

**Fuente:** Sección 5. Participantes del Negocio; RG-02 ("Cada usuario tendrá un único rol dentro del sistema"). Los cinco valores están declarados literalmente en la especificación.

| Código              | Nombre              | Descripción                                                             |
| ------------------- | ------------------- | ----------------------------------------------------------------------- |
| COMPRADOR           | Comprador           | Compra productos publicados en el marketplace.                          |
| VENDEDOR            | Vendedor            | Registra y administra sus propios productos.                            |
| OPERADOR_LOGÍSTICO  | Operador Logístico  | Administra la operación física de bodegas y despachos.                  |
| ADMINISTRADOR       | Administrador       | Administra vendedores, bodegas y reembolsos.                            |
| SUPERVISOR          | Supervisor          | Perfil de consulta y seguimiento operativo, con acceso de solo lectura. |

---

# EstadoUsuario

## Descripción

Representa si un usuario puede operar normalmente dentro de la plataforma.

`EstadoUsuario` describe el estado del acceso del participante al sistema. Es independiente de `EstadoComercialComprador`, que describe la capacidad de compra de un comprador: un comprador puede ser un usuario activo de la plataforma y aun así estar comercialmente restringido.

## Hereda De

`CatálogoDominio`

## Valores Permitidos

**Fuente:** DOMINIO 1 — "Estado: Condición operativa (Activo, Bloqueado, etc.)". `INACTIVO` se infiere para completar el catálogo, reflejando el catálogo equivalente de la referencia bancaria: un usuario que ya no está activo pero que nunca fue sancionado (`BLOQUEADO`) es una condición de negocio distinta y frecuente, por ejemplo una cuenta de vendedor bajo revisión o un empleado en licencia. El propio "etc." de la especificación indica que la lista que da no es exhaustiva.

| Código    | Nombre    | Descripción                                                    |
| --------- | --------- | -------------------------------------------------------------- |
| ACTIVO    | Activo    | El usuario puede acceder al sistema normalmente.               |
| INACTIVO  | Inactivo  | El usuario existe pero no puede ejecutar operaciones.          |
| BLOQUEADO | Bloqueado | El acceso del usuario ha sido suspendido.                      |

---

# EstadoComercialComprador

## Descripción

Representa la condición del comprador para realizar compras.

Este catálogo es independiente de `EstadoUsuario`. Un comprador cuyo estado comercial no sea `HABILITADO` no puede confirmar un pedido, aunque su acceso a la plataforma permanezca activo.

## Hereda De

`CatálogoDominio`

## Valores Permitidos

**Fuente:** DOMINIO 2 — "Estado comercial: Condición del comprador para realizar compras." La especificación nombra el atributo pero no enumera sus valores. Inferido como catálogo de dos estados: la existencia de un estado comercial separado del estado de usuario solo tiene sentido si un comprador puede ser impedido de comprar sin que su cuenta esté bloqueada a nivel de `EstadoUsuario` — es decir, por razones comerciales y no de seguridad. Una distinción binaria es la forma mínima que le da sentido al atributo.

| Código      | Nombre      | Descripción                                                |
| ----------- | ----------- | ---------------------------------------------------------- |
| HABILITADO  | Habilitado  | El comprador puede realizar pedidos normalmente.           |
| RESTRINGIDO | Restringido | El comprador está temporalmente impedido de comprar.       |

---

# TipoProducto

## Descripción

Representa si un producto es físico o digital, determinando si requiere inventario y envío.

Este catálogo se mantiene como atributo de `Producto` además de la especialización `ProductoFísico` / `ProductoDigital`, de modo que el catálogo pueda filtrarse y validarse sin depender del tipo del objeto en tiempo de ejecución.

## Hereda De

`CatálogoDominio`

## Valores Permitidos

**Fuente:** DOMINIO 5 — "Tipo de Producto: Físico o Digital." Ambos valores están declarados literalmente en la especificación.

| Código  | Nombre  | Descripción                                                                    |
| ------- | ------- | ------------------------------------------------------------------------------ |
| FÍSICO  | Físico  | Requiere control de inventario y despacho físico a través de una bodega.      |
| DIGITAL | Digital | Se entrega inmediatamente tras la confirmación del pago.                       |

---

# EstadoProducto

## Descripción

Representa el estado de publicación de un producto en el catálogo.

Solo un producto en estado `PUBLICADO` es visible en el catálogo público y puede añadirse a un carrito.

## Hereda De

`CatálogoDominio`

## Valores Permitidos

**Fuente:** DOMINIO 5 — "Estado: Publicado, Suspendido o Descontinuado." Los tres valores están declarados literalmente en la especificación.

| Código        | Nombre        | Descripción                                       |
| ------------- | ------------- | ------------------------------------------------- |
| PUBLICADO     | Publicado     | Visible en el catálogo público.                   |
| SUSPENDIDO    | Suspendido    | Temporalmente oculto del catálogo.                |
| DESCONTINUADO | Descontinuado | Retirado permanentemente de la venta activa.      |

## Ciclo de Vida

```text
PUBLICADO
    │
    ├──────────────> SUSPENDIDO
    │                    │
    │                    └──────> PUBLICADO
    │
    └──────────────> DESCONTINUADO
```

`SUSPENDIDO` es reversible; `DESCONTINUADO` es terminal.

---

# EstadoInventario

## Descripción

Representa la condición de un registro de existencias, usada para determinar si es elegible para reserva.

## Hereda De

`CatálogoDominio`

## Valores Permitidos

**Fuente:** Sección 11 — "No se puede reservar inventario inexistente o marcado como 'Dañado'." `DISPONIBLE` se infiere como la contraparte necesaria de `DAÑADO`: la regla de validación solo tiene sentido como una distinción binaria entre existencias que pueden y que no pueden reservarse.

| Código     | Nombre     | Descripción                                                       |
| ---------- | ---------- | ----------------------------------------------------------------- |
| DISPONIBLE | Disponible | Las existencias pueden reservarse y venderse normalmente.         |
| DAÑADO     | Dañado     | Las existencias están marcadas como dañadas y no pueden reservarse. |

---

# TipoMovimientoInventario

## Descripción

Representa el tipo de cambio aplicado a un registro de inventario.

Los movimientos de inventario son el registro histórico de cada variación de existencias, mientras que `Inventario` mantiene las cantidades actuales resultantes.

## Hereda De

`CatálogoDominio`

## Valores Permitidos

**Fuente:** DOMINIO 6 — "Movimientos: Ingreso, Reserva, Salida por venta, Ajuste y Devolución." Los cinco valores están declarados literalmente en la especificación.

| Código       | Nombre           | Descripción                                                         |
| ------------ | ---------------- | ------------------------------------------------------------------- |
| INGRESO      | Ingreso          | Existencias que entran a la bodega.                                 |
| RESERVA      | Reserva          | Existencias reservadas para un pedido pendiente.                    |
| SALIDA_VENTA | Salida por Venta | Existencias que salen de la bodega por una venta confirmada.        |
| AJUSTE       | Ajuste           | Corrección manual de la cantidad de existencias.                    |
| DEVOLUCIÓN   | Devolución       | Existencias que reingresan por una devolución de producto aprobada. |

---

# EstadoPedido

## Descripción

Representa la etapa actual del ciclo de vida de un pedido.

El estado describe la situación actual del pedido, mientras que sus operaciones proveen el registro histórico de las acciones ejecutadas a lo largo de su ciclo de vida.

**Nota sobre el valor `CARRITO`.** El DOMINIO 7 lista "Carrito" como la primera etapa del ciclo de vida del pedido. En este modelo esa etapa está materializada por la entidad `Carrito`, que es independiente de `Pedido` porque el OBJ-07 y el OBJ-08 son dos objetivos funcionales distintos. Como consecuencia, **ninguna instancia de `Pedido` ostenta jamás el valor `CARRITO`**: un pedido se crea directamente en `PENDIENTE_PAGO`. El valor se conserva de todos modos en el catálogo para que este permanezca fiel a las cinco etapas que la especificación enumera literalmente, y para que un revisor que compare la especificación con este documento las encuentre todas.

## Hereda De

`CatálogoDominio`

## Valores Permitidos

**Fuente:** DOMINIO 7 — "Ciclo de Estados del Pedido"; Sección 11 ("Un pedido finalizado no podrá ser modificado"). `CANCELADO` es inferido: la especificación enumera únicamente la ruta exitosa, pero un pedido cuyo `Pago` nunca alcanza `APROBADO` requiere un estado terminal — sin él, un pedido no pagado permanecería en `PENDIENTE_PAGO` indefinidamente y los valores `RECHAZADO` y `FALLIDO` de `EstadoPago` no tendrían consecuencia alguna a nivel del pedido.

| Código          | Nombre                 | Descripción                                                                                                                                     |
| --------------- | ---------------------- | ----------------------------------------------------------------------------------------------------------------------------------------------- |
| CARRITO         | Carrito                | Selección provisional de productos. Etapa representada por la entidad `Carrito`; ninguna instancia de `Pedido` ostenta este valor. Se conserva por fidelidad a la lista de estados del DOMINIO 7. |
| PENDIENTE_PAGO  | Pendiente de Pago      | Pedido creado, a la espera de confirmación financiera.                                                                                          |
| PAGADO          | Pagado                 | Pago confirmado; comienza el proceso de alistamiento y se emite la factura.                                                                     |
| DESPACHADO      | Despachado             | El primer envío del pedido ha salido físicamente de la bodega.                                                                                  |
| ENTREGADO       | Entregado / Finalizado | Todos los envíos han sido entregados; el pedido queda cerrado y ya no puede modificarse.                                                        |
| CANCELADO       | Cancelado              | Pedido terminado sin completar la entrega. Inferido; ver la nota de fuente arriba.                                                              |

## Ciclo de Vida

```text
CARRITO                   (etapa que ostenta la entidad Carrito)
  │
  │ confirmación del carrito
  ▼
PENDIENTE_PAGO
  │
  ├──────────────> CANCELADO
  │
  ▼
PAGADO
  │
  ├──────────────> CANCELADO
  │
  ├──────────────> ENTREGADO   (pedidos compuestos exclusivamente por líneas digitales)
  │
  ▼
DESPACHADO
  │
  ▼
ENTREGADO
```

`ENTREGADO` y `CANCELADO` son terminales. Un pedido en `ENTREGADO` ya no puede modificarse bajo ninguna circunstancia (Sección 11).

---

# EstadoPago

## Descripción

Representa el estado de un intento de pago registrado contra un pedido.

Cada intento de cobrar el monto de un pedido es su propia entidad `Pago` con su propio estado, de modo que un intento rechazado permanezca como registro histórico en lugar de ser sobrescrito por un reintento posterior.

Un pedido pasa a `PAGADO` únicamente cuando uno de sus intentos de pago alcanza `APROBADO`.

## Hereda De

`CatálogoDominio`

## Valores Permitidos

**Fuente:** DOMINIO 7 (estados "Pendiente de Pago" y "Pagado"); Sección 6.1 paso 6 — "Se valida el pago y se inicia el flujo de preparación." La especificación exige validar el pago pero no enumera los posibles resultados de esa validación, así que los valores son inferidos: una validación que solo pudiera tener éxito no sería una validación. `RECHAZADO` y `FALLIDO` se distinguen porque tienen significados de negocio diferentes — una negativa comercial es una decisión, mientras que una falla técnica es un incidente, y solo la primera dice algo del comprador.

| Código    | Nombre    | Descripción                                                                                              |
| --------- | --------- | -------------------------------------------------------------------------------------------------------- |
| PENDIENTE | Pendiente | Intento de pago registrado, a la espera de validación.                                                   |
| APROBADO  | Aprobado  | Pago validado exitosamente; el pedido pasa a `PAGADO`.                                                   |
| RECHAZADO | Rechazado | Pago rehusado durante la validación. El pedido no se ve afectado y puede registrarse un nuevo intento.   |
| FALLIDO   | Fallido   | El pago no pudo completarse por un error de procesamiento y no por una negativa.                         |

## Ciclo de Vida

```text
PENDIENTE
   │
   ├──────────────> RECHAZADO
   │
   ├──────────────> FALLIDO
   │
   ▼
APROBADO
```

`APROBADO`, `RECHAZADO` y `FALLIDO` son terminales para un intento dado. Un intento rechazado o fallido nunca modifica el pedido; se crea un `Pago` nuevo por cada reintento.

---

# EstadoEnvío

## Descripción

Representa la etapa actual de ejecución de un envío.

Un mismo pedido puede producir varios envíos cuando sus líneas físicas están almacenadas en bodegas distintas, y cada uno de ellos lleva su propio estado.

## Hereda De

`CatálogoDominio`

## Valores Permitidos

**Fuente:** OBJ-10; Sección 4.1 ("Envíos: Procesos logísticos para productos físicos"); Sección 6.1 paso 7, que describe empaque, despacho y transporte como sub-pasos secuenciales. La especificación nombra el proceso pero no enumera sus estados, así que los valores siguientes son inferidos mapeando directamente sobre esa secuencia, con la misma granularidad que `EstadoPedido` usa para sus propias etapas de despacho y entrega.

| Código      | Nombre       | Descripción                                                                                     |
| ----------- | ------------ | ----------------------------------------------------------------------------------------------- |
| PENDIENTE   | Pendiente    | Envío creado y en proceso de empaque; todavía no ha salido de la bodega.                        |
| EN_TRÁNSITO | En Tránsito  | Envío despachado desde la bodega y en ruta hacia el comprador.                                  |
| ENTREGADO   | Entregado    | Entrega confirmada. Cuando todos los envíos de un pedido alcanzan este estado, el pedido pasa a `ENTREGADO`. |

## Ciclo de Vida

```text
PENDIENTE
   │
   │ despacho  (genera un MovimientoInventario de tipo SALIDA_VENTA)
   ▼
EN_TRÁNSITO
   │
   │ confirmación de entrega
   ▼
ENTREGADO
```

---

# EstadoDevolución

## Descripción

Representa la etapa actual de una solicitud de devolución.

La devolución concierne al producto que se retorna; el movimiento de dinero que sigue a una devolución aprobada está representado por separado en `EstadoReembolso`.

## Hereda De

`CatálogoDominio`

## Valores Permitidos

**Fuente:** OBJ-11, que nombra "devoluciones" como un proceso que requiere administración; Matriz de Responsabilidades, donde el comprador inicia el proceso. La especificación no enumera los estados, así que los valores siguientes son inferidos siguiendo el patrón solicitud → decisión → cierre que la propia especificación usa para los flujos guiados por aprobación, y que la referencia bancaria usa para sus propios productos basados en solicitud.

| Código     | Nombre     | Descripción                                                                                     |
| ---------- | ---------- | ----------------------------------------------------------------------------------------------- |
| SOLICITADA | Solicitada | Solicitud de devolución creada por el comprador, a la espera de revisión.                       |
| APROBADA   | Aprobada   | Devolución aceptada; origina un `Reembolso` y, para items físicos, un retorno de existencias.   |
| RECHAZADA  | Rechazada  | Solicitud de devolución denegada. No se origina ningún reembolso.                               |
| COMPLETADA | Completada | Producto devuelto recibido y procesado; la solicitud queda cerrada.                             |

## Ciclo de Vida

```text
SOLICITADA
    │
    ├──────────────> RECHAZADA
    │
    ▼
 APROBADA
    │
    │ se crea el Reembolso + MovimientoInventario de tipo DEVOLUCIÓN
    ▼
COMPLETADA
```

`RECHAZADA` y `COMPLETADA` son terminales.

---

# EstadoReembolso

## Descripción

Representa la etapa actual de un reembolso monetario.

`Reembolso` es la contraparte financiera de `Pago`: uno registra dinero saliendo del marketplace, el otro dinero entrando en él.

## Hereda De

`CatálogoDominio`

## Valores Permitidos

**Fuente:** OBJ-11, que nombra "reembolsos" como un proceso administrado por separado de las devoluciones; Matriz de Responsabilidades ("Gestión Reembolsos → Admin ejecuta"). La especificación no enumera los estados, así que los valores siguientes son inferidos, siguiendo el mismo patrón de ejecución que `EstadoPago` en dirección opuesta.

| Código    | Nombre    | Descripción                                                            |
| --------- | --------- | ---------------------------------------------------------------------- |
| PENDIENTE | Pendiente | Reembolso originado por una devolución aprobada, aún no ejecutado.     |
| PROCESADO | Procesado | Fondos restituidos al comprador por un administrador.                  |
| RECHAZADO | Rechazado | Reembolso denegado durante la revisión administrativa.                 |

## Ciclo de Vida

```text
PENDIENTE
   │
   ├──────────────> RECHAZADO
   │
   ▼
PROCESADO
```

---

# TipoOperación

## Descripción

Representa el tipo de operación de negocio significativa ejecutada dentro del marketplace.

Las operaciones representan eventos o acciones de negocio ejecutadas sobre las entidades del dominio. Toda acción de negocio significativa debe referenciar un `TipoOperación`, y toda operación debe quedar registrada en el `RegistroAuditoría`.

Las operaciones son independientes de los estados de las entidades:

* Un **estado** representa la situación actual de una entidad.
* Una **operación** representa una acción o evento que ocurrió.

Por ejemplo:

```text
Pedido.estadoPedido = PAGADO
```

representa el estado actual del pedido, mientras que:

```text
Operación.tipoOperación = CONFIRMACIÓN_PAGO_PEDIDO
```

representa el evento que causó que el pedido quedara pagado.

## Hereda De

`CatálogoDominio`

## Valores Permitidos

**Fuente:** Derivados de los eventos de negocio identificados a lo largo de la especificación — incorporación de usuarios y vendedores (DOMINIO 1, DOMINIO 3, Sección 6.1 paso 1), administración de bodegas (DOMINIO 4), publicación del catálogo (DOMINIO 5, Sección 6.1 pasos 2 y 4), movimientos de inventario (DOMINIO 6), ciclo de vida del carrito y del pedido (DOMINIO 7, OBJ-07, OBJ-08, Sección 6.1 pasos 5–8), facturación (OBJ-09), logística (OBJ-10) y el flujo de devolución/reembolso (OBJ-11) — de modo que el `RegistroAuditoría` cubra el alcance completo de trazabilidad exigido por el OBJ-12 y por el compromiso declarado en la Sección 1.

Los valores están agrupados por el área de negocio a la que pertenecen. Cada valor corresponde a una acción listada en los *Ejemplos de Operaciones Generadas* de las entidades del *Modelo de Dominio*.

### Operaciones de Usuario

| Código                | Nombre                 | Descripción                                                                    |
| --------------------- | ---------------------- | ------------------------------------------------------------------------------ |
| REGISTRO_USUARIO      | Registro de Usuario    | Se registró un usuario en la plataforma.                                       |
| CAMBIO_ESTADO_USUARIO | Cambio Estado Usuario  | Se modificó el estado operativo de un usuario.                                 |
| REGISTRO_COMPRADOR    | Registro de Comprador  | Se registró un comprador en la plataforma.                                     |
| REGISTRO_VENDEDOR     | Registro de Vendedor   | Un administrador incorporó un vendedor, junto con su primera bodega.           |

### Operaciones de Bodega

| Código          | Nombre              | Descripción                    |
| --------------- | ------------------- | ------------------------------ |
| REGISTRO_BODEGA | Registro de Bodega  | Se registró una bodega.        |

### Operaciones de Catálogo

| Código                   | Nombre                     | Descripción                                                    |
| ------------------------ | -------------------------- | -------------------------------------------------------------- |
| REGISTRO_PRODUCTO        | Registro de Producto       | Un vendedor registró un producto.                              |
| PUBLICACIÓN_PRODUCTO     | Publicación de Producto    | Se publicó un producto en el catálogo público.                 |
| SUSPENSIÓN_PRODUCTO      | Suspensión de Producto     | Se ocultó temporalmente un producto del catálogo.              |
| DESCONTINUACIÓN_PRODUCTO | Descontinuación de Producto | Se retiró permanentemente un producto de la venta activa.     |

### Operaciones de Inventario

| Código                  | Nombre                    | Descripción                                                     |
| ----------------------- | ------------------------- | --------------------------------------------------------------- |
| INGRESO_INVENTARIO      | Ingreso de Inventario     | Entraron existencias a una bodega.                              |
| RESERVA_INVENTARIO      | Reserva de Inventario     | Se reservaron existencias para un pedido.                       |
| SALIDA_VENTA_INVENTARIO | Salida por Venta          | Salieron existencias de una bodega por una venta confirmada.    |
| AJUSTE_INVENTARIO       | Ajuste de Inventario      | Se corrigió manualmente la cantidad de existencias.             |
| DEVOLUCIÓN_INVENTARIO   | Devolución a Inventario   | Reingresaron existencias a una bodega por una devolución aprobada. |

### Operaciones de Carrito

| Código                   | Nombre                    | Descripción                                              |
| ------------------------ | ------------------------- | -------------------------------------------------------- |
| ADICIÓN_ITEM_CARRITO     | Adición de Item al Carrito | Se añadió una variante de producto a un carrito.        |
| ELIMINACIÓN_ITEM_CARRITO | Eliminación de Item       | Se eliminó una variante de producto de un carrito.       |
| CONFIRMACIÓN_CARRITO     | Confirmación de Carrito   | Se confirmó un carrito, produciendo un pedido.           |

### Operaciones de Pedido

| Código                   | Nombre                        | Descripción                                                          |
| ------------------------ | ----------------------------- | -------------------------------------------------------------------- |
| CREACIÓN_PEDIDO          | Creación de Pedido            | Se creó un pedido a partir de un carrito confirmado.                 |
| CONFIRMACIÓN_PAGO_PEDIDO | Confirmación de Pago          | Se confirmó el pago de un pedido y el pedido quedó pagado.           |
| DESPACHO_PEDIDO          | Despacho de Pedido            | El pedido fue despachado, tras su primer envío.                      |
| ENTREGA_PEDIDO           | Entrega de Pedido             | Todos los envíos del pedido fueron entregados; el pedido quedó cerrado. |
| CANCELACIÓN_PEDIDO       | Cancelación de Pedido         | El pedido terminó sin completar la entrega.                          |

### Operaciones de Pago

| Código          | Nombre               | Descripción                                            |
| --------------- | -------------------- | ------------------------------------------------------ |
| REGISTRO_PAGO   | Registro de Pago     | Se registró un intento de pago contra un pedido.       |
| APROBACIÓN_PAGO | Aprobación de Pago   | Un intento de pago fue validado exitosamente.          |
| RECHAZO_PAGO    | Rechazo de Pago      | Un intento de pago fue rehusado o falló.               |

### Operaciones de Facturación

| Código          | Nombre               | Descripción                                        |
| --------------- | -------------------- | -------------------------------------------------- |
| EMISIÓN_FACTURA | Emisión de Factura   | Se emitió una factura para un pedido pagado.       |

### Operaciones de Envío

| Código          | Nombre               | Descripción                                                        |
| --------------- | -------------------- | ------------------------------------------------------------------ |
| CREACIÓN_ENVÍO  | Creación de Envío    | Se creó un envío para las líneas físicas de un pedido.             |
| DESPACHO_ENVÍO  | Despacho de Envío    | Un envío salió de una bodega.                                      |
| ENTREGA_ENVÍO   | Entrega de Envío     | Se confirmó la entrega de un envío.                                |

### Operaciones de Devolución y Reembolso

| Código                        | Nombre                       | Descripción                                                        |
| ----------------------------- | ---------------------------- | ------------------------------------------------------------------ |
| CREACIÓN_SOLICITUD_DEVOLUCIÓN | Creación Solicitud Devolución | Un comprador solicitó la devolución de uno o más items comprados. |
| APROBACIÓN_DEVOLUCIÓN         | Aprobación de Devolución     | Se aceptó una solicitud de devolución.                             |
| RECHAZO_DEVOLUCIÓN            | Rechazo de Devolución        | Se denegó una solicitud de devolución.                             |
| FINALIZACIÓN_DEVOLUCIÓN       | Finalización de Devolución   | Se recibió el producto devuelto y la solicitud quedó cerrada.      |
| PROCESAMIENTO_REEMBOLSO       | Procesamiento de Reembolso   | Un administrador ejecutó un reembolso.                             |
| RECHAZO_REEMBOLSO             | Rechazo de Reembolso         | Se denegó un reembolso durante la revisión administrativa.         |

---

# TipoEntidadAfectada

## Descripción

Representa el tipo de entidad de negocio afectada por una `Operación`.

**Decisión de diseño — por qué existe este catálogo.** La referencia bancaria apunta `Operación.productoAfectado` hacia `ProductoBancario`, una raíz abstracta compartida por todos sus productos. NexusMarket no tiene un concepto de negocio equivalente: los eventos significativos afectan a entidades tan distintas como `Pedido`, `Pago`, `Inventario`, `Envío`, `SolicitudDevolución`, `Reembolso`, `Producto`, `Vendedor` y `Bodega`, que no comparten ningún significado de negocio común. Introducir una raíz técnica artificial para agruparlas inventaría un concepto que la especificación no respalda, y contradiría la regla de diseño según la cual la herencia representa especialización genuina del dominio.

En su lugar, `Operación` y `RegistroAuditoría` cualifican la entidad afectada con este catálogo más el identificador de la entidad. La referencia permanece tipada y controlada por el dominio, sin una jerarquía falsa y sin las cadenas arbitrarias que las Notas de Diseño prohíben.

## Hereda De

`CatálogoDominio`

## Valores Permitidos

**Fuente:** Derivados de las entidades del *Modelo de Dominio* que generan operaciones, según lo listado en sus *Ejemplos de Operaciones Generadas*. Cada valor corresponde a una entidad o familia de entidades que un evento de negocio puede afectar.

| Código               | Nombre                | Descripción                                                                                     |
| -------------------- | --------------------- | ----------------------------------------------------------------------------------------------- |
| USUARIO              | Usuario               | Eventos de identidad y acceso sobre cualquier participante de la plataforma.                    |
| VENDEDOR             | Vendedor              | Incorporación y administración comercial de un vendedor. Se mantiene separado de `USUARIO` porque la especificación trata el registro de vendedores como su propio proceso de negocio (DOMINIO 3, OBJ-02) con su propio rol responsable. |
| BODEGA               | Bodega                | Registro y administración de una ubicación de almacenamiento.                                   |
| PRODUCTO             | Producto              | Eventos de catálogo sobre un producto y sus variantes.                                          |
| INVENTARIO           | Inventario            | Eventos de existencias sobre un registro de inventario.                                         |
| CARRITO              | Carrito               | Eventos sobre la selección provisional de un comprador.                                         |
| PEDIDO               | Pedido                | Eventos sobre el ciclo de vida de un pedido.                                                    |
| PAGO                 | Pago                  | Eventos sobre un intento de pago.                                                               |
| FACTURA              | Factura               | Eventos sobre un documento comercial de facturación.                                            |
| ENVÍO                | Envío                 | Eventos sobre el cumplimiento logístico de un pedido.                                           |
| SOLICITUD_DEVOLUCIÓN | Solicitud Devolución  | Eventos sobre una solicitud de devolución y sus líneas.                                         |
| REEMBOLSO            | Reembolso             | Eventos sobre una restitución monetaria.                                                        |

---

# Moneda

## Descripción

Representa una moneda soportada por el marketplace.

La moneda es un Objeto de Valor de negocio porque su significado está determinado por sus valores controlados y no por una identidad independiente.

**Decisión de diseño — por qué `Moneda` aplica a NexusMarket.** La especificación nunca nombra una moneda, lo cual podría sugerir que el concepto está fuera de alcance. No lo está: el marketplace intermedia entre vendedores terceros independientes y compradores, emite facturas (OBJ-09), recauda pagos (Sección 6.1 paso 6) y devuelve dinero mediante reembolsos (OBJ-11). Un `BigDecimal` sin denominación se vuelve ambiguo en el momento en que existe más de una moneda, y un monto reembolsado debe ser demostrablemente el mismo monto que se cobró — lo cual no puede verificarse si ninguno de los dos lados está denominado. La referencia bancaria establece `Moneda` como Objeto de Valor de `CatálogoDominio` exactamente por esta razón, y el mismo argumento se sostiene aquí. Modelarlo ahora cuesta un Objeto de Valor; omitirlo obligaría a cambiar después todos los atributos monetarios del modelo.

Toda entidad que mantiene un monto monetario mantiene también su moneda: `Producto`, `Pedido`, `Pago`, `Factura` y `Reembolso`.

## Hereda De

`CatálogoDominio`

## Atributos Adicionales

| Atributo   | Tipo   | Descripción                                    |
| ---------- | ------ | ---------------------------------------------- |
| códigoIso  | String | Código de moneda ISO 4217.                     |
| símbolo    | String | Símbolo de la moneda usado para visualización. |

## Valores Permitidos

**Fuente:** Inferido. La especificación no enumera monedas, así que el catálogo se siembra con los mismos valores de la referencia bancaria. Esto es deliberado y no arbitrario: el propósito del Objeto de Valor es que soportar una moneda adicional se convierta en una entrada de catálogo y no en un cambio al modelo de dominio, de modo que el conjunto inicial solo necesita ser coherente, no exhaustivo.

| Código ISO | Nombre                | Símbolo |
| ---------- | --------------------- | ------- |
| COP        | Peso Colombiano       | $       |
| USD        | Dólar Estadounidense  | $       |
| EUR        | Euro                  | €       |

---

# Enumeraciones Primitivas

Los siguientes conceptos se representarían como enumeraciones simples, porque contienen valores técnicos fijos y no requieren metadatos de catálogo de negocio como `código`, `nombre` o `descripción`.

**No se identificó ninguno a partir de la especificación de NexusMarket.**

A diferencia de la referencia bancaria, el documento fuente no da base alguna para conceptos puramente técnicos como canales de notificación o niveles de severidad de auditoría: coloca explícitamente las interfaces, los mecanismos de autenticación, las tecnologías de implementación y los detalles de almacenamiento fuera de su alcance (Sección 3.2). Todos los catálogos definidos arriba remiten a un concepto de negocio explícito de la especificación, así que todos se modelan como Objetos de Valor de `CatálogoDominio`.

Los resultados de aprobación — que la referencia bancaria modela como una enumeración primitiva `DecisiónAprobación` — tampoco se modelan aquí por separado, porque en este dominio cada flujo guiado por aprobación ya lleva su decisión dentro de su propio catálogo de negocio: `EstadoDevolución` para las devoluciones, `EstadoReembolso` para los reembolsos y `EstadoPago` para los pagos. Añadir una enumeración genérica de decisión duplicaría información que el dominio ya expresa con mayor precisión.

---

# Notas de Diseño

## Herencia

Todos los catálogos de negocio heredan de `CatálogoDominio`, lo cual garantiza que todo valor controlado lleve un `código`, un `nombre` y una `descripción`.

## Inmutabilidad

Todos los Objetos de Valor son inmutables después de su creación. Sus valores no pueden modificarse una vez que el objeto ha sido instanciado.

## Igualdad

Los Objetos de Valor se comparan según sus valores y no según su identidad de objeto. Dos instancias que contienen los mismos valores de negocio representan el mismo Objeto de Valor.

## Valores Controlados

Los catálogos de negocio deben usar valores controlados definidos por el dominio. La aplicación debe evitar reemplazar estos conceptos con cadenas arbitrarias como:

```text
"ACTIVO"
"PAGADO"
"DAÑADO"
```

a lo largo del código. En su lugar debe usarse el Objeto de Valor correspondiente:

```text
EstadoUsuario
EstadoPedido
EstadoInventario
```

## Enumeraciones de Negocio frente a Técnicas

Un concepto de negocio se modela como Objeto de Valor de `CatálogoDominio` cuando requiere un código de negocio, un nombre visible, una descripción de negocio y evolución controlada del dominio. Una enumeración simple se usa cuando el concepto representa un valor técnico fijo sin metadatos de negocio adicionales. No se identificó ningún concepto de ese tipo en esta especificación; ver la sección anterior.

## Estados frente a Operaciones

Un catálogo de **estado** representa la situación actual de una entidad — `EstadoPedido`, `EstadoPago`, `EstadoEnvío`. Un catálogo de **operación** representa una acción o evento que ocurrió — `TipoOperación`. Ambos se mantienen separados a lo largo del dominio: `Pedido.estadoPedido = PAGADO` describe dónde está el pedido ahora, mientras que `Operación.tipoOperación = CONFIRMACIÓN_PAGO_PEDIDO` describe qué ocurrió para llevarlo allí.

## Catálogos de Estado Independientes

Los estados que describen preocupaciones diferentes se modelan como catálogos separados y nunca se colapsan en uno solo: `EstadoUsuario` describe el acceso a la plataforma, mientras que `EstadoComercialComprador` describe la capacidad de compra. Un comprador puede ser un usuario activo y aun así estar comercialmente restringido.

## Relación con las Entidades

Las entidades referencian Objetos de Valor en lugar de cadenas primitivas siempre que el valor referenciado represente un concepto de negocio controlado:

```text
Usuario.rol                            : RolSistema
Usuario.estado                         : EstadoUsuario
Comprador.estadoComercial              : EstadoComercialComprador
Producto.tipoProducto                  : TipoProducto
Producto.estadoProducto                : EstadoProducto
Producto.moneda                        : Moneda
Inventario.estadoInventario            : EstadoInventario
MovimientoInventario.tipoMovimiento    : TipoMovimientoInventario
Pedido.estadoPedido                    : EstadoPedido
Pedido.moneda                          : Moneda
Pago.estadoPago                        : EstadoPago
Pago.moneda                            : Moneda
Factura.moneda                         : Moneda
Envío.estadoEnvío                      : EstadoEnvío
SolicitudDevolución.estadoDevolución   : EstadoDevolución
Reembolso.estadoReembolso              : EstadoReembolso
Reembolso.moneda                       : Moneda
Operación.tipoOperación                : TipoOperación
Operación.tipoEntidadAfectada          : TipoEntidadAfectada
RegistroAuditoría.tipoOperación        : TipoOperación
RegistroAuditoría.rolUsuario           : RolSistema
RegistroAuditoría.tipoEntidadAfectada  : TipoEntidadAfectada
```

## Trazabilidad hacia la Especificación

Cada catálogo declara la sección de la *Especificación Funcional del Negocio* de la cual provienen sus valores, y todo catálogo cuyos valores no estén completamente enumerados en la especificación declara su justificación de inferencia directamente en su propia sección. Esta es una adición deliberada frente a la referencia bancaria: el razonamiento que produjo el catálogo viaja con el artefacto en lugar de requerir una lista de revisión aparte.

## Resultado

Este enfoque mejora la seguridad de tipos, la expresividad del dominio, la mantenibilidad y la consistencia con los principios de Diseño Guiado por el Dominio, a la vez que soporta la evolución futura del dominio: añadir una moneda, un estado o un tipo de operación es una entrada de catálogo, no un cambio al modelo.
