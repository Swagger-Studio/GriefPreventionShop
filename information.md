# 💎 GriefPreventionShop - Release Assets

> [!IMPORTANT]
> **DO NOT PUSH THIS FILE TO GITHUB.** This is your "Master Copy" for copy-pasting into SpigotMC, MC-Market, or GitHub Releases.

---

## 🏆 Release Quick Stats
- **Release Title**: `GriefPreventionShop v1.0.0 - The Premium Release`
- **Short Description**: *Premium Claim Block Shop for GriefPrevention with Discord Notifications, Transaction History, and Professional GUI Layout.*

---

## 📦 Release Description (GitHub / SpigotMC)

**GriefPreventionShop** is a high-performance, professional-grade addon for GriefPrevention. I built this to solve the problem of clunky chat-based shops by providing a centered, high-contrast GUI that feels like a premium "Studio" client.

### ✨ Key Features:
- **🎨 Custom Aesthetic**: Using **Lime & Red Shulker Boxes** and **Candles** for a unique, identifiable look. No more boring emerald blocks!
- **👤 Dynamic Stats Head**: A real-time player head in the menu that dynamically shows your **Skin**, **Balance**, and **Current Claim Blocks**.
- **📠 Discord Notifications**: Real-time asynchronous webhook notifications. Admin can see exactly who bought how much, complete with their skin avatar.
- **📚 Interactive History**: Players can use `/claimhistory` to see their last 50 transactions in a dedicated menu.
- **🔊 Multi-Audio Feedback**: Configurable sound rewards (like Level-Up) for successful buys and error sounds (Villager Hurt) for failed ones.
- **⚙️ Symbol Standardization**: Every menu and message uses the unique **&e♯** symbol for a cohesive, hand-crafted feel.
- **⚡ Async Engine**: Webhooks, logging, and history are handled off-thread to ensure 20.0 TPS even under load.

### 🛡️ Permissions & Commands:
| Command | Permission | Description |
| :--- | :--- | :--- |
| `/gpshop` | `griefpreventionshop.use` | Opens the main Claim Shop |
| `/claimhistory` | `griefpreventionshop.history` | View your personal purchase logs |
| `/gpshop reload` | `griefpreventionshop.admin` | Reload all configs, menus, and sounds |

---

## 🛠️ Setup Guide
1.  Verify you have **Vault** and **GriefPrevention** installed.
2.  Drop the `GriefPreventionShop.jar` into your plugins folder.
3.  Configure your `price-per-block` in `config.yml`.
4.  (Optional) Add your Discord Webhook URL in `menus/webhook.yml` and enable it.
5.  Run `/gpshop reload` and you are live!

---

## 💬 Socials
- **Discord Support**: https://discord.gg/Yxq6H8cb
- **Developer**: Swagger Studio

---
*Generated for the v1.0.0 Official Release.*
