# Modelo de Dominio

## Introducción

El Modelo de Dominio representa las entidades de negocio centrales de la plataforma de marketplace NexusMarket. Estas entidades encapsulan las reglas de negocio, los datos, las relaciones y los conceptos de ciclo de vida descritos en la *Especificación Funcional del Negocio - NexusMarket*.

El modelo sigue los principios de Diseño Orientado a Objetos y de Diseño Guiado por el Dominio (DDD). La herencia se usa para representar especialización genuina del dominio, mientras que se prefieren las relaciones explícitas entre objetos por encima de los campos identificadores genéricos.

El modelo distingue entre:

* **Usuarios**, que representan a las personas autorizadas para interactuar con la plataforma y el único rol que cada una desempeña dentro de ella.
* **Bodegas**, que representan las ubicaciones físicas donde se mantiene el inventario.
* **Productos y Variantes de Producto**, que representan los bienes ofrecidos en el catálogo y las unidades vendibles concretas derivadas de ellos.
* **Inventario**, que representa las existencias distribuidas de cada unidad vendible en cada bodega.
* **Documentos comerciales**, que representan el recorrido de compra del comprador: `Carrito`, `Pedido`, `Pago` y `Factura`.
* **Entidades de cumplimiento y posventa**, que representan `Envío`, `SolicitudDevolución` y `Reembolso`.
* **Operaciones**, que representan las acciones de negocio significativas ejecutadas dentro del marketplace.
* **Registros de Auditoría**, que proveen el histórico inmutable de las operaciones.

Una entidad de negocio puede generar múltiples operaciones a lo largo de su ciclo de vida. Toda operación de negocio significativa debe quedar registrada en la traza de auditoría.

Donde la especificación nombra un concepto de negocio pero no enumera su conjunto completo de atributos, los atributos faltantes se han inferido a partir del contexto del negocio — el flujo operativo, los objetivos funcionales y las validaciones críticas — de la misma forma que en el modelo de referencia bancario. Cada entidad declara su **Fuente** en la especificación, y cada atributo inferido declara su justificación en línea, de modo que la trazabilidad viaje dentro del documento y no en una lista de revisión aparte.

---

# Jerarquía de Clases del Dominio

```text
Usuario (Abstracta)
├── Comprador
├── Vendedor
├── OperadorLogístico
├── Administrador
└── Supervisor

Bodega (Abstracta)
├── BodegaMarketplace
└── BodegaVendedor

Producto (Abstracta)
├── ProductoFísico
└── ProductoDigital

VarianteProducto

Inventario
MovimientoInventario

Carrito
ItemCarrito

Pedido
ItemPedido
Pago
Factura

Envío

SolicitudDevolución
ItemDevolución
Reembolso

Operación

RegistroAuditoría
```

---

# Relaciones del Dominio

```text
Usuario
   ├── Comprador
   │      ├── posee (exactamente uno activo) ──> Carrito
   │      ├── realiza ──────────────────────────> Pedido
   │      ├── es facturado en ──────────────────> Factura
   │      └── solicita ─────────────────────────> SolicitudDevolución
   │
   ├── Vendedor
   │      ├── posee (al menos una) ─────────────> BodegaVendedor
   │      └── registra ─────────────────────────> Producto
   │
   ├── OperadorLogístico
   │      └── es responsable de ────────────────> Envío
   │
   ├── Administrador
   │      ├── incorpora ────────────────────────> Vendedor
   │      ├── administra ───────────────────────> Bodega
   │      └── procesa ──────────────────────────> Reembolso
   │
   └── Supervisor
          └── consulta ─────────────────────────> Operación / RegistroAuditoría

Bodega
   ├── BodegaMarketplace
   └── BodegaVendedor
          └── propietario ───────────────────────> Vendedor

Producto
   ├── ProductoFísico
   ├── ProductoDigital
   ├── vendedor ─────────────────────────────────> Vendedor
   └── tiene (al menos una) ─────────────────────> VarianteProducto
                                                        │
                                                        └── almacenada como ──> Inventario
                                                                                   ├── bodega ─────> Bodega
                                                                                   └── alterado por > MovimientoInventario
                                                                                                          └── ejecutadoPor ──> Usuario

Carrito
   ├── comprador ────────────────────────────────> Comprador
   ├── contiene ─────────────────────────────────> ItemCarrito
   │                                                    └── variante ──> VarianteProducto
   └── confirmado en ────────────────────────────> Pedido

Pedido
   ├── comprador ────────────────────────────────> Comprador
   ├── contiene ─────────────────────────────────> ItemPedido
   │                                                    └── variante ──> VarianteProducto
   ├── saldado por ──────────────────────────────> Pago
   ├── facturado por ────────────────────────────> Factura
   ├── cumplido por ─────────────────────────────> Envío
   │                                                    ├── bodegaOrigen ───────> Bodega
   │                                                    ├── operadorLogístico ──> OperadorLogístico
   │                                                    └── items ──────────────> ItemPedido
   └── disputado por ────────────────────────────> SolicitudDevolución
                                                         ├── contiene ───> ItemDevolución
                                                         │                     └── itemPedido ──> ItemPedido
                                                         └── saldada por > Reembolso
                                                                               └── procesadoPor ──> Administrador

Cualquier entidad de negocio
   │
   │ acción de negocio significativa
   ▼
Operación
   ├── ejecutadoPor ─────────────────────────────> Usuario
   └── registrada en ────────────────────────────> RegistroAuditoría
```

---

# Entidades

---

# Usuario (Abstracta)

## Descripción

Representa a cualquier participante autorizado para interactuar con la plataforma NexusMarket.

Esta clase abstracta centraliza la información de identificación, contacto y acceso que comparten todos los roles del sistema: Comprador, Vendedor, Operador Logístico, Administrador y Supervisor.

Cada participante desempeña exactamente un rol dentro del sistema, y ese rol determina las responsabilidades y capacidades de negocio asociadas al participante.

**Decisión de diseño — jerarquía de un solo nivel frente a la referencia bancaria:** la referencia bancaria divide la identidad en tres niveles (`Persona` → `Cliente` → `Usuario`) porque allí un cliente empresarial puede tener varios usuarios operativos distintos que referencian a la misma compañía. NexusMarket no tiene un caso equivalente en su especificación — cada comprador, vendedor o empleado corresponde a exactamente una cuenta — de modo que una única jerarquía `Usuario` con subclases por rol es suficiente y evita introducir una distinción que las reglas de negocio no requieren.

**Decisión de diseño — documento de identidad frente a identificador interno:** el DOMINIO 1 lista un único "Identificador" ambiguo, mientras que la Sección 11 exige que **tanto** el documento de identidad **como** el correo electrónico sean únicos en la plataforma. Un solo campo no puede cargar ambas responsabilidades, así que el modelo los separa explícitamente, reflejando la referencia bancaria (`Persona.identificación` para el documento nacional de identidad, `Usuario.idUsuario` para el identificador interno del sistema).

Esta clase no puede instanciarse directamente.

**Fuente:** DOMINIO 1. Administración de Usuarios; Sección 5. Participantes del Negocio; Sección 11. Validaciones Críticas; RG-01, RG-02, RG-03.

## Atributos

| Atributo               | Tipo        | Descripción                                                                                                                                                                                                                                                                        |
| ---------------------- | ----------- | ---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| idUsuario              | String      | Identificador único interno del usuario dentro de la plataforma. Inferido: separado de `númeroIdentificación` porque el documento de identidad es un dato de negocio que pertenece a la persona, mientras que este identificador pertenece al sistema.                              |
| númeroIdentificación   | String      | Número del documento nacional de identidad del participante. Único en la plataforma. Inferido de la Sección 11 ("El documento de identidad y correo electrónico deben ser únicos en la plataforma"), que exige este dato aunque el DOMINIO 1 no lo liste por separado.               |
| nombreCompleto         | String      | Nombre oficial completo del usuario.                                                                                                                                                                                                                                               |
| correoElectrónico      | String      | Medio principal de acceso y comunicación. Único en la plataforma.                                                                                                                                                                                                                  |
| rol                    | RolSistema  | Rol de negocio que define las responsabilidades y permisos del usuario. Exactamente uno por usuario.                                                                                                                                                                                |
| estado                 | EstadoUsuario | Condición operativa actual del usuario, por ejemplo Activo o Bloqueado.                                                                                                                                                                                                          |

## Relaciones

* Un `Usuario` se especializa como exactamente uno de `Comprador`, `Vendedor`, `OperadorLogístico`, `Administrador` o `Supervisor`.
* Un `Usuario` puede ejecutar cero o más instancias de `Operación`.
* Cada `Operación` registra el `Usuario` responsable de ella, y cada `RegistroAuditoría` registra además el `RolSistema` que ostentaba en el momento de la ejecución.
* El `rol` pertenece a `Usuario` porque representa el significado y las responsabilidades del participante dentro del marketplace, y es lo que determina qué especialización aplica.

## Reglas de Negocio

```text
Toda operación debe ser ejecutada por un usuario autenticado.         (RG-01)
Cada usuario ostenta exactamente un rol dentro del sistema.           (RG-02)
Ningún participante puede administrar información fuera de su rol.    (RG-03)
númeroIdentificación debe ser único en la plataforma.                 (Sección 11)
correoElectrónico debe ser único en la plataforma.                    (Sección 11)
```

## Ejemplos de Operaciones Generadas

* `REGISTRO_USUARIO`
* `CAMBIO_ESTADO_USUARIO`

---

# Comprador

## Descripción

Representa a un cliente registrado que compra productos publicados en el marketplace.

El comprador mantiene las direcciones de entrega usadas para cumplir sus pedidos y un estado comercial que determina si actualmente puede realizar compras.

Un comprador nunca administra información perteneciente a otros compradores ni al inventario, lo cual es una restricción explícita de la especificación.

**Fuente:** DOMINIO 2. Gestión de Compradores; OBJ-03; Matriz de Responsabilidades.

## Hereda De

`Usuario`

## Atributos

| Atributo                | Tipo                       | Descripción                                                                                                                                                                                                                                             |
| ----------------------- | -------------------------- | --------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| direcciónPrincipal      | String                     | Ubicación habitual de entrega del comprador.                                                                                                                                                                                                            |
| direccionesAdicionales  | List\<String\>             | Ubicaciones secundarias de entrega. Opcional; vacía por defecto.                                                                                                                                                                                        |
| estadoComercial         | EstadoComercialComprador   | Condición del comprador para realizar compras.                                                                                                                                                                                                          |
| carritoActivo           | Carrito?                   | El único carrito abierto del comprador. Inferido: el OBJ-07 trata el carrito como un objeto de negocio administrado, y un comprador debe poder retomar una selección ya iniciada, lo cual exige una referencia estable a ella.                          |
| pedidos                 | List\<Pedido\>             | Pedidos realizados por el comprador. Vacía por defecto. Inferido, reflejando la referencia bancaria: no se puebla por defecto, se carga bajo demanda por el servicio de consulta correspondiente.                                                        |

