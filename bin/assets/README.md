# Asset pack del mod

Este proyecto puede incluir un *asset pack* además del plugin. Para **agregar partículas nuevas** (sin reemplazar las vanilla), la regla de oro es:

- Usar **IDs propios** (namespace propio) para que no haya colisiones con assets existentes.
- Exportar/colocar aquí los archivos que te genere el Asset Editor (o tu pipeline) respetando la estructura de carpetas que usa Hytale para assets.

El flag que habilita el envío del asset pack a clientes está en `src/main/resources/manifest.json` (`IncludesAssetPack: true`).

Notas:
- Si reusás el mismo ID que uno vanilla, *eso sí lo reemplaza*.
- Si todavía no tenés claro el esquema JSON exacto de partículas, crealas en el Asset Editor y exportá: así te asegurás que los archivos queden en el formato/paths correctos.

