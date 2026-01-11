# 🔐 Guía Completa de 2FA con Discord - PhoenixLogin

Esta guía te explica paso a paso cómo configurar y usar el sistema de autenticación de dos factores (2FA) con Discord en PhoenixLogin.

---

## 📋 **¿Qué es 2FA?**

La autenticación de dos factores (2FA) añade una capa extra de seguridad al sistema de login. Cuando está activado:

1. El jugador ingresa su contraseña normalmente con `/login <contraseña>`
2. El plugin genera un código de 6 dígitos
3. El código se envía automáticamente a tu servidor de Discord
4. El jugador debe ingresar el código con `/verify <código>`
5. Solo después de verificar el código, el login se completa

**Beneficios:**
- ✅ Protección contra robo de contraseñas
- ✅ Seguridad extra en cuentas importantes
- ✅ Notificaciones inmediatas de intentos de acceso
- ✅ Historial de accesos en Discord

---

## 🚀 **Configuración Paso a Paso**

### **PASO 1: Crear un Webhook de Discord**

1. **Abre tu servidor de Discord**
   - Ve al servidor donde quieres recibir los códigos 2FA

2. **Selecciona un canal**
   - Elige un canal privado (solo admins deberían verlo)
   - Ejemplo: `#phoenix-2fa` o `#seguridad`

3. **Abre la configuración del canal**
   - Click derecho en el canal → Editar Canal
   - Ve a la sección "Integraciones"
   - Click en "Webhooks"

4. **Crea un nuevo Webhook**
   - Click en "Nuevo Webhook"
   - Ponle un nombre: `PhoenixLogin 2FA`
   - (Opcional) Cambia el avatar
   - **COPIA LA URL DEL WEBHOOK** - la necesitarás en el siguiente paso

5. **Guarda los cambios**

**Ejemplo de URL de Webhook:**
```
https://discord.com/api/webhooks/123456789012345678/AbCdEfGhIjKlMnOpQrStUvWxYz1234567890
```

---

### **PASO 2: Configurar PhoenixLogin**

1. **Abre el archivo de configuración**
   - Ruta: `plugins/PhoenixLogin/config.yml`

2. **Busca la sección `two-factor`**
   ```yaml
   two-factor:
     enabled: false
     discord-webhook: ""
     require-for-new-ips: true
     require-for-all: false
   ```

3. **Activa el 2FA y pega tu webhook**
   ```yaml
   two-factor:
     enabled: true
     discord-webhook: "https://discord.com/api/webhooks/TU_WEBHOOK_AQUI"
     require-for-new-ips: true
     require-for-all: false
   ```

4. **Guarda el archivo**

5. **Reacarga la configuración**
   - Usa el comando: `/phoenixlogin reload`
   - O reinicia el servidor

---

### **PASO 3: Configurar los Modos de 2FA**

Tienes dos opciones principales:

#### **Opción A: 2FA solo en IPs nuevas** (Recomendado)
```yaml
two-factor:
  enabled: true
  discord-webhook: "TU_WEBHOOK"
  require-for-new-ips: true   # ✅ ACTIVADO
  require-for-all: false      # ❌ DESACTIVADO
```

**¿Cuándo se pide 2FA?**
- Primera vez que el jugador se conecta
- Cuando el jugador se conecta desde una IP diferente
- Después de 24 horas sin conectarse

**Ventajas:**
- ✅ Balance perfecto entre seguridad y comodidad
- ✅ No molesta a jugadores que juegan desde casa
- ✅ Protege contra accesos desde otras ubicaciones

---

#### **Opción B: 2FA siempre** (Máxima Seguridad)
```yaml
two-factor:
  enabled: true
  discord-webhook: "TU_WEBHOOK"
  require-for-new-ips: false  # ❌ DESACTIVADO
  require-for-all: true       # ✅ ACTIVADO
```

**¿Cuándo se pide 2FA?**
- SIEMPRE, en cada login sin excepción

**Ventajas:**
- ✅ Máxima seguridad posible
- ✅ Ideal para servers con economía o items valiosos
- ✅ Protección total contra bots

**Desventajas:**
- ⚠️ Puede ser tedioso para jugadores frecuentes
- ⚠️ Requiere que todos los jugadores tengan Discord

---

## 💻 **Uso del Sistema 2FA**

### **Flujo Normal de Login con 2FA:**

1. **El jugador entra al servidor**
   ```
   Jugador conectado: Steve
   IP: 192.168.1.100 (Nueva IP detectada)
   ```

2. **El jugador ingresa su contraseña**
   ```
   /login MiContraseña123
   ```

3. **El sistema detecta que necesita 2FA**
   ```
   ✓ Contraseña correcta
   ⚠ Se requiere verificación 2FA
   📨 Código enviado a Discord
   📝 Usa: /verify <código>
   ```

4. **El código llega a Discord**
   ```
   🔐 PhoenixLogin - 2FA Verification
   Player: Steve
   Code: 847392
   
   This code expires in 2 minutes.
   ```

5. **El jugador ingresa el código**
   ```
   /verify 847392
   ```