## Relaciones

* Un `Comprador` posee exactamente un `Carrito` activo en cualquier momento dado.
* Un `Comprador` realiza cero o más instancias de `Pedido`.
* Un `Comprador` es la parte facturada de cero o más instancias de `Factura`, siempre a través de un `Pedido`.
* Un `Comprador` solicita cero o más instancias de `SolicitudDevolución`.
* Un `Comprador` es el receptor de cero o más instancias de `Reembolso`, siempre a través de una `SolicitudDevolución`.
* `pedidos` no se puebla por defecto; se carga bajo demanda.

## Reglas de Negocio

```text
Un comprador tiene exactamente un Carrito activo en cualquier momento.

Un comprador cuyo estadoComercial no sea HABILITADO no puede
confirmar un Pedido.

Un comprador solo puede acceder a los pedidos, carritos, facturas,
devoluciones y reembolsos que le pertenecen.                          (DOMINIO 2, RG-03)
```

## Ejemplos de Operaciones Generadas

* `REGISTRO_COMPRADOR`
* `CREACIÓN_PEDIDO`
* `CREACIÓN_SOLICITUD_DEVOLUCIÓN`

---

# Vendedor

## Descripción

Representa a un proveedor responsable de registrar y administrar sus propios productos.

Los vendedores no pueden auto-registrarse; son incorporados exclusivamente por un `Administrador`, junto con su primera bodega.

Modelado con identidad comercial propia, por analogía con `ClienteEmpresarial` en la referencia bancaria: un vendedor del marketplace vende a terceros y debe ser identificable como entidad fiscal y comercial — para facturación, para efectos tributarios y para la vitrina pública — aunque la especificación no detalle explícitamente este conjunto de atributos y solo declare la regla de incorporación.

**Fuente:** DOMINIO 3. Gestión de Vendedores; OBJ-02; Sección 6.1 paso 1; Matriz de Responsabilidades ("Registro Vendedores → Admin", "Registro Productos → Vendedor"). Atributos de identidad comercial inferidos por analogía con `ClienteEmpresarial` de la referencia bancaria.

## Hereda De

`Usuario`

## Atributos

| Atributo         | Tipo                        | Descripción                                                                                                                                                     |
| ---------------- | --------------------------- | ----------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| razónSocial      | String                      | Nombre legal o registrado del negocio del vendedor. Inferido, por analogía con `ClienteEmpresarial.razónSocial` de la referencia bancaria.                       |
| nit              | String                      | Número de identificación tributaria (NIT/RUT o equivalente). Inferido: requerido para emitir una `Factura` y para operar legalmente como entidad comercial.      |
| nombreComercial  | String                      | Nombre de vitrina, visible al público, mostrado a los compradores en el catálogo. Opcional; puede diferir de la razón social.                                    |
| bodegas          | List\<BodegaVendedor\>      | Bodegas propiedad del vendedor. Contiene al menos un elemento desde el momento de la incorporación.                                                              |
| productos        | List\<Producto\>            | Productos registrados por el vendedor. Vacía por defecto. Inferido, reflejando la referencia bancaria: se carga bajo demanda por el servicio de consulta.        |

## Relaciones

* Un `Vendedor` posee una o más instancias de `BodegaVendedor`.
* Un `Vendedor` registra cero o más instancias de `Producto`.
* Un `Vendedor` es incorporado por un `Administrador` y no puede ser creado por ningún otro rol.
* Un `Vendedor` participa indirectamente en instancias de `Pedido`, a través de los productos vendidos.
* Un `Vendedor` puede registrar instancias de `MovimientoInventario` sobre el inventario de sus propios productos.

## Reglas de Negocio

```text
Un Vendedor siempre es creado por un Administrador; el auto-registro
no está permitido.                                                    (DOMINIO 3)

Todo Vendedor posee al menos una BodegaVendedor, creada durante la
incorporación junto con el registro del vendedor.                     (Sección 6.1, paso 1)

Un Vendedor solo puede administrar sus propios productos y su propio
inventario.                                                           (RG-03)
```

## Ejemplos de Operaciones Generadas

* `REGISTRO_VENDEDOR`
* `REGISTRO_PRODUCTO`
* `PUBLICACIÓN_PRODUCTO`
* `AJUSTE_INVENTARIO`

---

# OperadorLogístico

## Descripción

Representa al participante responsable de la operación física de las bodegas y los despachos.

El operador logístico ejecuta las actividades de empaque, despacho y transporte del flujo de cumplimiento, y comparte con el vendedor las responsabilidades de administración del inventario.

**Fuente:** Sección 5. Participantes del Negocio; OBJ-10; Sección 6.1 paso 7; Matriz de Responsabilidades ("Administración Inventario", "Gestión de Pedidos").

## Hereda De

`Usuario`

## Atributos

Sin atributos adicionales más allá de los heredados de `Usuario`.

## Relaciones

* Un `OperadorLogístico` es responsable de cero o más instancias de `Envío`.
* Un `OperadorLogístico` puede registrar cero o más instancias de `MovimientoInventario`.
* Un `OperadorLogístico` opera sobre instancias de `Bodega` pero no es su propietario.

## Ejemplos de Operaciones Generadas

* `DESPACHO_ENVÍO`
* `ENTREGA_ENVÍO`
* `INGRESO_INVENTARIO`
* `AJUSTE_INVENTARIO`

---

# Administrador

## Descripción

Representa al participante responsable de administrar vendedores y bodegas, incluida la incorporación de vendedores, y de ejecutar los reembolsos.

**Fuente:** Sección 5. Participantes del Negocio; DOMINIO 3; DOMINIO 4; Matriz de Responsabilidades ("Registro Vendedores → Admin", "Gestión Reembolsos → Admin").

## Hereda De

`Usuario`

## Atributos

Sin atributos adicionales más allá de los heredados de `Usuario`.

## Relaciones

* Un `Administrador` incorpora cero o más instancias de `Vendedor`.
* Un `Administrador` administra cero o más instancias de `Bodega`.
* Un `Administrador` procesa cero o más instancias de `Reembolso`.

## Reglas de Negocio

```text
Solo un Administrador puede registrar un Vendedor.                    (DOMINIO 3)
Solo un Administrador puede procesar un Reembolso.                    (Matriz de Responsabilidades)
```

## Ejemplos de Operaciones Generadas

* `REGISTRO_VENDEDOR`
* `REGISTRO_BODEGA`
* `PROCESAMIENTO_REEMBOLSO`
* `CAMBIO_ESTADO_USUARIO`

---

# Supervisor

## Descripción

Representa un perfil de consulta y seguimiento operativo.

El supervisor observa la operación pero no modifica información de negocio, lo cual convierte a los registros de `Operación` y `RegistroAuditoría` en su principal material de trabajo.

**Fuente:** Sección 5. Participantes del Negocio ("Perfil de consulta y seguimiento operativo"); OBJ-12.

## Hereda De

`Usuario`

## Atributos

Sin atributos adicionales más allá de los heredados de `Usuario`.

## Relaciones

* Un `Supervisor` consulta registros de `Operación` y de `RegistroAuditoría`.
* Un `Supervisor` no posee, modifica ni crea entidades comerciales.

## Reglas de Negocio

```text
Un Supervisor mantiene acceso de solo lectura sobre la información
operativa del marketplace.                                            (Sección 5, RG-03)
```

---

# Bodega (Abstracta)

## Descripción

Representa una ubicación física de almacenamiento usada para administrar inventario.

La especificación distingue entre bodegas propiedad del marketplace y bodegas propiedad de los vendedores. El inventario siempre está ligado a una bodega específica, lo cual convierte a las bodegas en lugares individualmente identificables y direccionables.

Esta clase no puede instanciarse directamente.

**Fuente:** DOMINIO 4. Gestión de Bodegas; OBJ-04; DOMINIO 6 ("vinculado obligatoriamente a un producto y una bodega específica").

## Atributos

| Atributo      | Tipo   | Descripción                                                                                                                                                                                                                                                       |
| ------------- | ------ | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| identificador | String | Identifica de forma única a la bodega.                                                                                                                                                                                                                            |
| dirección     | String | Ubicación física de la bodega. Inferido: una ubicación de almacenamiento carece de sentido sin una localización física, y el DOMINIO 6 exige que el inventario esté ligado a "una bodega específica", lo cual implica que las bodegas son lugares direccionables.  |

## Relaciones

* Una `Bodega` mantiene cero o más registros de `Inventario`.
* Una `Bodega` puede ser el origen de cero o más instancias de `Envío`.
* Una `Bodega` es administrada por un `Administrador` y operada por un `OperadorLogístico`.

## Reglas de Negocio

```text
El Inventario siempre está vinculado a exactamente una Bodega.        (DOMINIO 6)
```

## Ejemplos de Operaciones Generadas

* `REGISTRO_BODEGA`

---

# BodegaMarketplace

## Descripción

Representa una bodega propiedad de NexusMarket y operada directamente por él.

**Fuente:** DOMINIO 4 — "Clasificación: Se distinguen bodegas del Marketplace y bodegas de Vendedores."

## Hereda De

`Bodega`

## Atributos

Sin atributos adicionales más allá de los heredados de `Bodega`.

## Relaciones

* Una `BodegaMarketplace` no tiene un `Vendedor` propietario; pertenece al marketplace mismo.
* Una `BodegaMarketplace` puede mantener registros de `Inventario` de variantes pertenecientes a cualquier `Vendedor`.

---

# BodegaVendedor

## Descripción

Representa una bodega propiedad de un vendedor específico.

Todo vendedor se registra junto con su primera bodega durante el flujo de incorporación, lo cual hace que la relación de propiedad sea obligatoria en ambas direcciones.

**Fuente:** DOMINIO 4 — "Clasificación: bodegas de Vendedores"; Sección 6.1 paso 1 — "El Administrador registra al vendedor y su primera bodega."

## Hereda De

`Bodega`

## Atributos

| Atributo     | Tipo     | Descripción                             |
| ------------ | -------- | --------------------------------------- |
| propietario  | Vendedor | Vendedor propietario de esta bodega.    |

## Relaciones

* Una `BodegaVendedor` pertenece a exactamente un `Vendedor`.
* Un `Vendedor` posee al menos una `BodegaVendedor`.

## Reglas de Negocio

```text
Una BodegaVendedor siempre tiene exactamente un Vendedor propietario.
Todo Vendedor posee al menos una BodegaVendedor.                      (Sección 6.1, paso 1)
```

