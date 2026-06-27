# Changelog

All notable changes to PhoenixLogin will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

---

## [2.0.0] - 2026-06-26

### 🐛 Fixed
- **CRITICAL: GameMode not restored after login** — Players were permanently stuck in ADVENTURE mode after authenticating. Now saves and restores GameMode, allowFlight, and flying state.
- **CRITICAL: Premium auto-login on offline-mode servers** — Added clear console warning when premium is enabled on an offline-mode server. Improved error messages for players.
- **ChatBlockListener blocking /verify** — The 2FA `/verify` command was blocked for unauthenticated players. Now included in defaults and the allowed commands list is configurable via `config.yml`.
- **SimpleDateFormat thread-safety** — Replaced with `DateTimeFormatter` to prevent date corruption in async contexts.
- **Login history cleanup never executed** — `cleanupOldEntries()` was defined but never called. Now runs automatically on startup.
- **Inventory/GameMode lost on server shutdown** — Players still authenticating during a shutdown now get their inventory and state restored before the plugin disables.
- **Session restore skipped location restore** — Players with valid sessions were not teleported back from void world correctly.
- **Premium auto-login skipped music stop and bossbar removal** — Fixed incomplete post-auth flow.

### ✨ Added
- **Join message toggle** — New `settings.join-message-enabled` option in config to completely disable join messages (requested by community).
- **Configurable allowed commands** — New `login.allowed-commands` list in config to whitelist extra commands during authentication.
- **History auto-cleanup** — New `advanced.logging.history-cleanup-days` option (default: 90 days).
- **Offline-mode premium warning** — Console shows a clear warning at startup if premium is enabled but server is in offline-mode.

### 🔄 Changed
- Version unified to 2.0.0 across all files (pom.xml, plugin.yml, config.yml, messages.yml)
- Improved shutdown order — restores player state before closing database
- `LocationManager` now saves full player state (GameMode, flight, location) instead of just location

---

## [1.4.0] - 2025-12-05

### ✨ Added
- **Two-Factor Authentication (2FA)**: Complete two-factor authentication system
  - Discord Webhooks integration for sending codes
  - 6-digit codes with a 2-minute expiration
  - New `/verify <code>` command for validation
  - Flexible configuration in `config.yml`
  - Support for requiring 2FA on new IPs or always

- **Login History System**: Complete login attempt history
  - New command `/loginhistory [player]` with aliases `/lhistory` and `/history`
  - Saves date, time, IP, method, and status of each attempt
  - Admins can check the history of any player
  - Database storage with automatic cleanup
  - Shows the last 10 attempts in a professional format

- **Tab Completion**: Professional auto-completion for commands
  - TabCompleter for `/phoenixlogin` with subcommand suggestions
  - Auto-completes player names in admin commands
  - Improves user experience

- **ChatBlockListener**: Tab completion blocking system
  - Now active and properly registered
  - Blocks unauthorized commands for unauthenticated players
  - Only shows `/login`, `/register`, and their aliases

- **bStats Integration**: Anonymous metrics system
  - Allows tracking plugin usage statistics
  - Completely anonymous and privacy-respecting
  - Helps with future development and improvements

### 🔄 Changed
- API version updated from 1.2.0 to 1.4.0
- Total commands increased from 7 to 9
- Total listeners increased from 3 to 4
- Configuration system reorganized for 2FA

### 🐛 Fixed
- ChatBlockListener now registers correctly on startup
- Version consistency between plugin and API

### 📚 Documentation
- README updated with new features
- Complete 2FA configuration guide with Discord
- Usage examples for new commands

---

## [1.3.0] - 2025-12-04

### ✨ Added
- Console logging system with ASCII art and ANSI colors
- Improved startup statistics
- Registered players counter on startup
- Professional music system with NBS support

### 🔄 Changed
- Visual improvements in console
- Log messages optimization

---

## [1.2.0] - 2025-12-03

### ✨ Added
- Public API system for other plugins
- Custom events (PreLogin, Login, PreRegister, Register, LoginFailed)
- Multi-language support (English & Spanish)
- Map Captcha system
- Improved session management

### 🔄 Changed
- Message system refactoring
- Plugin architecture improvements

---

## [1.1.0] - 2025-12-02

### ✨ Added
- VoidWorld authentication system
- Location manager for post-login spawn
- Visual effects system (BossBar, Titles, Particles)
- `/setspawn` command to configure spawn

### 🐛 Fixed
- Teleportation issues after login
- Spawn system bugs

---

## [1.0.0] - 2025-12-01

### ✨ Initial Release
- Basic authentication system with BCrypt
- SQLite and MySQL support
- Item captcha
- Unauthenticated player protection
- Basic commands: `/login`, `/register`, `/changepassword`, `/unregister`
- Session system with IP memory
- Anti brute-force protection

---

## Legend
- ✨ Added: New features
- 🔄 Changed: Changes in existing features
- 🐛 Fixed: Bug fixes
- 🗑️ Removed: Removed features
- 📚 Documentation: Documentation changes
- 🔒 Security: Security patches
