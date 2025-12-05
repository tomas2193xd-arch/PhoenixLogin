# 🔥 PhoenixLogin

[![Version](https://img.shields.io/badge/version-1.0.0-blue.svg)](https://github.com/YourUsername/PhoenixLogin/releases)
[![Minecraft](https://img.shields.io/badge/minecraft-1.19.4+-green.svg)](https://www.spigotmc.org/resources/phoenixlogin.XXXXX/)
[![License](https://img.shields.io/badge/license-MIT-yellow.svg)](LICENSE)
[![SpigotMC](https://img.shields.io/badge/spigot-download-orange.svg)](https://www.spigotmc.org/resources/phoenixlogin.XXXXX/)

**Advanced Authentication System for Minecraft Servers**

PhoenixLogin is a premium, feature-rich authentication plugin that provides maximum security with an innovative VoidAuthWorld system, advanced captcha, customizable music, and multi-language support.

---

## 🌟 Features

- 🛡️ **VoidAuthWorld** - Isolated void dimension for authentication
- 🔐 **BCrypt Encryption** - Industry-standard password hashing
- 🤖 **Anti-Bot Captcha** - Item and Math captcha systems
- 🎵 **Custom Music** - Vanilla sounds or NBS files support
- 🎨 **Visual Effects** - Boss bars, titles, particles, sounds
- 🌍 **Multi-Language** - Spanish & English (easily expandable)
- 💾 **Dual Database** - SQLite (default) or MySQL
- ⚡ **High Performance** - Async operations, zero lag
- 🛡️ **Brute-Force Protection** - Auto-lockout system
- 📊 **Session Management** - IP-based trusted sessions

---

## 📸 Screenshots

[Add screenshots here]

---

## 📦 Installation

### Requirements
- Minecraft Server: Spigot/Paper 1.19.4+
- Java 17 or higher

### Steps
1. Download the latest release from [Releases](https://github.com/YourUsername/PhoenixLogin/releases) or [SpigotMC](https://www.spigotmc.org/resources/phoenixlogin.XXXXX/)
2. Place `PhoenixLogin-1.0.0.jar` in your `plugins/` folder
3. **(Optional)** Install [NoteBlockAPI](https://www.spigotmc.org/resources/noteblockapi.19287/) for NBS music support
4. Restart your server
5. Configure `plugins/PhoenixLogin/config.yml`
6. Enjoy! 🎉

---

## 📖 Documentation

- [Configuration Guide](https://github.com/YourUsername/PhoenixLogin/wiki/Configuration)
- [Commands & Permissions](https://github.com/YourUsername/PhoenixLogin/wiki/Commands)
- [VoidAuthWorld System](VOIDAUTHWORLD.md)
- [Music System (NBS)](NBS_MUSIC_GUIDE.md)
- [Spawn System](SPAWN_SYSTEM.md)
- [Development Guide](DEVELOPMENT.md)

---

## 🎮 Quick Start

### For Players
```
New player:
1. Connect to server → Go to VoidAuthWorld
2. Complete captcha (if enabled)
3. /register <password> <password>
4. Done! You're in!

Returning player:
1. Connect → Go to VoidAuthWorld
2. /login <password>
3. Welcome back!
```

### For Admins
```
Set a spawn location:
/setspawn

View player info:
/phoenixlogin info <player>

Reload config:
/phoenixlogin reload
```

---

## 🔧 Development

### Building from Source

```bash
# Clone the repository
git clone https://github.com/YourUsername/PhoenixLogin.git
cd PhoenixLogin

# Build with Maven
mvn clean package

# Output: target/PhoenixLogin-1.0.0.jar
```

### Project Structure
```
PhoenixLogin/
├── src/main/
│   ├── java/dev/tomle/phoenixlogin/
│   │   ├── PhoenixLogin.java
│   │   ├── command/
│   │   ├── listener/
│   │   └── manager/
│   └── resources/
│       ├── config.yml
│       ├── messages_es.yml
│       ├── messages_en.yml
│       └── plugin.yml
├── target/
└── pom.xml
```

---

## 🤝 Contributing

Contributions are welcome! Please feel free to submit a Pull Request.

1. Fork the repository
2. Create your feature branch (`git checkout -b feature/AmazingFeature`)
3. Commit your changes (`git commit -m 'Add some AmazingFeature'`)
4. Push to the branch (`git push origin feature/AmazingFeature`)
5. Open a Pull Request

---

## 🐛 Bug Reports

Found a bug? Please [open an issue](https://github.com/YourUsername/PhoenixLogin/issues) with:
- Plugin version
- Server version (Spigot/Paper)
- Minecraft version
- Detailed description
- Error logs (if applicable)

---

## 📄 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

---

## 💖 Support

If you like this project:
- ⭐ Star this repository
- 🐛 Report bugs
- 💡 Suggest features
- 📢 Share with others

---

## 🚀 Roadmap

- [ ] 2FA with Discord
- [ ] Email verification & recovery
- [ ] GUI-based authentication
- [ ] BungeeCord/Velocity support
- [ ] Admin panel (GUI)
- [ ] Geographic IP detection
- [ ] More captcha types

---

## 👨‍💻 Author

**TomLe**

- GitHub: [@YourUsername](https://github.com/YourUsername)
- SpigotMC: [Profile](https://www.spigotmc.org/members/yourprofile.XXXXX/)

---

## 🙏 Acknowledgments

- Adventure API for modern messaging
- HikariCP for MySQL pooling
- BCrypt for password hashing
- NoteBlockAPI for NBS music support
- All contributors and testers

---

**Made with ❤️ for the Minecraft community**
