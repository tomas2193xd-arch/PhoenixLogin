# PhoenixLogin - Advanced Authentication System

![Version](https://img.shields.io/badge/version-1.0.0-blue.svg)
![Minecraft](https://img.shields.io/badge/minecraft-1.20.x-green.svg)
![License](https://img.shields.io/badge/license-MIT-yellow.svg)

## 🔥 Características Principales

### ✅ Sistema de Autenticación
- **Login & Register**: Sistema completo de registro y autenticación
- **Sesiones Inteligentes**: Recuerda IPs y mantiene sesiones activas
- **Hashing BCrypt**: Contraseñas cifradas con algoritmo BCrypt de alta seguridad
- **Cambio de Contraseña**: Los jugadores pueden cambiar su contraseña en cualquier momento

### 🛡️ Seguridad Avanzada
- **Protección Brute-Force**: Límite de intentos fallidos con bloqueo temporal
- **Validación de Contraseñas**: Requisitos configurables (longitud, mayúsculas, números, caracteres especiales)
- **Sistema de Logs**: Registro completo de todos los intentos de login
- **Bloqueo de Acciones**: El jugador no puede hacer nada hasta autenticarse

### 🤖 Anti-Bot Captcha
- **Captcha de Items**: El jugador debe colocar un item específico en un slot
- **Captcha Matemático**: Resolver operaciones matemáticas simples
- **Dificultad Configurable**: Ajusta la dificultad según tus necesidades

### 🎨 Efectos Visuales & Sonido
- **Boss Bars**: Temporizador visual con cuenta regresiva
- **Titles**: Mensajes de título con subtítulos personalizados
- **Sonidos**: Efectos de sonido para cada acción
- **Partículas**: Efectos de partículas al autenticarse
- **🎵 Música de Login**: Música de fondo atmosférica durante la autenticación en el VoidWorld

### 🌍 Multi-Idioma
- **Español e Inglés incluidos**: Archivos de mensajes completos
- **Fácil de Traducir**: Añade más idiomas editando archivos YAML
- **Cambio Instantáneo**: Cambia el idioma sin reiniciar el servidor

### 💾 Base de Datos
- **SQLite**: Sin configuración, funciona out-of-the-box
- **MySQL**: Soporte completo con pool de conexiones HikariCP
- **Operaciones Async**: Todas las consultas se ejecutan de forma asíncrona

### ⚙️ Altamente Configurable
- **Config.yml Completo**: Más de 40 opciones configurables
- **Comportamiento Personalizable**: Ajusta cada aspecto del plugin
- **Efectos Opcionales**: Activa/desactiva efectos según tu preferencia

---

## 📦 Instalación

1. Descarga el archivo `PhoenixLogin-1.0.0.jar`
2. Colócalo en la carpeta `plugins/` de tu servidor
3. Reinicia o recarga el servidor
4. Configura `config.yml` según tus necesidades
5. ¡Listo!

---

## 🎮 Comandos

### Para Jugadores
- `/register <contraseña> <confirmar>` - Registrar una cuenta
- `/login <contraseña>` - Iniciar sesión
- `/changepassword <anterior> <nueva>` - Cambiar contraseña
- `/unregister <contraseña>` - Eliminar tu cuenta

### Para Administradores
- `/phoenixlogin reload` - Recargar configuración
- `/phoenixlogin info <jugador>` - Ver información de un jugador
- `/phoenixlogin unregister <jugador>` - Eliminar cuenta de un jugador
- `/phoenixlogin stats` - Ver estadísticas del plugin

---

## 🔐 Permisos

- `phoenixlogin.admin` - Acceso a todos los comandos de administración
- `phoenixlogin.bypass` - Bypass del sistema de autenticación
- `phoenixlogin.premium` - Auto-login para cuentas premium (requiere activación)

---

## ⚙️ Configuración

El plugin crea automáticamente 3 archivos de configuración:

- **config.yml** - Configuración principal del plugin
- **messages_es.yml** - Mensajes en español
- **messages_en.yml** - Mensajes en inglés

### Cambiar Idioma
```yaml
language: "es"  # o "en" para inglés
```

### Configurar MySQL
```yaml
database:
  type: "MYSQL"
  mysql:
    host: "localhost"
    port: 3306
    database: "phoenixlogin"
    username: "root"
    password: "tu_password"
```

---

## 🚀 Características Futuras (Roadmap)

- [ ] 2FA con Discord/Email
- [ ] Geolocalización de IPs
- [ ] GUI de administración
- [ ] Sistema de recuperación de contraseñas
- [ ] Detección de VPN
- [ ] Captcha de mapa visual
- [ ] Integración con Discord webhooks
- [ ] Soporte para BungeeCord/Velocity

---

## 🐛 Reportar Bugs

Si encuentras algún bug o tienes sugerencias, por favor crea un issue describiendo:
- Versión del plugin
- Versión de Minecraft/Spigot
- Descripción detallada del problema
- Logs de error (si aplica)

---

## 📄 Licencia

Este proyecto está bajo la licencia MIT.

---

## 👨‍💻 Desarrollador

Creado por **TomLe**

¿Te gusta el plugin? ¡Dale una estrella! ⭐