6. **Login completado**
   ```
   ✓ Verificación 2FA completada exitosamente!
   ¡Bienvenido de vuelta Steve!
   ```

---

## 🔍 **Mensajes del Sistema**

### **Mensajes para el Jugador:**

| Situación | Mensaje |
|-----------|---------|
| Código correcto | `✓ Verificación 2FA completada exitosamente!` |
| Código incorrecto | `✗ Código de verificación incorrecto o expirado.` |
| Sin código pendiente | `No tienes ninguna verificación pendiente.` |
| Código expirado | `✗ Código de verificación incorrecto o expirado.` |

### **Mensajes en Discord:**

El mensaje que llega a Discord tiene este formato:
```
🔐 PhoenixLogin - 2FA Verification

Player: NombreJugador
Code: 123456

This code expires in 2 minutes.
```

---

## ⏱️ **Expiración de Códigos**

- ⏰ **Duración:** 2 minutos (120 segundos)
- 🔄 **Después de expirar:** El jugador debe volver a hacer `/login`
- ♻️ **Códigos nuevos:** Cada login genera un código único diferente

---

## 🛡️ **Seguridad y Mejores Prácticas**

### ✅ **Recomendaciones:**

1. **Canal Privado:**
   - Crea un canal de Discord solo para admins
   - Solo los staff deben ver los códigos 2FA

2. **Webhook Secreto:**
   - NO compartas la URL del webhook
   - Si se filtra, bórralo y crea uno nuevo

3. **Modo Recomendado:**
   - Usa `require-for-new-ips: true` para balance
   - Solo activa `require-for-all: true` si es necesario

4. **Permisos de Discord:**
   - Solo admins deben poder ver el canal de 2FA
   - Configura los permisos del canal correctamente

### ❌ **Errores Comunes:**

1. **"No se envió el código a Discord"**
   - Verifica que la URL del webhook esté correcta
   - Asegúrate de que el webhook no fue borrado
   - Comprueba que el bot tenga permisos en el canal

2. **"Código siempre incorrecto"**
   - Verifica que no haya espacios extras
   - El código debe ser exactamente 6 dígitos
   - Comprueba que no haya expirado (2 minutos)

3. **"No me pide 2FA"**
   - Verifica que `enabled: true` en config.yml
   - Haz `/phoenixlogin reload` después de cambiar config
   - Comprueba que tengas un webhook configurado

---

## 🔧 **Troubleshooting**

### Problema: Los códigos no llegan a Discord

**Solución:**
1. Verifica la URL del webhook en `config.yml`
2. Asegúrate de que el webhook no fue borrado
3. Comprueba los logs del servidor: `plugins/PhoenixLogin/logs/`
4. Intenta crear un webhook nuevo

### Problema: El jugador se queda bloqueado

**Solución como Admin:**
```
/phoenixlogin unregister NombreJugador
```
El jugador tendrá que registrarse de nuevo.

### Problema: Quiero desactivar 2FA temporalmente

**Solución:**
```yaml
two-factor:
  enabled: false  # Cambia esto a false
```
Luego: `/phoenixlogin reload`

---

## 📊 **Monitore la Seguridad**

Usa el comando de historial para ver intentos de login:
```
/loginhistory NombreJugador
```

Muestra:
- ✓ Logins exitosos
- ✗ Intentos fallidos
- IP addresses
- Fechas y horas

---

## 🎯 **Ejemplo Completo de Configuración**

```yaml
# config.yml - Configuración de 2FA

two-factor:
  # Activar sistema 2FA
  enabled: true
  
  # Tu webhook de Discord
  discord-webhook: "https://discord.com/api/webhooks/123...ABC"
  
  # Solo pedir 2FA en IPs nuevas (RECOMENDADO)
  require-for-new-ips: true
  
  # NUNCA pedir 2FA siempre (dejar en false)
  require-for-all: false
```

---

## ❓ **FAQ - Preguntas Frecuentes**

**Q: ¿El 2FA es obligatorio?**
A: No, es opcional. Configúralo con `enabled: false` si no lo quieres.

**Q: ¿Los códigos se guardan?**
A: No, los códigos expiran en 2 minutos y se eliminan. Son de un solo uso.

**Q: ¿Puedo usar otro método además de Discord?**
A: En v1.4.0 solo está Discord. Telegram y email vienen en futuras versiones.

**Q: ¿Funciona sin Discord?**
A: No, necesitas un servidor de Discord y un webhook configurado.

**Q: ¿Afecta el rendimiento del servidor?**
A: No, el envío de webhooks es asíncrono y no causa lag.

---

## 📞 **Soporte**

¿Problemas configurando el 2FA?

- 🐛 **Reporta bugs:** [GitHub Issues](https://github.com/tomas2193xd-arch/PhoenixLogin/issues)
- 💬 **Discord:** Tomas2193
- 📧 **Email:** [Contacto]

---

**✅ ¡Listo! Tu servidor ahora tiene autenticación de dos factores con Discord.**

---

*Guía creada para PhoenixLogin v1.4.0*  
*Última actualización: 5 de Diciembre, 2025*