---

# Producto (Abstracta)

## Descripción

Representa un bien ofrecido a la venta en el catálogo del marketplace.

El catálogo diferencia entre productos físicos, que requieren inventario y despacho, y productos digitales, que se entregan inmediatamente tras el pago.

Un producto es un **concepto de catálogo**: carga la descripción comercial que los compradores exploran. No es en sí mismo la unidad vendible — ese papel corresponde a `VarianteProducto`, que es lo que el inventario, los carritos y los pedidos realmente referencian.

Esta clase no puede instanciarse directamente.

**Fuente:** DOMINIO 5. Gestión del Catálogo; OBJ-05; Sección 6.1 pasos 2 y 4; Matriz de Responsabilidades ("Registro Productos → Vendedor").

## Atributos

| Atributo        | Tipo                       | Descripción                                                                                                                                                                                                                                                     |
| --------------- | -------------------------- | ----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| identificador   | String                     | Identifica de forma única al producto.                                                                                                                                                                                                                          |
| nombre          | String                     | Nombre comercial del producto. Inferido: un catálogo no puede explorarse ni publicarse sin un nombre visible.                                                                                                                                                    |
| descripción     | String                     | Texto descriptivo del producto. Inferido: atributo estándar de catálogo, implicado por el DOMINIO 5 junto con la Sección 6.1 paso 4 — "Los productos se hacen visibles en el catálogo público."                                                                  |
| tipoProducto    | TipoProducto               | Físico o Digital. Listado explícitamente como atributo del producto en el DOMINIO 5, y conservado como atributo además de la distinción por subclase para que el catálogo pueda filtrarse y validarse sin depender del tipo en tiempo de ejecución.              |
| vendedor        | Vendedor                   | Vendedor que registró y es dueño del producto. Directamente implicado por la Matriz de Responsabilidades ("Registro Productos → Vendedor").                                                                                                                      |
| precio          | BigDecimal                 | Precio de venta del producto. Inferido: un catálogo comercial y un flujo de pago (Sección 6.1 pasos 5–6) no pueden funcionar sin un precio.                                                                                                                      |
| moneda          | Moneda                     | Moneda en la que se expresa el `precio`. Inferido: ver la justificación de moneda en **Reglas de Diseño del Dominio**.                                                                                                                                           |
| variantes       | List\<VarianteProducto\>   | Variaciones vendibles del producto: color, talla, modelo, etc. Contiene al menos un elemento.                                                                                                                                                                    |
| estadoProducto  | EstadoProducto             | Publicado, Suspendido o Descontinuado.                                                                                                                                                                                                                          |

## Relaciones

* Un `Producto` pertenece a exactamente un `Vendedor`.
* Un `Producto` tiene una o más instancias de `VarianteProducto`.
* Un `Producto` nunca es referenciado directamente por `Inventario`, `ItemCarrito` ni `ItemPedido`; esas entidades referencian una `VarianteProducto`.
* Un `Producto` se especializa como `ProductoFísico` o como `ProductoDigital`, y esa especialización determina si participa en `Inventario` y en `Envío`.

## Reglas de Negocio

```text
Todo Producto tiene al menos una VarianteProducto. Un producto sin
variación real se registra con una única variante por defecto, de
modo que las existencias, las líneas de carrito y las líneas de
pedido siempre referencien una variante.

Todas las variantes de un Producto comparten el precio del Producto.
La especificación no establece diferencias de precio entre variantes,
así que no se modela un precio a nivel de variante.

Solo el Vendedor propietario puede modificar un Producto.             (RG-03)

Solo un Producto cuyo estadoProducto sea PUBLICADO es visible en el
catálogo público y puede añadirse a un Carrito.                       (DOMINIO 5, Sección 6.1 paso 4)
```

## Ejemplos de Operaciones Generadas

* `REGISTRO_PRODUCTO`
* `PUBLICACIÓN_PRODUCTO`
* `SUSPENSIÓN_PRODUCTO`
* `DESCONTINUACIÓN_PRODUCTO`

---

# ProductoFísico

## Descripción

Representa un producto tangible que requiere control de inventario y despacho físico a través de una bodega.

**Fuente:** DOMINIO 5 — "productos físicos (requieren inventario y despacho)."

## Hereda De

`Producto`

## Atributos

Sin atributos adicionales. La distinción frente a `ProductoDigital` es de comportamiento: solo las variantes de un `ProductoFísico` participan en `Inventario`, y solo sus líneas de pedido participan en `Envío`.

## Relaciones

* Las instancias de `VarianteProducto` de un `ProductoFísico` tienen registros de `Inventario` en una o más instancias de `Bodega`.
* Un `ItemPedido` que referencia una variante de un `ProductoFísico` se cumple a través de un `Envío`.

## Reglas de Negocio

```text
Toda VarianteProducto de un ProductoFísico debe tener al menos un
registro de Inventario antes de que el producto pueda publicarse.     (DOMINIO 6, Sección 6.1 paso 3)
```

---

# ProductoDigital

## Descripción

Representa un producto intangible entregado inmediatamente tras la confirmación del pago, sin intervención de bodega ni de envío.

**Fuente:** DOMINIO 5 — "productos digitales (entrega inmediata tras pago)."

## Hereda De

`Producto`

## Atributos

Sin atributos adicionales. La especificación declara únicamente la regla de temporalidad de la entrega — inmediata, tras el pago — y no un mecanismo de entrega, así que aquí no se modela ninguno, para mantener la entidad estrictamente alineada con lo que está declarado.

## Relaciones

* Un `ProductoDigital` y sus variantes nunca participan en `Inventario`.
* Un `ItemPedido` que referencia una variante de un `ProductoDigital` nunca participa en un `Envío`.

## Reglas de Negocio

```text
Un ProductoDigital nunca genera registros de Inventario.              (DOMINIO 5, DOMINIO 6)
Un ProductoDigital nunca genera registros de MovimientoInventario.
Un ProductoDigital nunca participa en un Envío.
Un ItemPedido que referencia una variante de ProductoDigital se
considera entregado en cuanto el Pedido alcanza PAGADO.               (DOMINIO 5)
```

---

# VarianteProducto

## Descripción

Representa una variación vendible específica de un producto, como un color, una talla o un modelo.

La variante es la unidad que el negocio realmente vende y cuenta. Las existencias se mantienen por variante, y las líneas de carrito y de pedido registran la variante exacta seleccionada por el comprador.

**Decisión de diseño — entidad, no objeto de valor:** una variante tiene identidad propia y código comercial propio (SKU), es referenciada de forma independiente por `Inventario`, `ItemCarrito` e `ItemPedido`, y es el nivel al cual se cuentan las existencias. Modelarla como un atributo de `Producto` haría imposible controlar el stock de una combinación concreta como "rojo, talla M", que es lo que exige el inventario distribuido del DOMINIO 6.

**Decisión de diseño — clave/valor genérico frente a campos fijos:** campos fijos como `color` o `talla` obligarían a cambiar el modelo cada vez que aparezca una nueva dimensión de variante, por ejemplo "material" o "capacidad", mientras que un par genérico `nombreAtributo`/`valorAtributo` cubre cualquier tipo de variante implicado por el "etc." de la especificación sin alterar el modelo.

**Fuente:** DOMINIO 5 — "Variantes: Diferencias de color, talla, modelo, etc."; DOMINIO 6 (inventario distribuido ligado a un producto y una bodega específicos).

## Atributos

| Atributo        | Tipo     | Descripción                                                                                                                                                                                                                            |
| --------------- | -------- | ---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| idVariante      | String   | Identificador único interno de la variante. Inferido: requerido porque la variante es referenciada de forma independiente por `Inventario`, `ItemCarrito` e `ItemPedido`.                                                               |
| sku             | String   | Código comercial único de la unidad vendible (Stock Keeping Unit). Inferido: un inventario distribuido operado por personal de bodega requiere un código de negocio estable para identificar qué se cuenta, se recibe y se despacha.    |
| producto        | Producto | Producto al que pertenece esta variante.                                                                                                                                                                                               |
| nombreAtributo  | String   | Nombre de la característica que varía, por ejemplo "Color" o "Talla". Inferido: una lista de variantes necesita una estructura clave/valor para ser utilizable; esta es la forma mínima que representa "color, talla, modelo, etc."     |
| valorAtributo   | String   | Valor de esa característica, por ejemplo "Rojo" o "M".                                                                                                                                                                                 |

## Relaciones

* Una `VarianteProducto` pertenece a exactamente un `Producto`.
* Una `VarianteProducto` tiene cero o más registros de `Inventario`, uno por cada `Bodega` que la almacene. Las variantes de un `ProductoDigital` no tienen ninguno.
* Una `VarianteProducto` puede ser referenciada por cero o más instancias de `ItemCarrito`.
* Una `VarianteProducto` puede ser referenciada por cero o más instancias de `ItemPedido`.

## Reglas de Negocio

```text
sku es único en toda la plataforma.

Un Producto sin variación real se registra con una única variante por
defecto, de modo que todo Inventario, ItemCarrito e ItemPedido siempre
referencie una variante y no exista ninguna ruta opcional
producto/variante.

Las variantes de un ProductoDigital nunca tienen registros de
Inventario.                                                           (DOMINIO 5)
```

---

# Inventario

## Descripción

Representa las existencias de una variante de producto específica dentro de una bodega específica.

El inventario es distribuido: debe estar siempre vinculado a exactamente una variante y una bodega. Nunca se permiten existencias negativas bajo ninguna circunstancia.

**Decisión de diseño — inventario por variante:** la especificación exige que el inventario esté ligado a "un producto y una bodega específica", pero también define las variantes como diferencias de color, talla y modelo. Unas existencias contadas a nivel de producto no pueden responder cuántas unidades de "rojo, talla M" quedan en una bodega dada, así que la asociación se hace hacia `VarianteProducto`, que es la especialización del concepto de producto que el negocio realmente cuenta.

**Fuente:** DOMINIO 6. Gestión del Inventario; OBJ-06; Sección 11. Validaciones Críticas.

## Atributos

| Atributo           | Tipo              | Descripción                                                                                                                                                                                                                                                       |
| ------------------ | ----------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| identificador      | String            | Identifica de forma única el registro de inventario.                                                                                                                                                                                                              |
| variante           | VarianteProducto  | Variante de producto a la que se refiere este registro de inventario. Obligatorio.                                                                                                                                                                                |
| bodega             | Bodega            | Bodega a la que pertenece este registro de inventario. Obligatorio.                                                                                                                                                                                               |
| cantidadDisponible | Integer           | Cantidad actualmente disponible para la venta. Nunca debe ser negativa.                                                                                                                                                                                           |
| cantidadReservada  | Integer           | Cantidad reservada por pedidos pendientes. Inferido: el tipo de movimiento "Reserva" del DOMINIO 6 solo tiene sentido si las existencias reservadas se rastrean por separado de las disponibles; de lo contrario una reserva no podría distinguirse de una venta.  |
| estadoInventario   | EstadoInventario  | Condición de las existencias, por ejemplo Disponible o Dañado.                                                                                                                                                                                                    |

