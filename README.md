# PhoenixLogin

<div align="center">
  
  ### Advanced Authentication System for Minecraft
  
  [![GitHub](https://img.shields.io/badge/GitHub-Repository-181717?style=for-the-badge&logo=github)](https://github.com/tomas2193xd-arch/PhoenixLogin)
  [![YouTube](https://img.shields.io/badge/YouTube-Demo-FF0000?style=for-the-badge&logo=youtube)](https://youtu.be/YVRDkWvm3n0)
  [![License](https://img.shields.io/badge/License-MIT-green?style=for-the-badge)](LICENSE)
  
  **Protect your Minecraft server with next-generation authentication**
</div>

---

## 📋 About

**PhoenixLogin** is a powerful authentication plugin designed to protect your Minecraft server against unauthorized access, bots, and cracked accounts. With BCrypt encryption, anti-bot captcha system, immersive visual effects, and a unique void authentication world, PhoenixLogin ensures maximum security for your server.

## ✨ Key Features

### 🔐 Complete Authentication System
- **Secure Registration & Login** with BCrypt password encryption
- **Two-Factor Authentication (2FA)** with Discord integration
- **Login History Tracking** with detailed analytics
- **Session Management** with configurable IP memory
- **Brute-Force Protection** with temporary account lockout
- **In-Game Password Management** (change/delete accounts)
- **Customizable Password Requirements** (length, uppercase, numbers, special chars)
- **Auto-Kick** for unauthenticated players after timeout

### 🤖 Advanced Anti-Bot Protection
- **Item Captcha System**: Players must place a specific item in a designated slot
- **Map Captcha System**: Professional map-based verification
- **Math Captcha** (coming soon): Solve simple math operations
- **Configurable Difficulty**: Adjust to your server's needs
- **Effective Bot Prevention**: Stop automated attacks

### 🌍 Unique Void Authentication World
- **Isolated Login Environment**: Players authenticate in a separate void world
- **Privacy Protection**: Unauthenticated players can't see your server
- **Automatic World Generation**: No manual setup required
- **Smart Teleportation**: Returns to spawn or previous location after login

### 🎭 Immersive Visual Effects
- **Boss Bar**: Real-time authentication status indicator
- **Titles & Subtitles**: Eye-catching instructions and messages
- **Sound Effects**: Audio feedback for login, register, and errors
- **Particle Effects**: Visual confirmations and error indicators
- **Login Music System**: 
  - Vanilla Minecraft sound support
  - Custom .nbs file support (requires NoteBlockAPI)

### 🛡️ Total Player Protection
- Movement blocking until authentication
- Interaction prevention (break, place, inventory, etc.)
- Damage immunity (received and dealt)
- Command restriction
- Complete player freeze for unauthenticated users

### 💾 Flexible Storage
- **SQLite** support (default, zero configuration)
- **MySQL/MariaDB** support with connection pooling
- **Asynchronous Operations** to prevent server lag
- **Automatic Schema Migration**

### 🌐 Multi-Language Support
- Fully translatable message system
- Spanish included by default
- Easy language file customization

### ⚙️ Highly Configurable
- 50+ configuration options
- Fine-tune every aspect of the plugin
- Well-documented config file
- Hot-reload support (no restart needed)

## 📹 Video Demonstration

[![PhoenixLogin Demo](https://img.youtube.com/vi/YVRDkWvm3n0/maxresdefault.jpg)](https://youtu.be/YVRDkWvm3n0)

Click to watch the full demonstration video!

## 🚀 Installation

1. **Download** the latest `PhoenixLogin.jar` from [Releases](https://github.com/tomas2193xd-arch/PhoenixLogin/releases)
2. **Place** the jar file in your server's `plugins/` folder
3. **Restart** your server or run `/reload confirm`
4. **Configure** the plugin by editing `plugins/PhoenixLogin/config.yml`
5. **(Optional)** Install [NoteBlockAPI](https://www.spigotmc.org/resources/noteblockapi.19287/) for custom music
6. **Enjoy** a secure server!

## 🎯 Commands

### Player Commands
| Command                          | Aliases                 | Description            |
| -------------------------------- | ----------------------- | ---------------------- |
| `/login <password>`              | `/l`                    | Log into your account  |
| `/register <password> <confirm>` | `/reg`                  | Register a new account |
| `/captcha <code>`                | -                       | Verify captcha code    |
| `/verify <code>`                 | -                       | Verify 2FA code        |
| `/changepassword <old> <new>`    | `/changepass`, `/cp`    | Change your password   |
| `/unregister <password>`         | -                       | Delete your account    |
| `/loginhistory [player]`         | `/lhistory`, `/history` | View login history     |

### Admin Commands
| Command         | Aliases          | Description              |
| --------------- | ---------------- | ------------------------ |
| `/phoenixlogin` | `/plogin`, `/pl` | Main admin command       |
| `/setspawn`     | -                | Set login spawn location |

## 🔑 Permissions

| Permission              | Description                     | Default |
| ----------------------- | ------------------------------- | ------- |
| `phoenixlogin.admin`    | Access to admin commands        | op      |
| `phoenixlogin.setspawn` | Set login spawn location        | op      |
| `phoenixlogin.bypass`   | Bypass login requirement        | false   |
| `phoenixlogin.premium`  | Auto-login for premium accounts | false   |

## 🔧 Requirements

- **Minecraft**: 1.19 or higher
- **Server Software**: Spigot, Paper, Purpur, or any compatible fork
- **Java**: 17 or higher
- **Optional**: [NoteBlockAPI](https://www.spigotmc.org/resources/noteblockapi.19287/) for custom music

## ⚙️ Configuration Highlights

### Security Settings
```yaml
security:
  password:
    min-length: 4              # Minimum password length
    max-length: 32             # Maximum password length
    require-uppercase: false   # Require uppercase letters
    require-numbers: false     # Require numbers
    require-special: false     # Require special characters
  max-login-attempts: 3        # Maximum login attempts
  lockout-duration: 300        # Lockout duration (seconds)
  sessions:
    enabled: true              # Session system
    duration: 60               # Session duration (seconds)
    remember-ip: true          # Remember player IP
```

### Captcha System
```yaml
captcha:
  enabled: true                # Enable anti-bot system
  type: "ITEM"                 # ITEM or MATH
  item:
    required-item: "EMERALD"   # Required item
    target-slot: 4             # Slot to place it in
```

### Void World
```yaml
void-world:
  enabled: true                       # Use void world for authentication
  world-name: "phoenixlogin_void"     # Void world name
  fallback-to-spawn: true             # Fallback if world fails
```

### Two-Factor Authentication (2FA)
```yaml
two-factor:
  enabled: false                      # Enable 2FA system
  discord-webhook: ""                 # Discord webhook URL for codes
  require-for-new-ips: true          # Require 2FA when logging from new IP
  require-for-all: false             # Always require 2FA (more secure)
```

**How to set up 2FA with Discord:**

1. **Create a Discord Webhook:**
   - Go to your Discord server
   - Edit a channel → Integrations → Webhooks
   - Click "New Webhook"
   - Copy the Webhook URL

2. **Configure PhoenixLogin:**
   ```yaml
   two-factor:
     enabled: true
     discord-webhook: "https://discord.com/api/webhooks/YOUR_WEBHOOK_URL"
     require-for-new-ips: true
   ```

3. **How it works:**
   - Player logs in from a new IP → Plugin sends  6-digit code to Discord
   - Player receives code in Discord
   - Player uses `/verify <code>` in-game
   - Code expires in 2 minutes
   - After verification, login completes successfully

4. **Modes:**
   - `require-for-new-ips: true` - Only asks 2FA on new IPs (recommended)
   - `require-for-all: true` - Always requires 2FA (maximum security)

### Login History
The plugin automatically tracks all login attempts. Players can view their history with:
- `/loginhistory` - View your own login history
- `/loginhistory <player>` - Admins can view any player's history

History shows:
- ✓/✗ Success or failure status
- Date and time
- IP address
- Authentication method

## 🛠️ Building from Source

```bash
# Clone the repository
git clone https://github.com/tomas2193xd-arch/PhoenixLogin.git
cd PhoenixLogin

# Build with Maven
mvn clean package

# The compiled jar will be in target/PhoenixLogin-<version>.jar
```

## 🐛 Bug Reports & Support

Found a bug or need help? 

- **GitHub Issues**: [Report here](https://github.com/tomas2193xd-arch/PhoenixLogin/issues)
- **Discord**: Tomas2193

## 💡 Planned Features

- 🔐 Two-factor authentication (2FA)
- 🌐 Discord webhook integration
- 👤 Premium account auto-login
- 📊 Advanced statistics and logs
- 🔍 Suspicious IP filtering
- 📧 Email password recovery
- 🎨 GUI administration panel

## 🌟 Why PhoenixLogin?

✅ **Open Source**: Review and contribute to the code  
✅ **Frequent Updates**: Regular improvements and bug fixes  
✅ **Optimized Performance**: Asynchronous operations, zero lag  
✅ **Easy to Use**: Intuitive configuration and setup  
✅ **Security First**: Industry-standard encryption and protection  
✅ **100% Free**: No premium features, no limitations  

## 📄 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## 💖 Credits

**Developer**: TomLe (Tomas2193)  
**Version**: 1.4.0  
**Release Date**: December 5, 2025

---

<div align="center">
  
  **If you like this plugin, give it a ⭐ on GitHub!**
  
  Made with ❤️ for the Minecraft community
  
</div>
