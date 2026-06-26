# 🦅 PhoenixLogin - Advanced Authentication Plugin
### Desarrollado por Tomas2193

![Version](https://img.shields.io/badge/version-2.0-blue) ![Spigot](https://img.shields.io/badge/platform-spigot-orange) ![License](https://img.shields.io/badge/license-MIT-green)

**PhoenixLogin** es la solución definitiva para la autenticación en servidores de Minecraft. Diseñado desde cero para ser seguro, rápido y extremadamente personalizable.

---

## 🔥 Novedades de la Versión 2.0 "Super Update"

Esta actualización transforma por completo la experiencia del usuario y del administrador.

### 🎨 Experiencia Visual y UX
- **Inmersión Total**: Los jugadores que están logueándose **no ven a otros jugadores** y viceversa. Esto elimina el caos visual en el spawn.
- **Chat Limpio**: Sistema inteligente que oculta el chat global a los usuarios no autenticados. Solo verás lo importante.
- **Títulos Personalizables**: Configura los mensajes de bienvenida y éxito directamente desde la `config.yml`.
- **Efectos Premium**: Sonidos, partículas y barras de jefe (BossBars) totalmente configurables.

### 🛡️ Seguridad de Grado Militar
- **Base de Datos Robusta**:
  - Hashing de contraseñas con **BCrypt** (Estándar de la industria).
  - Soporte para **SQLite** (local, cero configuración) y **MySQL** (para redes grandes).
  - Connection Pool optimizado con **HikariCP** para un rendimiento extremo.
- **Protección**:
  - Límites de intentos de login.
  - Bloqueo temporal de cuentas (Lockout).
  - Filtrado de contraseñas en logs de consola.

### 🧩 Funcionalidades Clave
- **Captcha Inteligente**: Si un usuario se equivoca el comando, el sistema le sugiere amablemente cómo usarlo en lugar de lanzar errores rojos.
- **Sistema de Sesiones**: Recuerda la IP de los jugadores para que no tengan que loguearse cada vez que entran (configurable).
- **Void World**: Un mundo vacío dedicado para la carga y login, optimizando el rendimiento del lobby principal.
- **Configuración en Español**: Archivo `config.yml` masivo y explicado detalladamente en español.

---

## 🚀 Instalación

1. Descarga el plugin.
2. Colócalo en la carpeta `plugins/` de tu servidor.
3. Inicia el servidor.
4. ¡Disfruta! La configuración por defecto ya es óptima.

## ⚙️ Comandos

| Comando | Descripción | Permiso |
|BCrypt|---|---|
| `/login <pass>` | Iniciar sesión | N/A |
| `/register <pass> <pass>` | Registrarse | N/A |
| `/captcha <code>` | Verificar captcha | `phoenixlogin.captcha` |
| `/changepassword` | Cambiar contraseña | N/A |
| `/phoenixlogin` | Comando de admin | `phoenixlogin.admin` |

## 👨‍💻 Autor

**Tomas2193** - *Creador y Desarrollador Principal*

> "Haciendo plugins que simplemente funcionan."
