# PhoenixLogin v1.4.0 - Security & Experience Update 🚀

This update focuses on polishing the user experience and hardening security based on server owner feedback.

### 🛡️ **Critical Security Improvements**
- **Console Privacy**: Authentication commands (`/login`, `/register`, etc.) now strictly filter passwords from the server console logs. Your players' passwords are safe even from console viewers.
- **Mandatory Captcha**: The Anti-Bot Captcha is now **mandatory for ALL players** (new and registered) before they can authenticate. This prevents automated login attacks on existing accounts.
- **Strict Registration**: The `/register` command is now completely blocked until the captcha is solved.

### 💎 **Visual & Experience Changes**
- **On-Screen Captcha Instructions**: Replaced chat spam with a clean **Title & Subtitle** display instructing players to look at the map over a 10-second duration.
- **Clean Console Startup**: Significantly reduced console noise during startup. You now get a beautiful, clean ASCII banner and a concise "Statistics Dashboard" summary. No more spam of debug info.
- **NBS Music Fixes**: Improved detection for NoteBlockAPI. If you have NBS music files but forgot to enable them or lack the dependency, the console will now guide you instead of silently failing or playing vanilla sounds without explanation.

### 🔧 **Bug Fixes**
- Fixed an issue where `use-nbs: false` in default config could override user settings during updates (logic improved).
- Fixed internal logging where some warnings were too aggressive.
- Optimized inventory caching/restoration flow.

---

**Upgrading from 1.3.x?**
1. Stop your server.
2. Replace `PhoenixLogin.jar`.
3. Start your server.
4. *Optional*: Check `config.yml` if you use custom music. 

**Enjoy the update! ⭐**
