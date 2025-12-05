# 🎵 PhoenixLogin - NoteBlock Music System (NBS)

## ✅ IMPLEMENTACIÓN COMPLETA

El plugin **PhoenixLogin** ahora soporta música personalizada usando archivos **`.nbs`** (Note Block Studio) gracias a **NoteBlockAPI**.

---

## 🎯 ¿Qué es esto?

Con NoteBlock music puedes reproducir **CANCIONES REALES** en Minecraft durante el login:
- 🎵 Cualquier canción convertida a note blocks
- 🎼 Calidad profesional
- 🔁 Loop automático
- 🚀 Sin lag

---

## 📋 Requisitos

### 1. **NoteBlockAPI** (Plugin)
Primero necesitas instalar **NoteBlockAPI** en tu servidor:

**Descargar**: https://www.spigotmc.org/resources/noteblockapi.19287/

1. Descarga `NoteBlockAPI-1.6.2.jar`
2. Colócalo en `plugins/`
3. Reinicia el servidor

---

## 🎼 Obteniendo Archivos .nbs

Tienes 3 opciones:

### **Opción 1: Descargar canciones ya hechas** ⭐ RECOMENDADO

#### **🌐 Sitios con .nbs listos**:

1. **OpenNBS** (Oficial)
   - https://opennbs.org/
   - Tiene biblioteca de canciones

2. **Minecraft Note Block Community**
   - https://www.minecraft-noteblock.com/
   - Miles de canciones populares

3. **YouTube "NBS download"**
   - Muchos creadores suben .nbs files

#### **🔥 Canciones Populares para Login**:
- **"Sweden" (C418)** - Clásica de Minecraft
- **"Wet Hands" (C418)** - Tranquila
- **"Aria Math" (C418)** - Espacial
- **"Minecraft Theme" (Calm 1, 2, 3)**
- **"Undertale - Megalovania"** - Épico
- **"Terraria - Boss 2"** - Intenso
- **Música Lofi** - Relajante

---

### **Opción 2: Crear tu propia música**

**Usa Note Block Studio**:
- https://opennbs.org/
- Programa GRATIS para Windows/Mac/Linux
- Arrastra MIDI files y conviértelos a .nbs
- O crea desde cero

---

###  **Opción 3: Convertir MIDI a NBS**

1. Consigue archivo MIDI de tu canción favorita
2. Abre Note Block Studio
3. File → Import → MIDI
4. Ajusta y exporta como .nbs

---

## 📁 Instalación de .nbs

### **Paso 1: Crear carpeta de música**
```
plugins/PhoenixLogin/music/
```

### **Paso 2: Copiar tu archivo .nbs**
Ejemplo:
```
plugins/PhoenixLogin/music/login.nbs
plugins/PhoenixLogin/music/sweden.nbs
plugins/PhoenixLogin/music/wet_hands.nbs
```

### **Paso 3: Configurar**
Edita `plugins/PhoenixLogin/config.yml`:

```yaml
login-music:
  enabled: true
  
  # Activar modo NBS
  use-nbs: true     # ← Cambia esto a true
  
  # Nombre del archivo (en plugins/PhoenixLogin/music/)
  nbs-file: "login.nbs"    # ← Tu archivo
  
  # Volumen (0.0 - 1.0)
  nbs-volume: 1.0
```

### **Paso 4: Reiniciar**
```
/stop
```

---

## ⚙️ Configuración

### **Config completo**:
```yaml
login-music:
  enabled: true
  
  # OPCIÓN 1: Sonidos Vanilla (No requiere NoteBlockAPI)
  use-nbs: false
  sound: "MUSIC_DISC_CAT"
  volume: 0.3
  pitch: 1.0
  loop-interval: 100
  
  # OPCIÓN 2: Archivos NBS (Requiere NoteBlockAPI)
  use-nbs: true              # ← Activar NBS
  nbs-file: "sweden.nbs"     # ← Nombre del archivo
  nbs-volume: 1.0            # ← Volumen
```

---

## 🔍 Verificación

### **Logs al iniciar el servidor**:

#### ✅ **Si todo está bien**:
```
[PhoenixLogin] NoteBlockAPI detected! NBS music support enabled.
[PhoenixLogin] Started NBS music for TomLe: Sweden
```

#### ⚠️ **Si falta NoteBlockAPI**:
```
[PhoenixLogin] NoteBlockAPI not found. Using vanilla sounds only.
[PhoenixLogin] To enable NBS music, install NoteBlockAPI: https://www.spigotmc.org/resources/noteblockapi.19287/
```

#### ⚠️ **Si falta el archivo .nbs**:
```
[PhoenixLogin] NBS file not found: plugins/PhoenixLogin/music/login.nbs
[PhoenixLogin] Create the music folder: plugins/PhoenixLogin/music/
[PhoenixLogin] Falling back to vanilla music...
```