## Relaciones

* Un registro de `Inventario` se refiere a exactamente una `VarianteProducto` y exactamente una `Bodega`.
* Un registro de `Inventario` solo se modifica a través de instancias de `MovimientoInventario`, que proveen su trazabilidad.
* Un registro de `Inventario` suministra las existencias consumidas por las reservas de `ItemPedido` y liberadas por las devoluciones de `ItemDevolución`.

## Reglas de Negocio

```text
cantidadDisponible y cantidadReservada nunca deben ser negativas.     (DOMINIO 6)

No se pueden reservar existencias inexistentes ni existencias cuyo
estadoInventario sea DAÑADO.                                          (Sección 11)

Un registro de Inventario existe únicamente para variantes de un
ProductoFísico.                                                       (DOMINIO 5)

Todo cambio en cantidadDisponible o cantidadReservada debe quedar
registrado como un MovimientoInventario.                              (DOMINIO 6)
```

## Ejemplos de Operaciones Generadas

* `INGRESO_INVENTARIO`
* `RESERVA_INVENTARIO`
* `SALIDA_VENTA_INVENTARIO`
* `AJUSTE_INVENTARIO`
* `DEVOLUCIÓN_INVENTARIO`

---

# MovimientoInventario

## Descripción

Representa un cambio individual aplicado a un registro de inventario, proveyendo trazabilidad para cada variación de existencias.

El movimiento es el registro histórico de lo que le ocurrió a las existencias, mientras que `Inventario` mantiene las cantidades actuales resultantes.

**Fuente:** DOMINIO 6 — "Movimientos: Ingreso, Reserva, Salida por venta, Ajuste y Devolución."

## Atributos

| Atributo          | Tipo                      | Descripción                                                                                                                                                                                                                                        |
| ----------------- | ------------------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| identificador     | String                    | Identifica de forma única el movimiento.                                                                                                                                                                                                           |
| inventario        | Inventario                | Registro de inventario afectado por este movimiento.                                                                                                                                                                                               |
| tipoMovimiento    | TipoMovimientoInventario  | Tipo de movimiento: Ingreso, Reserva, Salida por Venta, Ajuste o Devolución.                                                                                                                                                                       |
| cantidad          | Integer                   | Cantidad involucrada en el movimiento.                                                                                                                                                                                                             |
| fechaMovimiento   | LocalDateTime             | Fecha y hora en que ocurrió el movimiento. Inferido: todo evento de negocio trazable requiere una marca de tiempo, en coherencia con cómo la especificación trata la temporalidad del ciclo de vida del pedido.                                    |
| ejecutadoPor      | Usuario                   | Usuario que originó el movimiento. Inferido de la Matriz de Responsabilidades, donde la administración de inventario se comparte explícitamente entre Vendedor y Operador Logístico, así que el movimiento debe registrar cuál de ellos actuó.     |

## Relaciones

* Un `MovimientoInventario` afecta exactamente un registro de `Inventario`.
* Un `MovimientoInventario` es ejecutado por exactamente un `Usuario`, que es un `Vendedor` o un `OperadorLogístico`.
* Un `MovimientoInventario` de tipo `RESERVA` o `SALIDA_VENTA` se origina en un `Pedido`.
* Un `MovimientoInventario` de tipo `DEVOLUCIÓN` se origina en una `SolicitudDevolución` aprobada.

## Reglas de Negocio

```text
Un MovimientoInventario es inmutable una vez registrado; las
correcciones se expresan como nuevos movimientos de tipo AJUSTE,
nunca como ediciones.

Un movimiento nunca puede dejar cantidadDisponible ni
cantidadReservada en negativo.                                        (DOMINIO 6)

Solo un Vendedor (sobre sus propios productos) o un OperadorLogístico
pueden registrar un MovimientoInventario.                             (Matriz de Responsabilidades, RG-03)
```

---

# Carrito

## Descripción

Representa la selección provisional y editable de variantes de producto que hace un comprador antes del checkout.

Un carrito no conlleva compromiso comercial alguno y puede modificarse libremente, a diferencia de un `Pedido`, que representa un compromiso formal.

Modelado como entidad separada de `Pedido` porque el OBJ-07 ("Gestionar el carrito de compras") y el OBJ-08 ("Controlar el ciclo completo de los pedidos") están listados como dos objetivos funcionales distintos, lo cual implica dos conceptos distintos y no uno solo. El carrito se convierte en un `Pedido` una vez que el comprador confirma la compra, momento en el cual comienza el ciclo de vida del pedido descrito en el DOMINIO 7.

**Fuente:** OBJ-07; DOMINIO 7 (estado "Carrito"); Sección 6.1 paso 5 — "El comprador selecciona productos mediante el carrito y confirma el pedido."

## Atributos

| Atributo       | Tipo                 | Descripción                                                                                                                                                       |
| -------------- | -------------------- | -------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| identificador  | String               | Identifica de forma única el carrito. Inferido: requerido para que el carrito sea un objeto de negocio direccionable, como implica el OBJ-07.                     |
| comprador      | Comprador            | Comprador propietario del carrito.                                                                                                                                |
| itemsCarrito   | List\<ItemCarrito\>  | Variantes de producto actualmente seleccionadas en el carrito. Puede estar vacía.                                                                                 |
| fechaCreación  | LocalDateTime        | Fecha y hora de creación del carrito. Inferido: coherente con el marcado temporal de todos los demás objetos de negocio trazables de este modelo.                  |

## Relaciones

* Un `Carrito` pertenece a exactamente un `Comprador`.
* Un `Carrito` contiene cero o más instancias de `ItemCarrito`.
* Un `Carrito` se confirma en exactamente un `Pedido` en el checkout; desde ese momento deja de ser el carrito activo del comprador.

## Reglas de Negocio

```text
Un Comprador tiene exactamente un Carrito activo en cualquier momento.

Un Carrito solo puede contener variantes de Productos cuyo
estadoProducto sea PUBLICADO.                                         (DOMINIO 5, Sección 6.1 paso 4)

Confirmar un Carrito crea un Pedido y cierra el Carrito. El Carrito
nunca se modifica después, de modo que el pedido que produjo siga
siendo reproducible.

Un Carrito no conlleva compromiso comercial y no reserva inventario;
la reserva ocurre cuando se crea el Pedido.                           (DOMINIO 6, DOMINIO 7)
```

## Ejemplos de Operaciones Generadas

* `ADICIÓN_ITEM_CARRITO`
* `ELIMINACIÓN_ITEM_CARRITO`
* `CONFIRMACIÓN_CARRITO`

---

# ItemCarrito

## Descripción

Representa una línea individual de variante de producto dentro de un carrito, antes del checkout.

A diferencia de `ItemPedido`, un item de carrito no congela el precio: el carrito es una selección provisional y siempre refleja el precio vigente del catálogo.

**Fuente:** OBJ-07; Sección 6.1 paso 5. Estructura de línea inferida, reflejando `ItemPedido`: un carrito no puede contener "productos seleccionados" sin ella.

## Atributos

| Atributo   | Tipo              | Descripción                                                                        |
| ---------- | ----------------- | ---------------------------------------------------------------------------------- |
| carrito    | Carrito           | Carrito al que pertenece esta línea.                                               |
| variante   | VarianteProducto  | Variante de producto seleccionada por el comprador.                                |
| cantidad   | Integer           | Cantidad de la variante actualmente seleccionada. Debe ser mayor que cero.         |

## Relaciones

* Un `ItemCarrito` pertenece a exactamente un `Carrito`.
* Un `ItemCarrito` referencia exactamente una `VarianteProducto`.
* Un `ItemCarrito` se convierte en un `ItemPedido` cuando el carrito se confirma.

## Reglas de Negocio

```text
cantidad debe ser mayor que cero.

Un ItemCarrito no congela el precio; el precio aplicable es el precio
vigente del Producto hasta que el Pedido se confirme.

La misma VarianteProducto aparece a lo sumo una vez por Carrito;
añadirla de nuevo incrementa la cantidad de la línea existente.
```

---

# Pedido

## Descripción

Representa el compromiso comercial formal del comprador, creado una vez que un `Carrito` se confirma en el checkout. Su ciclo de vida es el proceso central del sistema.

Un pedido congela las condiciones comerciales de la compra — las variantes seleccionadas, sus cantidades, sus precios unitarios y la dirección de entrega — de modo que los cambios posteriores en el catálogo o en el perfil del comprador nunca alteren pedidos históricos.

Un pedido que alcanza el estado finalizado ya no puede modificarse bajo ninguna circunstancia.

**Fuente:** DOMINIO 7. Gestión de Pedidos; OBJ-08; Sección 6.1 pasos 5–8; Sección 11. Validaciones Críticas.

## Atributos

| Atributo         | Tipo                | Descripción                                                                                                                                                                                                                                                                              |
| ---------------- | ------------------- | ---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| identificador    | String              | Identifica de forma única el pedido.                                                                                                                                                                                                                                                     |
| comprador        | Comprador           | Comprador que realizó el pedido.                                                                                                                                                                                                                                                         |
| itemsPedido      | List\<ItemPedido\>  | Líneas de variante de producto confirmadas dentro del pedido, copiadas del carrito de origen en el checkout. Contiene al menos un elemento.                                                                                                                                               |
| direcciónEnvío   | String              | Dirección de entrega elegida para este pedido, copiada en el checkout. Inferido: el DOMINIO 2 otorga al comprador una dirección principal más direcciones adicionales, así que el pedido debe registrar cuál se seleccionó — por la misma razón por la que `ItemPedido` congela `precioUnitario`. |
| estadoPedido     | EstadoPedido        | Etapa actual del ciclo de vida del pedido: Pendiente de Pago, Pagado, Despachado, Entregado/Finalizado o Cancelado. Un pedido se crea directamente en `PENDIENTE_PAGO`; el valor `CARRITO` del catálogo corresponde a la etapa representada por la entidad `Carrito` y ningún `Pedido` lo ostenta jamás.                                                                                                                                                                                  |
| fechaCreación    | LocalDateTime       | Fecha y hora de creación del pedido, es decir, cuando se confirmó el carrito.                                                                                                                                                                                                             |
| montoTotal       | BigDecimal          | Valor total del pedido, igual a la suma de los `subtotal` de sus `itemsPedido`.                                                                                                                                                                                                           |
| moneda           | Moneda              | Moneda en la que se expresa `montoTotal`. Inferido: ver la justificación de moneda en **Reglas de Diseño del Dominio**.                                                                                                                                                                   |
| pagos            | List\<Pago\>        | Intentos de pago registrados contra el pedido. Contiene al menos un elemento desde el momento en que se intenta el pago por primera vez.                                                                                                                                                  |
| factura          | Factura?            | Documento comercial emitido para el pedido una vez pagado. Ausente mientras el pedido no haya sido pagado.                                                                                                                                                                                |
| envíos           | List\<Envío\>       | Envíos que cumplen las líneas físicas del pedido. Vacía para pedidos compuestos exclusivamente por productos digitales.                                                                                                                                                                   |

