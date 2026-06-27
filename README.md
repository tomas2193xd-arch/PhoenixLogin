# 🦅 PhoenixLogin - Advanced Authentication Plugin
### Developed by Tomas2193

![Version](https://img.shields.io/badge/version-2.0-blue) ![Spigot](https://img.shields.io/badge/platform-spigot-orange) ![License](https://img.shields.io/badge/license-MIT-green)

**PhoenixLogin** is the ultimate authentication solution for Minecraft servers. Designed from the ground up to be secure, fast, and highly customizable.

---

## 🔥 What's New in Version 2.0 "Super Update"

This update completely transforms the user and administrator experience.

### 🎨 Visual Experience and UX
* **Total Immersion**: Players who are logging in **do not see other players** and vice versa. This eliminates visual chaos at spawn.
* **Clean Chat**: An intelligent system that hides global chat from unauthenticated users. You will only see what is important.
* **Customizable Titles**: Configure the welcome and success messages directly from the `config.yml`.
* **Premium Effects**: Fully customizable sounds, particles, and BossBars.

### 🛡️ Military-Grade Security
* **Robust Database**:
    * Password hashing with **BCrypt** (Industry standard).
    * Support for **SQLite** (local, zero configuration) and **MySQL** (for large networks).
    * Optimized Connection Pool with **HikariCP** for extreme performance.
* **Protection**:
    * Login attempt limits.
    * Temporary account lockouts.
    * Password filtering in console logs.

### 🧩 Key Features
* **Smart Captcha**: If a user types the command incorrectly, the system politely suggests how to use it instead of throwing red errors.
* **Session System**: Remembers players' IPs so they will not have to log in every time they join (configurable).
* **Void World**: A dedicated empty world for loading and logging in, optimizing the performance of the main lobby.
* **Spanish Configuration**: A massive `config.yml` file with detailed explanations in Spanish.

---

## 🚀 Installation

1. Download the plugin.
2. Place it in the `plugins/` folder of your server.
3. Start the server.
4. Enjoy! The default configuration is already optimal.

## ⚙️ Commands

| Command | Description | Permission |
|---|---|---|
| `/login <pass>` | Log in | N/A |
| `/register <pass> <pass>` | Register | N/A |
| `/captcha <code>` | Verify captcha | `phoenixlogin.captcha` |
| `/changepassword` | Change password | N/A |
| `/phoenixlogin` | Admin command | `phoenixlogin.admin` |

## 👨‍💻 Author

**Tomas2193** - *Creator and Lead Developer*

> "Making plugins that just work."