---

## 🎮 Ejemplo Completo

### **Configuración con "Sweden"**:

1. **Descargar "Sweden.nbs"**:
   - Busca en Google: "minecraft sweden nbs download"
   - O usa Note Block Studio para crearlo

2. **Copiar**:
   ```
   plugins/PhoenixLogin/music/sweden.nbs
   ```

3. **Config**:
   ```yaml
   login-music:
     enabled: true
     use-nbs: true
     nbs-file: "sweden.nbs"
     nbs-volume: 0.8
   ```

4. **Reiniciar**

5. **¡Disfrutar!**
   - Los jugadores escucharán "Sweden" en el VoidAuthWorld
   - Se detiene automáticamente al hacer login

---

## 🎵 Recomendaciones de Canciones

### **Para Login Screen (Tranquilas)**:
- ⭐ **Sweden (C418)** - LA canción de Minecraft
- ⭐ **Wet Hands (C418)** - Tranquila, nostálgica
- **Minecraft (C418)** - Clásica
- **Haggstrom (C418)** - Relajante
- **Subwoofer Lullaby (C418)** - Ambient

### **Para ambiente épico**:
- **Aria Math (C418)** - Espacial, épica
- **Undertale - Megalovania**
- **Terraria - Boss 2**
- **Zelda - Song of Time**

### **Para ambiente misterioso**:
- **Stranger Things Theme**
- **Lavender Town (Pokemon)**
- **Undertale - Ruins**

---

## 🐛 Troubleshooting

### **"La música no suena"**
1. ✅ Verifica que NoteBlockAPI esté instalado
2. ✅ Verifica que `use-nbs: true`
3. ✅ Verifica que el archivo .nbs existe en `plugins/PhoenixLogin/music/`
4. ✅ Mira los logs para errores
5. ✅ Reinicia el servidor

### **"El archivo no se encuentra"**
```
Crear carpeta manualmente:
mkdir plugins/PhoenixLogin/music
```

###  **"Vuelve a vanilla sounds"**
Esto es el **fallback automático** cuando:
- NoteBlockAPI no está instalado
- El archivo .nbs no existe
- El archivo .nbs está corrupto

El plugin seguirá funcionando con sonidos vanilla.

---

## 🔄 Cambiar entre Vanilla y NBS

### **Usar Vanilla (por defecto)**:
```yaml
use-nbs: false
```

### **Usar NBS**:
```yaml
use-nbs: true
```

Puedes cambiar en cualquier momento con `/phoenixlogin reload`

---

## 💡 Tips Pro

1. **Múltiples canciones**:
   - Guarda varias en `music/`
   - Cambia `nbs-file` según el mood

2. **Volumen**:
   - `nbs-volume: 1.0` = 100%
   - `nbs-volume: 0.5` = 50%
   - Ajusta según la canción

3. **Duración**:
   - Las canciones se repiten automáticamente (loop)
   - No  necesitas configurar nada

4. **Sincronización**:
   - Todos los jugadores no autenticados escuchan la música
   - Se detiene individualmente al autenticarse

---

## 📊 Comparación

| Feature               | Vanilla Sounds | NBS Files           |
| --------------------- | -------------- | ------------------- |
| Requiere plugin extra | ❌ No           | ✅ Sí (NoteBlockAPI) |
| Calidad               | Buena          | Excelente           |
| Personalización       | Limitada       | Total               |
| Canciones reales      | ❌              | ✅                   |
| Fácil de configurar   | ✅              | ⭐ Medio             |
| Lag                   | Ninguno        | Ninguno             |

---

## 📚 Enlaces Útiles

- **NoteBlockAPI**: https://www.spigotmc.org/resources/noteblockapi.19287/
- **Note Block Studio**: https://opennbs.org/
- **OpenNBS Songs**: https://opennbs.org/songs
- **Minecraft Note Block**: https://www.minecraft-noteblock.com/
- **Tutorial NBS**: https://www.youtube.com/results?search_query=note+block+studio+tutorial

---

## ✅ Checklist de Instalación

- [ ] Instalar NoteBlockAPI en el servidor
- [ ] Reiniciar servidor
- [ ] Descargar archivo .nbs
- [ ] Crear carpeta `plugins/PhoenixLogin/music/`
- [ ] Copiar archivo .nbs a la carpeta
- [ ] Configurar `config.yml` con `use-nbs: true`
- [ ] Configurar `nbs-file` con el nombre correcto
- [ ] Reiniciar servidor
- [ ] Verificar logs
- [ ] Conectar y probar

---

**🎵 ¡Disfruta de música personalizada en tu servidor! 🔥**