## Relaciones

* Un `Pedido` pertenece a exactamente un `Comprador`.
* Un `Pedido` se origina en exactamente un `Carrito` confirmado.
* Un `Pedido` contiene una o más instancias de `ItemPedido`.
* Un `Pedido` se salda mediante una o más instancias de `Pago`; solo una de ellas puede alcanzar `APROBADO`.
* Un `Pedido` se factura mediante a lo sumo una `Factura`, emitida cuando el pedido alcanza `PAGADO`.
* Un `Pedido` se cumple mediante cero o más instancias de `Envío`. No tiene ninguna cuando todas sus líneas son digitales, y puede tener más de una cuando sus líneas físicas se despachan desde bodegas distintas.
* Un `Pedido` puede ser disputado por cero o más instancias de `SolicitudDevolución`.

## Reglas de Negocio

```text
Un Pedido se crea únicamente a partir de un Carrito confirmado, y solo
por un Comprador cuyo estadoComercial sea HABILITADO.                 (DOMINIO 2, Sección 6.1 paso 5)

Un Pedido en estado ENTREGADO ya no puede modificarse bajo ninguna
circunstancia.                                                        (Sección 11)

Un Pedido puede alcanzar CANCELADO desde PENDIENTE_PAGO o desde
PAGADO, y nunca después del despacho. Cancelar un Pedido libera todas
las reservas de inventario que mantiene. CANCELADO es inferido; ver
EstadoPedido en Objetos de Valor del Dominio.

montoTotal siempre equivale a la suma de los subtotales de los
itemsPedido, calculada en el momento de la creación y nunca
recalculada después.

direcciónEnvío es una copia tomada en el checkout. Los cambios
posteriores en las direcciones del comprador nunca alteran un Pedido
existente.

Crear un Pedido reserva inventario para cada línea que referencie una
variante de un ProductoFísico.                                        (DOMINIO 6)

Pedidos mixtos — líneas físicas y digitales en el mismo Pedido:
  · las líneas digitales se consideran entregadas en cuanto el Pedido
    alcanza PAGADO;
  · las líneas físicas generan una o más instancias de Envío;
  · el Pedido alcanza DESPACHADO cuando se despacha su primer Envío,
    y ENTREGADO solo cuando todos los Envíos han sido entregados;
  · un Pedido compuesto exclusivamente por líneas digitales no genera
    ningún Envío y pasa directamente de PAGADO a ENTREGADO.
```

## Ejemplos de Operaciones Generadas

* `CREACIÓN_PEDIDO`
* `CONFIRMACIÓN_PAGO_PEDIDO`
* `DESPACHO_PEDIDO`
* `ENTREGA_PEDIDO`
* `CANCELACIÓN_PEDIDO`

---

# ItemPedido

## Descripción

Representa una línea individual y confirmada de variante de producto dentro de un pedido, con la cantidad y el precio fijados en el momento de la compra.

**Fuente:** DOMINIO 7; Sección 6.1 paso 5.

## Atributos

| Atributo        | Tipo              | Descripción                                                                                                                                                              |
| --------------- | ----------------- | -------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| pedido          | Pedido            | Pedido al que pertenece esta línea.                                                                                                                                      |
| variante        | VarianteProducto  | Variante de producto comprada por el comprador. El producto en sí es alcanzable a través de `variante.producto`.                                                         |
| cantidad        | Integer           | Cantidad de la variante solicitada. Debe ser mayor que cero.                                                                                                             |
| precioUnitario  | BigDecimal        | Precio del producto en el momento en que se confirmó el pedido. Mantenido independiente de `Producto.precio` para que los cambios posteriores de precio no alteren pedidos históricos. |
| subtotal        | BigDecimal        | `cantidad * precioUnitario`.                                                                                                                                             |

## Relaciones

* Un `ItemPedido` pertenece a exactamente un `Pedido`.
* Un `ItemPedido` referencia exactamente una `VarianteProducto`.
* Un `ItemPedido` que referencia una variante de un `ProductoFísico` se incluye en exactamente un `Envío`.
* Un `ItemPedido` puede ser referenciado por cero o más instancias de `ItemDevolución`.

## Reglas de Negocio

```text
cantidad debe ser mayor que cero.

precioUnitario y subtotal se congelan en el checkout y nunca se
recalculan, de modo que los pedidos históricos permanezcan fieles a
las condiciones aceptadas por el comprador.

Un ItemPedido nunca se modifica después de creado el Pedido.          (Sección 11)
```

---

# Pago

## Descripción

Representa un intento de cobrar al comprador el monto de un pedido.

La especificación exige validar el pago antes de que comience el cumplimiento, y el ciclo de vida del pedido distingue entre `PENDIENTE_PAGO` y `PAGADO`, pero ninguna entidad del borrador original registraba el dinero entrante — solo `Reembolso` registraba el dinero saliente. `Pago` cierra esa brecha.

**Decisión de diseño — intentos de pago, no un único pago:** un pago puede ser rechazado y reintentado. Modelar un único pago por pedido sobrescribiría el intento fallido y perdería su traza, lo cual contradice el requisito de trazabilidad de la Sección 1 y dejaría sin sentido los valores `RECHAZADO` y `FALLIDO` de `EstadoPago`. Por eso cada intento es su propio registro.

**Decisión de diseño — `Pago` no es `Factura`:** la `Factura` es el documento comercial que soporta la venta; el `Pago` es el evento financiero que la salda. Tienen ciclos de vida distintos, responsables distintos y razones de existir distintas, así que ninguno reemplaza al otro.

**Fuente:** DOMINIO 7 (estados "Pendiente de Pago" y "Pagado"); Sección 6.1 paso 6 — "Se valida el pago y se inicia el flujo de preparación."

## Atributos

| Atributo       | Tipo           | Descripción                                                                                                                                                              |
| -------------- | -------------- | -------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| identificador  | String         | Identifica de forma única el intento de pago.                                                                                                                            |
| pedido         | Pedido         | Pedido que este pago salda.                                                                                                                                              |
| monto          | BigDecimal     | Monto cobrado, igual a `Pedido.montoTotal`.                                                                                                                              |
| moneda         | Moneda         | Moneda en la que se expresa el pago. Inferido: ver la justificación de moneda en **Reglas de Diseño del Dominio**.                                                        |
| estadoPago     | EstadoPago     | Estado actual del intento de pago.                                                                                                                                       |
| fechaPago      | LocalDateTime  | Fecha y hora en que se registró el intento de pago. Inferido: coherente con el marcado temporal de todos los demás eventos de negocio trazables de este modelo.           |

## Relaciones

* Un `Pago` salda exactamente un `Pedido`.
* Un `Pedido` puede acumular varias instancias de `Pago`, una por intento.
* Un `Pago` que alcanza `APROBADO` es lo que mueve su `Pedido` de `PENDIENTE_PAGO` a `PAGADO`.
* Un `Pago` es la contraparte financiera de un `Reembolso`: uno registra dinero entrando, el otro dinero saliendo.

## Reglas de Negocio

```text
monto siempre equivale a Pedido.montoTotal; la especificación no
contempla pagos parciales.

A lo sumo un Pago por Pedido puede alcanzar APROBADO.

Un Pedido pasa a PAGADO únicamente cuando una de sus instancias de
Pago alcanza APROBADO.                                                (Sección 6.1 paso 6)

Un Pago rechazado o fallido nunca modifica el Pedido; permanece como
registro histórico del intento.

No se modela ningún medio de pago: la especificación valida que el
pago ocurrió pero no describe el medio utilizado.
```

## Ejemplos de Operaciones Generadas

* `REGISTRO_PAGO`
* `APROBACIÓN_PAGO`
* `RECHAZO_PAGO`

---

# Factura

## Descripción

Representa la información comercial de facturación asociada a una venta completada.

La factura es el documento comercial que soporta la transacción, emitido una vez que el pedido ha sido pagado.

**Fuente:** OBJ-09; Sección 4.1 — "Facturación: Información comercial asociada a las ventas"; Sección 6.1 paso 6.

## Atributos

| Atributo       | Tipo           | Descripción                                                                                                       |
| -------------- | -------------- | ----------------------------------------------------------------------------------------------------------------- |
| identificador  | String         | Identifica de forma única la factura.                                                                             |
| pedido         | Pedido         | Pedido para el cual se generó esta factura. Toda factura remite a exactamente una transacción comercial.          |
| comprador      | Comprador      | Comprador que está siendo facturado.                                                                              |
| fechaEmisión   | LocalDateTime  | Fecha y hora de emisión de la factura, una vez que el pedido alcanzó `PAGADO`.                                    |
| montoTotal     | BigDecimal     | Monto total facturado, tomado de `Pedido.montoTotal`.                                                             |
| moneda         | Moneda         | Moneda en la que se expresa la factura, tomada del pedido.                                                        |

## Relaciones

* Una `Factura` corresponde a exactamente un `Pedido`.
* Una `Factura` se emite a exactamente un `Comprador`, que es el comprador de ese pedido.
* Una `Factura` documenta la venta; no registra el recaudo de los fondos, lo cual corresponde a `Pago`.

## Reglas de Negocio

```text
Una Factura se emite únicamente cuando el Pedido alcanza PAGADO.      (Sección 6.1 paso 6)

A lo sumo una Factura por Pedido.

Una Factura es inmutable una vez emitida; las correcciones se
expresan como nuevos documentos comerciales, nunca como ediciones.

montoTotal y moneda siempre coinciden con los del Pedido.
```

## Ejemplos de Operaciones Generadas

* `EMISIÓN_FACTURA`

---

# Envío

## Descripción

Representa el proceso logístico de empaque, despacho y transporte de las líneas físicas de un pedido hacia el comprador.

Un pedido cuyas líneas físicas están almacenadas en bodegas distintas produce más de un envío, ya que cada envío parte de exactamente una bodega de origen.

**Fuente:** OBJ-10; Sección 4.1 — "Envíos: Procesos logísticos para productos físicos"; Sección 6.1 pasos 7–8; Matriz de Responsabilidades ("Gestión de Pedidos → Op. Logístico").

