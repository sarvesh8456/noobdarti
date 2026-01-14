# Cracked Server Alt Account Fix

## Problem
On **cracked servers**, when you used `/verifiyadmin forceverify sarvesh`, the player was verified in the database. However, when they joined with a **different alt account name** (like "sarvesh2"), the system:
- Generated a different UUID for the alt account (since cracked servers use offline UUIDs based on the player name)
- Treated the alt as a completely different player
- Still asked for OTP verification and blocked it as an alt account

This happened because the verification system was purely UUID-based, but each alt account has a different UUID on a cracked server.

## Solution
Added **IP-based whitelist** functionality for cracked servers with multiple accounts:

### How It Works

#### Three Ways to Force Verify:

1. **Online Player (Auto-detects IP)**
   ```
   /verifiyadmin forceverify sarvesh
   ```
   When the player is online, their IP is automatically captured and whitelisted.

2. **Offline Player with Manual IP**
   ```
   /verifiyadmin forceverify sarvesh 192.168.1.100
   ```
   If you know the IP, provide it manually.

3. **Offline Player (Auto-retrieves Last Known IP)** ✨ NEW
   ```
   /verifiyadmin forceverify sarvesh
   ```
   If the player has **ever tried to join before** (even if kicked with OTP), their IP is automatically retrieved from the database!

#### How It Works Under the Hood

1. When you run `/verifiyadmin forceverify <playerName>`:
   - System generates the offline UUID from the player name
   - **If offline**: Queries the OTP table in database for the last request from that UUID to get their IP
   - **If online**: Captures IP directly from the player object
   - Verifies the player in the verification database
   - Whitelists their account name
   - **Whitelists their IP address** so any alt from that IP bypasses verification

2. When any account joins from a whitelisted IP:
   - The system checks the IP whitelist FIRST (happens before any OTP/alt checks)
   - If the IP is whitelisted, the player is allowed in immediately WITHOUT OTP
   - This works regardless of the account name/UUID

### Changes Made

**VerificationManager.java:**
- Added `ipWhitelist` field: A concurrent thread-safe set of whitelisted IPs
- Updated `handlePlayerJoin()`: Now checks IP whitelist before UUID verification
- Added `addIPToWhitelist(String playerIP)`: Adds an IP to the whitelist
- Added `removeIPFromWhitelist(String playerIP)`: Removes an IP from the whitelist

**DatabaseManager.java:**
- Added `getLastKnownIPByUUID(String playerUUID)`: Queries the OTP table for the player's last known IP address

**AdminCommands.java (forceverify command):**
- Now supports optional IP parameter
- Auto-detects IP from online players
- Auto-retrieves last known IP from database for offline players
- Shows detailed results including IP source and detection method

### Usage

**For online players (easiest):**
```
/verifiyadmin forceverify sarvesh
```
Output:
```
=== Force Verify Results ===
Player: sarvesh
UUID: xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx
IP Address: 192.168.1.100 (auto-detected)
Database: SUCCESS
Whitelist: SUCCESS
IP Whitelist: SUCCESS (alts from this IP will bypass verification)
```

**For offline players with manual IP:**
```
/verifiyadmin forceverify sarvesh 192.168.1.100
```
Output:
```
=== Force Verify Results ===
Player: sarvesh
UUID: xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx
IP Address: 192.168.1.100 (auto-detected)
Database: SUCCESS
Whitelist: SUCCESS
IP Whitelist: SUCCESS (alts from this IP will bypass verification)
```

**For offline players (auto-retrieves from database):**
```
/verifiyadmin forceverify sarvesh
```
If they've ever joined before:
```
=== Force Verify Results ===
Player: sarvesh
UUID: xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx
IP Address: 192.168.1.100 (auto-detected)
Database: SUCCESS
Whitelist: SUCCESS
IP Whitelist: SUCCESS (alts from this IP will bypass verification)
```

If they've never joined:
```
=== Force Verify Results ===
Player: sarvesh
UUID: xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx
IP Address: Not found (player never joined or specify with /verifyadmin forceverify <player> <IP>)
Database: SUCCESS
Whitelist: SUCCESS
⚠ No IP found - offline alts will need OTP
  To fix: /verifyadmin forceverify sarvesh <IP>
```

### Important Notes

1. **Database Storage**: IP whitelists are stored in memory only. They reset on server restart.
   - **Workaround**: Whenever someone joins, their IP is logged to the database, so you can always retrieve it with forceverify!

2. **New Players**: Brand new players who have never joined before cannot have IP auto-retrieved. You must either:
   - Wait for them to join (they'll get kicked with OTP but their IP gets logged)
   - Then run forceverify
   - OR manually provide their IP when running forceverify

3. **Changing IP**: If a player's ISP changes their IP, the old whitelist entry won't help. They'll need to:
   - Have them join (to log new IP)
   - Then run forceverify again

4. **Premium Servers**: On premium servers where each account has a unique UUID, forceverify works normally since each player is a separate entity. The IP whitelist is ignored for premium servers.

## Testing the Fix

1. Player with alt account tries to join (gets OTP kick)
2. Admin runs: `/verifiyadmin forceverify playername`
3. Player's IP is automatically detected from the database
4. Player joins with any alt account
5. ✅ Alt account bypasses OTP and joins successfully!

## Future Improvements
- Add database persistence for IP whitelist (survives server restarts)
- Add command to manually manage IP whitelist: `/verifiyadmin addip <IP>` and `/verifiyadmin removeip <IP>`
- Add configuration option to enable/disable IP whitelist per-server
