# 💎 GriefPreventionShop

Hey there! Welcome to **GriefPreventionShop**, a premium-grade addon for the GriefPrevention plugin. I created this to give server owners a super clean, high-performance way for players to buy claim blocks without messy chat commands.

This plugin isn't just a basic menu; it’s built to feel like a "Classic Studio" professional client, with smooth interactions and a centered GUI design that keeps the focus where it matters.

### 🚀 Awesome Features:
- **Premium GUI Design**: Forget generic emerald blocks. We use Lime/Red Shulker Boxes and Candles for a much sleeker visual experience.
- **Account Stats**: See your current money and claim blocks directly inside the menu (via your own skin head!).
- **Discord Webhooks**: Get live purchase alerts in your Discord, complete with the player's skin avatar on the side. 
- **Transaction History**: A dedicated `/claimhistory` menu so players can track their spending.
- **Async Performance**: Everything from webhooks to history logging is done asynchronously, so your TPS stays at a solid 20.0.
- **Custom Sounds**: Satisfying level-up sounds for success and a villager-hurt sound for failed transactions.

### 🛡️ Permissions & Commands:
- **/gpshop** - Opens the main Treasury menu.
  - `griefpreventionshop.use`
- **/claimhistory** - Opens your personal purchase logs.
  - `griefpreventionshop.history`
- **/gpshop reload** - Reloads absolutely everything (configs, menus, sounds).
  - `griefpreventionshop.admin`

### ⚙️ Quick Setup:
1. Drop the jar in your plugins folder.
2. Make sure you have **Vault** and **GriefPrevention** (obviously!).
3. Adjust your `price-per-block` and `currency-symbol` in `config.yml`.
4. (Optional) Put your Discord Webhook URL in `webhook.yml` to get those sweet notifications.

---
*Made with ❤️ by Swagger Studio. If you find any bugs, just hit me up on Discord!*