## Atributos

| Atributo            | Tipo                 | Descripción                                                                                                                                                                                                                    |
| ------------------- | -------------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| identificador       | String               | Identifica de forma única el envío.                                                                                                                                                                                            |
| pedido              | Pedido               | Pedido que se está cumpliendo.                                                                                                                                                                                                 |
| items               | List\<ItemPedido\>   | Líneas del pedido incluidas en este envío. Inferido: un pedido puede despacharse desde varias bodegas, así que cada envío debe declarar qué líneas transporta; de lo contrario el despacho parcial no podría representarse.     |
| bodegaOrigen        | Bodega               | Bodega desde la cual se despachó el envío.                                                                                                                                                                                     |
| operadorLogístico   | OperadorLogístico    | Operador responsable del despacho. Directamente implicado por la Matriz de Responsabilidades.                                                                                                                                   |
| estadoEnvío         | EstadoEnvío          | Estado actual del envío.                                                                                                                                                                                                       |
| fechaDespacho       | LocalDateTime        | Fecha y hora en que el envío salió de la bodega.                                                                                                                                                                               |
| fechaEntrega        | LocalDateTime        | Fecha y hora en que el envío fue entregado. Su confirmación es lo que hace avanzar el pedido hacia `ENTREGADO` (Sección 6.1 paso 8).                                                                                            |

## Relaciones

* Un `Envío` cumple líneas de exactamente un `Pedido`.
* Un `Envío` parte de exactamente una `Bodega`.
* Un `Envío` es responsabilidad de exactamente un `OperadorLogístico`.
* Un `Envío` transporta una o más instancias de `ItemPedido`, todas ellas referenciando variantes de instancias de `ProductoFísico`.

## Reglas de Negocio

```text
Un Envío se crea únicamente después de que el Pedido alcanza PAGADO. (Sección 6.1 pasos 6–7)

Un Envío nunca contiene líneas que referencien un ProductoDigital.    (DOMINIO 5)

Todo ItemPedido físico de un Pedido pertenece a exactamente un Envío.

Despachar un Envío genera un MovimientoInventario de tipo
SALIDA_VENTA sobre los registros de Inventario correspondientes.      (DOMINIO 6)

El Pedido alcanza ENTREGADO solo cuando cada una de sus instancias de
Envío ha sido entregada.                                              (Sección 6.1 paso 8)
```

## Ejemplos de Operaciones Generadas

* `CREACIÓN_ENVÍO`
* `DESPACHO_ENVÍO`
* `ENTREGA_ENVÍO`

---

# SolicitudDevolución

## Descripción

Representa la solicitud de un comprador para devolver uno o más items comprados de un pedido.

La solicitud declara exactamente qué líneas del pedido se están devolviendo y en qué cantidad, a través de sus líneas de `ItemDevolución`. Sin ese detalle no podría determinarse el monto a reembolsar, ya que un comprador puede devolver solo una parte de un pedido.

**Fuente:** OBJ-11; Matriz de Responsabilidades ("Gestión Reembolsos → Comprador"); DOMINIO 6 (tipo de movimiento "Devolución").

## Atributos

| Atributo             | Tipo                     | Descripción                                                                                                                                                          |
| -------------------- | ------------------------ | ---------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| identificador        | String                   | Identifica de forma única la solicitud de devolución.                                                                                                                |
| pedido               | Pedido                   | Pedido al que se refiere la devolución.                                                                                                                              |
| solicitadoPor        | Comprador                | Comprador que solicitó la devolución, coincidiendo con la Matriz de Responsabilidades, donde solo la fila del Comprador está marcada para el inicio de este proceso.  |
| itemsDevolución      | List\<ItemDevolución\>   | Líneas que se están devolviendo. Contiene al menos un elemento. Inferido: el OBJ-11 habla de devolver "uno o más productos", lo cual exige una estructura de líneas explícita. |
| motivo               | String                   | Razón dada para la devolución.                                                                                                                                       |
| fechaSolicitud       | LocalDateTime            | Fecha y hora de creación de la solicitud.                                                                                                                            |
| estadoDevolución     | EstadoDevolución         | Estado actual de la solicitud de devolución.                                                                                                                         |

## Relaciones

* Una `SolicitudDevolución` se refiere a exactamente un `Pedido`.
* Una `SolicitudDevolución` es creada por exactamente un `Comprador`, que debe ser el comprador de ese pedido.
* Una `SolicitudDevolución` contiene una o más instancias de `ItemDevolución`.
* Una `SolicitudDevolución` aprobada origina exactamente un `Reembolso`.
* Una `SolicitudDevolución` aprobada sobre items físicos genera instancias de `MovimientoInventario` de tipo `DEVOLUCIÓN`.

## Reglas de Negocio

```text
Una SolicitudDevolución solo puede ser creada por el Comprador
propietario del Pedido.                                               (RG-03)

Una SolicitudDevolución solo puede crearse sobre un Pedido que haya
sido entregado.                                                       (OBJ-11, DOMINIO 7)

La cantidad devuelta de una línea nunca puede exceder la cantidad
comprada en el ItemPedido correspondiente, descontando las cantidades
ya devueltas en solicitudes previas.

Una SolicitudDevolución aprobada sobre items físicos genera un
MovimientoInventario de tipo DEVOLUCIÓN.                              (DOMINIO 6)

Una SolicitudDevolución origina un Reembolso solo cuando es aprobada.
```

## Ejemplos de Operaciones Generadas

* `CREACIÓN_SOLICITUD_DEVOLUCIÓN`
* `APROBACIÓN_DEVOLUCIÓN`
* `RECHAZO_DEVOLUCIÓN`
* `FINALIZACIÓN_DEVOLUCIÓN`

---

# ItemDevolución

## Descripción

Representa una línea individual dentro de una solicitud de devolución: qué línea comprada del pedido se está devolviendo, y cuántas unidades de ella.

Esta es la estructura que hace calculable el monto del reembolso, ya que vincula cada unidad devuelta con el precio congelado en la línea original del pedido.

**Fuente:** OBJ-11 — "devolver uno o más productos"; estructura de línea inferida, reflejando `ItemPedido`, para que las devoluciones parciales y su monto de reembolso puedan representarse.

## Atributos

| Atributo             | Tipo                 | Descripción                                                                                                                                                                          |
| -------------------- | -------------------- | -------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| solicitudDevolución  | SolicitudDevolución  | Solicitud de devolución a la que pertenece esta línea.                                                                                                                               |
| itemPedido           | ItemPedido           | Línea original del pedido que se está devolviendo. La variante y su precio de compra son alcanzables a través de ella.                                                                |
| cantidad             | Integer              | Número de unidades que se están devolviendo. Debe ser mayor que cero y no mayor que la cantidad comprada.                                                                             |
| montoReembolsable    | BigDecimal           | `cantidad * itemPedido.precioUnitario`. Inferido: calculado a partir del precio de compra congelado para que el reembolso restituya lo que el comprador realmente pagó, no el precio vigente. |

## Relaciones

* Un `ItemDevolución` pertenece a exactamente una `SolicitudDevolución`.
* Un `ItemDevolución` se refiere a exactamente un `ItemPedido`, que debe pertenecer al pedido de esa solicitud de devolución.
* Los valores de `montoReembolsable` de todas las instancias de `ItemDevolución` de una solicitud suman el monto del `Reembolso` resultante.

## Reglas de Negocio

```text
cantidad debe ser mayor que cero y no debe exceder la cantidad del
ItemPedido referenciado, descontando las unidades ya devueltas.

montoReembolsable siempre se calcula a partir de
ItemPedido.precioUnitario, el precio congelado en el checkout, nunca
a partir del precio vigente del Producto.

El ItemPedido referenciado debe pertenecer al Pedido de la
SolicitudDevolución.
```

---

# Reembolso

## Descripción

Representa la restitución monetaria procesada como resultado de una devolución aprobada.

Modelado como entidad separada de `SolicitudDevolución` porque los objetivos de la propia especificación separan "devoluciones" — el retorno del producto — de "reembolsos" — el movimiento del dinero — y porque la Matriz de Responsabilidades asigna el paso del reembolso específicamente al Administrador, mientras que la devolución es iniciada por el Comprador.

`Reembolso` es la contraparte financiera de `Pago`: uno registra dinero saliendo del marketplace, el otro dinero entrando en él.

**Fuente:** OBJ-11; Matriz de Responsabilidades ("Gestión Reembolsos → Comprador solicita, Admin ejecuta").

## Atributos

| Atributo             | Tipo                 | Descripción                                                                                                                                       |
| -------------------- | -------------------- | ------------------------------------------------------------------------------------------------------------------------------------------------- |
| identificador        | String               | Identifica de forma única el reembolso.                                                                                                           |
| solicitudDevolución  | SolicitudDevolución  | Solicitud de devolución que originó este reembolso.                                                                                               |
| monto                | BigDecimal           | Monto restituido al comprador, igual a la suma de los `montoReembolsable` de las líneas `ItemDevolución` de la solicitud.                          |
| moneda               | Moneda               | Moneda en la que se expresa el reembolso, tomada del pedido original. Inferido: ver la justificación de moneda en **Reglas de Diseño del Dominio**. |
| estadoReembolso      | EstadoReembolso      | Estado actual del reembolso.                                                                                                                      |
| procesadoPor         | Administrador        | Administrador que procesó el reembolso, según la Matriz de Responsabilidades.                                                                      |
| fechaProceso         | LocalDateTime        | Fecha y hora en que se procesó el reembolso.                                                                                                      |

## Relaciones

* Un `Reembolso` se origina en exactamente una `SolicitudDevolución` aprobada.
* Un `Reembolso` es procesado por exactamente un `Administrador`.
* Un `Reembolso` restituye al `Comprador` que creó la solicitud de devolución, alcanzable a través de la solicitud.
* Un `Reembolso` es el movimiento financiero inverso del `Pago` que saldó el pedido original.

## Reglas de Negocio

```text
Un Reembolso existe únicamente para una SolicitudDevolución en estado
APROBADA.

monto siempre equivale a la suma de los montoReembolsable de las
líneas de devolución, y por lo tanto nunca excede lo que el comprador
realmente pagó.

moneda siempre coincide con la moneda del Pedido original.

Solo un Administrador puede procesar un Reembolso.                    (Matriz de Responsabilidades)
```

## Ejemplos de Operaciones Generadas

* `PROCESAMIENTO_REEMBOLSO`
* `RECHAZO_REEMBOLSO`

---

# Operación

## Descripción

Representa una acción de negocio significativa ejecutada dentro del marketplace.

Las operaciones proveen trazabilidad entre usuarios, entidades de negocio y registros de auditoría. Una entidad de negocio puede generar múltiples operaciones durante su ciclo de vida.

