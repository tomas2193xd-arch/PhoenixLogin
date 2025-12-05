# 🔥 PhoenixLogin - Advanced Authentication System

![Version](https://img.shields.io/badge/version-1.0.0-blue.svg)
![Minecraft](https://img.shields.io/badge/minecraft-1.19.4+-green.svg)
![License](https://img.shields.io/badge/license-MIT-yellow.svg)

---

## 📖 Description

**PhoenixLogin** is a premium, feature-rich authentication plugin that provides maximum security for your Minecraft server. With an innovative **VoidAuthWorld** system, advanced captcha, customizable music, and multi-language support, PhoenixLogin offers a professional and immersive login experience.

---

## ✨ Key Features

### 🛡️ **VoidAuthWorld System**
- Players spawn in a completely empty void world before authentication
- Maximum security - prevents server exploration before login
- Automatic location saving and restoration
- Smart safe-spawn detection

### 🔐 **Advanced Security**
- **BCrypt Password Hashing** - Industry-standard encryption
- **Brute-Force Protection** - Automatic account lockout after failed attempts
- **IP-based Sessions** - Remember trusted devices
- **Password Requirements** - Configurable complexity rules
- **Comprehensive Logging** - Track all authentication attempts

### 🤖 **Anti-Bot Captcha**
- **Item Captcha** - Place specific item in slot
- **Math Captcha** - Solve simple math problems
- **Configurable Difficulty** - Adjust for your needs
- Prevents automated attacks

### 🎵 **Immersive Music System**
- **Dual Mode**: Vanilla sounds OR custom NBS files
- **NoteBlockAPI Integration** - Play real music during login
- **Fully Configurable** - Choose your own sounds
- **Auto-stop** - Music ends upon authentication

### 🎨 **Visual Effects**
- **Boss Bars** - Visual countdown timer
- **Titles & Subtitles** - Custom messages
- **Particles** - Visual feedback for actions
- **Sounds** - Audio cues for events

### 🌍 **Multi-Language**
- **Spanish & English** included
- Easy to translate - YAML format
- Instant language switching
- No restart required

### 💾 **Database Support**
- **SQLite** - Works out-of-the-box, zero configuration
- **MySQL** - Full support with HikariCP connection pooling
- **Async Operations** - No server lag

### ⚙️ **Highly Configurable**
- 40+ configuration options
- Customize every aspect
- Enable/disable any feature
- Perfect for any setup

---

## 📸 Screenshots

[INSERT SCREENSHOTS HERE]
- VoidAuthWorld experience
- Captcha system
- Login flow
- Admin commands

---

## 🎮 Commands

### Player Commands
- `/register <password> <confirm>` - Register a new account
- `/login <password>` - Login to your account
- `/changepassword <old> <new>` - Change your password
- `/unregister <password>` - Delete your account

### Admin Commands
- `/phoenixlogin reload` - Reload configuration
- `/phoenixlogin info <player>` - View player information
- `/phoenixlogin unregister <player>` - Force unregister player
- `/phoenixlogin stats` - View plugin statistics
- `/setspawn` - Set post-login spawn location

---

## 🔐 Permissions

- `phoenixlogin.admin` - Access to all admin commands
- `phoenixlogin.bypass` - Bypass authentication system
- `phoenixlogin.premium` - Auto-login for premium accounts (requires activation)
- `phoenixlogin.setspawn` - Set spawn location

---

## ⚙️ Configuration

PhoenixLogin comes with extensive configuration options:

```yaml
# Database: SQLite or MySQL
# Security: Password rules, brute-force protection, sessions
# Captcha: Item or Math type with difficulty settings
# VoidWorld: Enable/disable, world name, fallback options
# Music: Vanilla sounds or custom NBS files
# Effects: Boss bars, titles, sounds, particles
# And much more!
```

See [Configuration Guide](https://github.com/YourUsername/PhoenixLogin/wiki/Configuration) for details.

---

## 🎵 Music System

### Vanilla Mode (Default)
- Uses built-in Minecraft sounds
- No additional plugins required
- Fully configurable

### NBS Mode (Advanced)
- Requires [NoteBlockAPI](https://www.spigotmc.org/resources/noteblockapi.19287/)
- Play custom .nbs files
- Real music during login
- Download songs from [OpenNBS](https://opennbs.org/)

---

## 📦 Installation

1. Download `PhoenixLogin-1.0.0.jar`
2. Place in your `plugins/` folder
3. **(Optional)** Download [NoteBlockAPI](https://www.spigotmc.org/resources/noteblockapi.19287/) for NBS music
4. Restart your server
5. Configure `config.yml` to your liking
6. Done! 🎉

---

## 🔧 Dependencies

**Required:**
- Spigot/Paper 1.19.4+
- Java 17+

**Optional:**
- [NoteBlockAPI](https://www.spigotmc.org/resources/noteblockapi.19287/) - For custom NBS music

---

## 📊 Tested Versions

✅ 1.19.4
✅ 1.20.x (Should work)

---

## 🐛 Bug Reports & Support

Found a bug? Have a suggestion?

- [GitHub Issues](https://github.com/YourUsername/PhoenixLogin/issues)
- [Discord Server](https://discord.gg/your-invite)
- [SpigotMC Discussion](https://www.spigotmc.org/resources/phoenixlogin.XXXXX/updates)

---

## 📄 License

PhoenixLogin is licensed under the **MIT License**.

---

## 👨‍💻 Author

Created with ❤️ by **TomLe**

- GitHub: [YourGitHub](https://github.com/YourUsername)
- Discord: YourDiscord#0000

---

## ⭐ Support the Project

If you like PhoenixLogin, please:
- ⭐ Leave a 5-star review
- 💬 Share with friends
- 🐛 Report bugs
- 💡 Suggest features

---

## 🚀 Roadmap (Future Updates)

- [ ] 2FA with Discord
- [ ] Email verification
- [ ] GUI-based login
- [ ] BungeeCord/Velocity support
- [ ] Geographic IP detection
- [ ] Admin panel GUI
- [ ] And more!

---

**Download PhoenixLogin today and secure your server! 🔥**
