# Changelog

All notable changes to PhoenixLogin will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

---

## [1.4.0] - 2025-12-05

### ✨ Added
- **Two-Factor Authentication (2FA)**: Sistema completo de autenticación de dos factores
  - Integración con Discord Webhooks para envío de códigos
  - Códigos de 6 dígitos con expiración de 2 minutos
  - Nuevo comando `/verify <código>` para validación
  - Configuración flexible en `config.yml`
  - Soporte para requerir 2FA en nuevas IPs o siempre

- **Login History System**: Historial completo de intentos de login
  - Nuevo comando `/loginhistory [player]` con aliases `/lhistory` y `/history`
  - Guarda fecha, hora, IP, método y estado de cada intento
  - Los admins pueden consultar historial de cualquier jugador
  - Almacenamiento en base de datos con limpieza automática
  - Muestra últimos 10 intentos con formato profesional

- **Tab Completion**: Autocompletado profesional para comandos
  - TabCompleter para `/phoenixlogin` con sugerencias de subcomandos
  - Autocompleta nombres de jugadores en comandos admin
  - Mejora la experiencia de usuario

- **ChatBlockListener**: Sistema de bloqueo de tab completion
  - Ahora activo y registrado correctamente
  - Bloquea comandos no autorizados para jugadores sin login
  - Solo muestra `/login`, `/register` y sus aliases

- **bStats Integration**: Sistema de métricas anónimas
  - Permite conocer estadísticas de uso del plugin
  - Totalmente anónimo y respetuoso con la privacidad
  - Ayuda al desarrollo y mejoras futuras

### 🔄 Changed
- API version actualizada de 1.2.0 a 1.4.0
- Total de comandos incrementado de 7 a 9
- Total de listeners incrementado de 3 a 4
- Reorganización del sistema de configuración para 2FA

### 🐛 Fixed
- ChatBlockListener ahora se registra correctamente en el startup
- Consistencia de versiones entre plugin y API

### 📚 Documentation
- README actualizado con nuevas features
- Guía completa de configuración de 2FA con Discord
- Ejemplos de uso de nuevos comandos

---

## [1.3.0] - 2025-12-04

### ✨ Added
- Console logging system con ASCII art y colores ANSI
- Estadísticas de startup mejoradas
- Contador de jugadores registrados en startup
- Sistema de música profesional con soporte NBS

### 🔄 Changed
- Mejoras visuales en consola
- Optimización de mensajes de log

---

## [1.2.0] - 2025-12-03

### ✨ Added
- Sistema de API pública para otros plugins
- Eventos personalizados (PreLogin, Login, PreRegister, Register, LoginFailed)
- Multi-language support (English & Spanish)
- Map Captcha system
- Session management mejorado

### 🔄 Changed
- Refactorización del sistema de mensajes
- Mejoras en la arquitectura del plugin

---

## [1.1.0] - 2025-12-02

### ✨ Added
- VoidWorld authentication system
- Location manager para spawn post-login
- Sistema de efectos visuales (BossBar, Titles, Particles)
- Comando `/setspawn` para configurar spawn

### 🐛 Fixed
- Problemas de teleportación después del login
- Bugs en el sistema de spawn

---

## [1.0.0] - 2025-12-01

### ✨ Initial Release
- Sistema básico de autenticación con BCrypt
- Soporte para SQLite y MySQL
- Captcha de items
- Protección de jugadores no autenticados
- Comandos básicos: `/login`, `/register`, `/changepassword`, `/unregister`
- Sistema de sesiones con memoria de IP
- Protección anti brute-force

---

## Legend
- ✨ Added: Nuevas features
- 🔄 Changed: Cambios en features existentes
- 🐛 Fixed: Bug fixes
- 🗑️ Removed: Features removidas
- 📚 Documentation: Cambios en documentación
- 🔒 Security: Parches de seguridad