Una operación representa un evento o acción que ocurrió; es distinta del estado actual de la entidad afectada.

Por ejemplo:

```text
Pedido.estadoPedido = PAGADO
```

representa el estado actual del pedido, mientras que:

```text
Operación.tipoOperación = CONFIRMACIÓN_PAGO_PEDIDO
```

representa la acción que causó el cambio de estado.

**Decisión de diseño — referencia tipada en lugar de una raíz abstracta común:** la referencia bancaria apunta `Operación.productoAfectado` hacia `ProductoBancario`, una raíz abstracta que todos sus productos comparten. NexusMarket no tiene un concepto de negocio equivalente: los eventos significativos afectan a entidades tan distintas como `Pedido`, `Pago`, `Inventario`, `Envío`, `SolicitudDevolución`, `Reembolso`, `Producto`, `Vendedor` y `Bodega`, que no comparten ningún significado de negocio común. Introducir una raíz técnica artificial para agruparlas inventaría un concepto que la especificación no respalda. En su lugar, la entidad afectada se identifica mediante un catálogo controlado (`TipoEntidadAfectada`) más su identificador, lo cual mantiene la referencia tipada y controlada por el dominio sin inventar una jerarquía falsa.

**Fuente:** OBJ-12; Sección 1 — "garantizando trazabilidad y coordinación entre todos los participantes"; RG-01. Patrón adoptado de la cadena `Operación` → `RegistroAuditoría` de la referencia bancaria.

## Atributos

| Atributo             | Tipo                  | Descripción                                                                 |
| -------------------- | --------------------- | --------------------------------------------------------------------------- |
| idOperación          | String                | Identifica de forma única la operación.                                     |
| tipoOperación        | TipoOperación         | Categoría de la operación de negocio.                                       |
| fechaEjecución       | LocalDateTime         | Fecha y hora en que ocurrió la operación.                                   |
| ejecutadoPor         | Usuario               | Usuario responsable de ejecutar la operación.                               |
| tipoEntidadAfectada  | TipoEntidadAfectada   | Tipo de entidad de negocio afectada por la operación.                       |
| idEntidadAfectada    | String                | Identificador de la instancia específica afectada por la operación.         |

## Relaciones

* Cada `Operación` es ejecutada por exactamente un `Usuario`.
* Cada `Operación` afecta exactamente una entidad de negocio, identificada por `tipoEntidadAfectada` e `idEntidadAfectada`.
* Una entidad de negocio puede generar cero o más instancias de `Operación` durante su ciclo de vida.
* Cada `Operación` significativa debe quedar registrada en el `RegistroAuditoría`.

## Reglas de Negocio

```text
Toda acción de negocio significativa ejecutada dentro del marketplace
debe generar una Operación.

Toda Operación debe ser ejecutada por un Usuario autenticado.         (RG-01)

Toda Operación debe quedar registrada en el RegistroAuditoría.

Una Operación registra un evento que ocurrió; nunca se modifica
después, porque el evento en sí no cambia.
```

---

# RegistroAuditoría

## Descripción

Representa la traza de auditoría inmutable del marketplace.

Cada registro de auditoría almacena información histórica sobre una operación de negocio significativa, junto con el rol que ostentaba el usuario en el momento de la ejecución.

Los registros de auditoría están pensados para persistirse en una base de datos NoSQL, de modo que soporten detalles flexibles y específicos de cada operación, y trazabilidad histórica de largo plazo.

Los registros de auditoría son de solo-anexado y no deben modificarse ni eliminarse una vez persistidos.

Esta entidad, junto con `Operación`, es la que satisface el OBJ-12 ("Consolidar información administrativa para consulta") y el compromiso de trazabilidad declarado en la Sección 1, ambos referidos a trazabilidad administrativa transversal y no al estado de una transacción de negocio individual.

**Fuente:** OBJ-12; Sección 1. Introducción y Contexto; Sección 5 (el Supervisor como perfil de consulta).

## Atributos

| Atributo             | Tipo                   | Descripción                                                          |
| -------------------- | ---------------------- | -------------------------------------------------------------------- |
| idAuditoría          | String                 | Identifica de forma única el registro de auditoría.                  |
| tipoOperación        | TipoOperación          | Tipo de operación de negocio registrada.                             |
| fechaOperación       | LocalDateTime          | Marca de tiempo del momento en que ocurrió el evento.                |
| ejecutadoPor         | Usuario                | Usuario responsable de la operación.                                 |
| rolUsuario           | RolSistema             | Rol del usuario en el momento de la ejecución.                       |
| tipoEntidadAfectada  | TipoEntidadAfectada    | Tipo de entidad de negocio involucrada en la operación.              |
| idEntidadAfectada    | String                 | Identificador de la instancia específica involucrada.                |
| detalles             | Map\<String, Object\>  | Documento flexible con información específica de la operación.       |

## Relaciones

* Cada registro de `RegistroAuditoría` corresponde a exactamente una `Operación`.
* Cada registro de `RegistroAuditoría` referencia al `Usuario` que ejecutó la operación y al `RolSistema` que ostentaba en ese momento.
* Los registros de `RegistroAuditoría` son la información principal consultada por el `Supervisor` y por los reportes administrativos.

## Reglas de Negocio

* Los registros de auditoría son inmutables.
* Los registros de auditoría son de solo-anexado.
* Un registro de auditoría no puede eliminarse una vez persistido.
* Toda operación de negocio significativa debe producir un registro de auditoría.
* El `rolUsuario` debe representar el rol aplicable en el momento en que se ejecutó la operación, no el rol actual del usuario.
* El campo `detalles` puede contener información específica de la operación requerida para la trazabilidad.

---

# Relación del Ciclo de Vida del Dominio

El ciclo de vida general de cualquier acción de negocio significativa en el marketplace es:

```text
Entidad de negocio
      │
      │ acción de negocio significativa
      ▼
  Operación
      │
      │ registro de auditoría
      ▼
RegistroAuditoría
```

Por ejemplo, cuando se confirma el pago de un pedido:

```text
Pedido
   │
   │ un Pago alcanza APROBADO
   ▼
 PAGADO
   │
   ├── Operación
   │      tipoOperación       = CONFIRMACIÓN_PAGO_PEDIDO
   │      tipoEntidadAfectada = PEDIDO
   │      idEntidadAfectada   = Pedido.identificador
   │      ejecutadoPor        = Usuario
   │
   └── RegistroAuditoría
          tipoOperación       = CONFIRMACIÓN_PAGO_PEDIDO
          tipoEntidadAfectada = PEDIDO
          idEntidadAfectada   = Pedido.identificador
          ejecutadoPor        = Usuario
          rolUsuario          = COMPRADOR
          detalles            = información específica de la operación
```

De forma similar, cuando se despacha un envío desde una bodega:

```text
Envío
   │
   │ estadoEnvío cambia
   ▼
EN_TRÁNSITO
   │
   ├── MovimientoInventario
   │      tipoMovimiento = SALIDA_VENTA
   │
   ├── Operación
   │      tipoOperación       = DESPACHO_ENVÍO
   │      tipoEntidadAfectada = ENVÍO
   │      idEntidadAfectada   = Envío.identificador
   │      ejecutadoPor        = Usuario
   │
   └── RegistroAuditoría
          tipoOperación       = DESPACHO_ENVÍO
          tipoEntidadAfectada = ENVÍO
          idEntidadAfectada   = Envío.identificador
          ejecutadoPor        = Usuario
          rolUsuario          = OPERADOR_LOGÍSTICO
          detalles            = información específica de la operación
```

Y cuando se procesa un reembolso tras una devolución aprobada:

```text
SolicitudDevolución
   │
   │ estadoDevolución cambia
   ▼
APROBADA
   │
   ├── Reembolso
   │      monto = Σ ItemDevolución.montoReembolsable
   │      estadoReembolso = PROCESADO
   │
   ├── MovimientoInventario
   │      tipoMovimiento = DEVOLUCIÓN
   │
   ├── Operación
   │      tipoOperación       = PROCESAMIENTO_REEMBOLSO
   │      tipoEntidadAfectada = REEMBOLSO
   │      idEntidadAfectada   = Reembolso.identificador
   │      ejecutadoPor        = Usuario
   │
   └── RegistroAuditoría
          tipoOperación       = PROCESAMIENTO_REEMBOLSO
          tipoEntidadAfectada = REEMBOLSO
          idEntidadAfectada   = Reembolso.identificador
          ejecutadoPor        = Usuario
          rolUsuario          = ADMINISTRADOR
          detalles            = información específica de la operación
```

---

# Reglas de Diseño del Dominio

## La Herencia Representa Especialización Genuina

La herencia se usa solo donde el negocio mismo distingue subtipos con reglas diferentes: los roles de usuario, la propiedad de las bodegas y la naturaleza de los productos. Nunca se usa como mecanismo técnico de agrupación, razón por la cual `Operación` identifica la entidad afectada mediante un catálogo controlado en lugar de una raíz abstracta inventada.

## Referencias Explícitas entre Objetos

Las entidades referencian otras entidades directamente en lugar de almacenar campos identificadores sueltos. La única excepción son `Operación.idEntidadAfectada` y `RegistroAuditoría.idEntidadAfectada`, donde la entidad referenciada es polimórfica por naturaleza y por lo tanto queda cualificada por `TipoEntidadAfectada`.

## Objetos de Valor en Lugar de Primitivos

Todo concepto de negocio controlado — roles, estados, tipos, monedas — se representa mediante un Objeto de Valor que hereda de `CatálogoDominio`, nunca mediante una cadena arbitraria. El catálogo completo está definido en *Objetos de Valor del Dominio*.

## La Unidad Vendible Es la Variante

`Producto` es un concepto de catálogo; `VarianteProducto` es la unidad vendible. Las existencias, las líneas de carrito y las líneas de pedido siempre referencian una variante, nunca un producto. Un producto sin variación real se registra con una única variante por defecto, de modo que no exista ninguna ruta opcional producto/variante en ninguna parte del modelo.

## Condiciones Comerciales Congeladas

Un `Pedido` copia, en lugar de referenciar, todo aquello que debe permanecer fiel al momento de la compra: `precioUnitario`, `subtotal`, `montoTotal` y `direcciónEnvío`. Los cambios posteriores en el catálogo o en el perfil del comprador nunca alteran un pedido histórico. `ItemDevolución.montoReembolsable` sigue el mismo principio, calculando a partir del precio congelado y no del vigente.

## Los Montos Monetarios Siempre Van Denominados

Toda entidad que mantiene un monto monetario mantiene también la `Moneda` en la que se expresa: `Producto`, `Pedido`, `Pago`, `Factura` y `Reembolso`.

**Justificación — por qué `Moneda` sí aplica aquí.** La especificación nunca nombra una moneda, lo cual podría sugerir que el concepto está fuera de alcance. No lo está: el marketplace intermedia entre vendedores terceros independientes y compradores, emite facturas, recauda pagos y devuelve dinero mediante reembolsos. Un `BigDecimal` sin denominación es ambiguo en el momento en que existe más de una moneda, y un monto reembolsado debe ser demostrablemente el mismo monto que se cobró — lo cual no puede verificarse si ninguno de los dos lados está denominado. La referencia bancaria ya establece `Moneda` como Objeto de Valor de `CatálogoDominio` exactamente por esta razón, y el mismo argumento se sostiene aquí. Modelarlo ahora cuesta un Objeto de Valor; omitirlo obligaría a cambiar después todos los atributos monetarios del modelo.

## El Dinero que Entra y el que Sale Se Registran Ambos

`Pago` registra el dinero que entra al marketplace y `Reembolso` registra el que sale. `Factura` es el documento comercial que soporta la venta y no reemplaza a ninguno de los dos: un documento que prueba qué se vendió no es un registro del movimiento de los fondos.

## Inmutabilidad de los Hechos Consumados

Los registros que representan algo que ya ocurrió nunca se editan: `MovimientoInventario`, `Pago`, `Factura`, `Operación`, `RegistroAuditoría` y cualquier `Pedido` en estado `ENTREGADO`. Las correcciones se expresan como nuevos registros — un movimiento de `AJUSTE`, un nuevo intento de pago, un nuevo documento comercial — nunca como modificaciones del original.

## Toda Acción Significativa Es Trazable

Toda acción de negocio significativa genera una `Operación`, y toda `Operación` queda registrada en el `RegistroAuditoría` junto con el rol que ostentaba el usuario en ese momento. Esta cadena es la que satisface el compromiso de trazabilidad de la Sección 1 y el objetivo de consulta administrativa OBJ-12.

## Acceso Delimitado por Rol

Ningún participante administra información fuera de su propio rol (RG-03). Esta restricción está declarada en las reglas de negocio de cada entidad — compradores sobre sus propios pedidos, vendedores sobre sus propios productos e inventario, administradores sobre vendedores, bodegas y reembolsos, supervisores en modo de solo lectura — de modo que la autorización sea una propiedad del dominio y no únicamente de la capa de aplicación.

## Trazabilidad hacia la Especificación

Cada entidad declara la sección de la *Especificación Funcional del Negocio* de la cual proviene, y todo atributo que no esté literalmente declarado en la especificación está marcado como inferido con su justificación. Esta es una adición deliberada frente a la referencia bancaria: el razonamiento que produjo el modelo viaja dentro del modelo, de modo que un revisor nunca tenga que reconstruirlo.

---

# Glosario de Equivalencias

Tabla de referencia para cruzar este documento con el entregable en inglés.

## Entidades

| Inglés (entregable) | Español (este documento) |
| ------------------- | ------------------------ |
| User (Abstract)     | Usuario (Abstracta)      |
| Buyer               | Comprador                |
| Seller              | Vendedor                 |
| LogisticsOperator   | OperadorLogístico        |
| Administrator       | Administrador            |
| Supervisor          | Supervisor               |
| Warehouse (Abstract)| Bodega (Abstracta)       |
| MarketplaceWarehouse| BodegaMarketplace        |
| SellerWarehouse     | BodegaVendedor           |
| Product (Abstract)  | Producto (Abstracta)     |
| PhysicalProduct     | ProductoFísico           |
| DigitalProduct      | ProductoDigital          |
| ProductVariant      | VarianteProducto         |
| Inventory           | Inventario               |
| InventoryMovement   | MovimientoInventario     |
| Cart                | Carrito                  |
| CartItem            | ItemCarrito              |
| Order               | Pedido                   |
| OrderItem           | ItemPedido               |
| Payment             | Pago                     |
| Invoice             | Factura                  |
| Shipment            | Envío                    |
| ReturnRequest       | SolicitudDevolución      |
| ReturnItem          | ItemDevolución           |
| Refund              | Reembolso                |
| Operation           | Operación                |
| AuditLog            | RegistroAuditoría        |

## Objetos de Valor

| Inglés (entregable)     | Español (este documento)  |
| ----------------------- | ------------------------- |
| DomainCatalog           | CatálogoDominio           |
| SystemRole              | RolSistema                |
| UserStatus              | EstadoUsuario             |
| BuyerCommercialStatus   | EstadoComercialComprador  |
| ProductType             | TipoProducto              |
| ProductStatus           | EstadoProducto            |
| InventoryStatus         | EstadoInventario          |
| InventoryMovementType   | TipoMovimientoInventario  |
| OrderStatus             | EstadoPedido              |
| PaymentStatus           | EstadoPago                |
| ShipmentStatus          | EstadoEnvío               |
| ReturnStatus            | EstadoDevolución          |
| RefundStatus            | EstadoReembolso           |
| OperationType           | TipoOperación             |
| AffectedEntityType      | TipoEntidadAfectada       |
| Currency                | Moneda                    |

## Atributos

| Inglés (entregable)   | Español (este documento) |
| --------------------- | ------------------------ |
| userId                | idUsuario                |
| identificationNumber  | númeroIdentificación     |
| fullName              | nombreCompleto           |
| email                 | correoElectrónico        |
| role                  | rol                      |
| status                | estado                   |
| primaryAddress        | direcciónPrincipal       |
| additionalAddresses   | direccionesAdicionales   |
| commercialStatus      | estadoComercial          |
| activeCart            | carritoActivo            |
| orders                | pedidos                  |
| legalBusinessName     | razónSocial              |
| taxId                 | nit                      |
| tradeName             | nombreComercial          |
| warehouses            | bodegas                  |
| products              | productos                |
| identifier            | identificador            |
| address               | dirección                |
| owner                 | propietario              |
| name                  | nombre                   |
| description           | descripción              |
| productType           | tipoProducto             |
| seller                | vendedor                 |
| price                 | precio                   |
| currency              | moneda                   |
| variants              | variantes                |
| productStatus         | estadoProducto           |
| variantId             | idVariante               |
| sku                   | sku                      |
| product               | producto                 |
| attributeName         | nombreAtributo           |
| attributeValue        | valorAtributo            |
| variant               | variante                 |
| warehouse             | bodega                   |
| availableQuantity     | cantidadDisponible       |
| reservedQuantity      | cantidadReservada        |
| inventoryStatus       | estadoInventario         |
| inventory             | inventario               |
| movementType          | tipoMovimiento           |
| quantity              | cantidad                 |
| movementDate          | fechaMovimiento          |
| performedBy           | ejecutadoPor             |
| buyer                 | comprador                |
| cartItems             | itemsCarrito             |
| creationDate          | fechaCreación            |
| cart                  | carrito                  |
| order                 | pedido                   |
| orderItems            | itemsPedido              |
| shippingAddress       | direcciónEnvío           |
| orderStatus           | estadoPedido             |
| totalAmount           | montoTotal               |
| payments              | pagos                    |
| invoice               | factura                  |
| shipments             | envíos                   |
| unitPrice             | precioUnitario           |
| subtotal              | subtotal                 |
| amount                | monto                    |
| paymentStatus         | estadoPago               |
| paymentDate           | fechaPago                |
| issueDate             | fechaEmisión             |
| items                 | items                    |
| originWarehouse       | bodegaOrigen             |
| logisticsOperator     | operadorLogístico        |
| shipmentStatus        | estadoEnvío              |
| dispatchDate          | fechaDespacho            |
| deliveryDate          | fechaEntrega             |
| requestedBy           | solicitadoPor            |
| returnItems           | itemsDevolución          |
| reason                | motivo                   |
| requestDate           | fechaSolicitud           |
| returnStatus          | estadoDevolución         |
| returnRequest         | solicitudDevolución      |
| orderItem             | itemPedido               |
| refundableAmount      | montoReembolsable        |
| refundStatus          | estadoReembolso          |
| processedBy           | procesadoPor             |
| processDate           | fechaProceso             |
| operationId           | idOperación              |
| operationType         | tipoOperación            |
| executionDate         | fechaEjecución           |
| affectedEntityType    | tipoEntidadAfectada      |
| affectedEntityId      | idEntidadAfectada        |
| auditId               | idAuditoría              |
| operationDate         | fechaOperación           |
| userRole              | rolUsuario               |
| details               | detalles                 |

## Códigos de Catálogo

| Inglés (entregable)          | Español (este documento)        |
| ---------------------------- | ------------------------------- |
| ACTIVE / INACTIVE / BLOCKED  | ACTIVO / INACTIVO / BLOQUEADO   |
| ENABLED / RESTRICTED         | HABILITADO / RESTRINGIDO        |
| PHYSICAL / DIGITAL           | FÍSICO / DIGITAL                |
| PUBLISHED                    | PUBLICADO                       |
| SUSPENDED                    | SUSPENDIDO                      |
| DISCONTINUED                 | DESCONTINUADO                   |
| AVAILABLE / DAMAGED          | DISPONIBLE / DAÑADO             |
| INBOUND                      | INGRESO                         |
| RESERVATION                  | RESERVA                         |
| SALE_OUTBOUND                | SALIDA_VENTA                    |
| ADJUSTMENT                   | AJUSTE                          |
| RETURN                       | DEVOLUCIÓN                      |
| CART                         | CARRITO                         |
| PENDING_PAYMENT              | PENDIENTE_PAGO                  |
| PAID                         | PAGADO                          |
| DISPATCHED                   | DESPACHADO                      |
| DELIVERED                    | ENTREGADO                       |
| CANCELLED                    | CANCELADO                       |
| PENDING                      | PENDIENTE                       |
| APPROVED                     | APROBADO / APROBADA             |
| REJECTED                     | RECHAZADO                       |
| FAILED                       | FALLIDO                         |
| PROCESSED                    | PROCESADO                       |
| IN_TRANSIT                   | EN_TRÁNSITO                     |
| REQUESTED                    | SOLICITADA                      |
| COMPLETED                    | COMPLETADA                      |
| BUYER / SELLER               | COMPRADOR / VENDEDOR            |
| LOGISTICS_OPERATOR           | OPERADOR_LOGÍSTICO              |
| ADMINISTRATOR / SUPERVISOR   | ADMINISTRADOR / SUPERVISOR      |
| ORDER / PAYMENT / INVENTORY  | PEDIDO / PAGO / INVENTARIO      |
| SHIPMENT / REFUND / PRODUCT  | ENVÍO / REEMBOLSO / PRODUCTO    |
| RETURN_REQUEST               | SOLICITUD_DEVOLUCIÓN            |
| WAREHOUSE                    | BODEGA                          |
