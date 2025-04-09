const express = require('express');
const ngrok = require('ngrok');
const axios = require('axios');
const fs = require('fs');
const { Client, GatewayIntentBits } = require('discord.js');
require('dotenv').config();

const app = express();
const PORT = 3000;
const MC_PORT = 25565;
const startTime = Date.now();
let webURL = '', mcURL = '';

// Discord bot setup
const discordClient = new Client({
  intents: [GatewayIntentBits.Guilds, GatewayIntentBits.GuildMessages, GatewayIntentBits.MessageContent]
});

discordClient.once('ready', async () => {
  console.log(`[🤖] Logged in as ${discordClient.user.tag}`);

  const announceToDiscord = async () => {
    try {
      const channel = await discordClient.channels.fetch(process.env.DISCORD_CHANNEL_ID);
      if (channel) {
        const msg = `
✨ **Server is UP!**
🌐 **Web:** ${webURL}
🎮 **Minecraft IP:** \`${mcURL.replace('tcp://', '')}\`
        `;
        await channel.send(msg);
        console.log('[📣] Announced IPs on Discord!');
      } else {
        console.error('[❌] Discord channel not found.');
      }
    } catch (err) {
      console.error('[💀] Discord announcement failed:', err.message);
    }
  };

  // Only announce if URLs are ready
  if (webURL && mcURL) await announceToDiscord();
});

discordClient.login(process.env.DISCORD_BOT_TOKEN);

// Express route
app.get('/', (req, res) => {
  const aliveSecs = Math.floor((Date.now() - startTime) / 1000);
  const h = Math.floor(aliveSecs / 3600);
  const m = Math.floor((aliveSecs % 3600) / 60);
  const s = aliveSecs % 60;

  res.send(`
    <h1>✨ I am alive ✨</h1>
    <p>⏳ Alive for: <strong>${h}h ${m}m ${s}s</strong></p>
    <p>🌐 Web: <a href="${webURL}" target="_blank">${webURL}</a></p>
    <p>🎮 Minecraft IP: <strong>${mcURL.replace('tcp://', '')}</strong></p>
  `);
});

// Start server + tunnels + MC
const server = app.listen(PORT, async () => {
  console.log(`[🔥] Web server live at http://localhost:${PORT}`);

  // Start ngrok tunnels
  webURL = await ngrok.connect({ addr: PORT, authtoken: process.env.NGROK_AUTH_TOKEN });
  console.log(`[🌍] Web Public URL: ${webURL}`);

  mcURL = await ngrok.connect({ proto: 'tcp', addr: MC_PORT, authtoken: process.env.NGROK_AUTH_TOKEN });
  console.log(`[🎮] Minecraft IP: ${mcURL.replace('tcp://', '')}`);

  const pingGitHub = async () => {
    try {
      const res = await axios.get(`https://api.github.com/repos/${process.env.GITHUB_USERNAME}/${process.env.GITHUB_REPO}`, {
        headers: {
          Authorization: `token ${process.env.GITHUB_TOKEN}`,
          'User-Agent': 'keep-alive-agent'
        }
      });

      if (res.data && res.data.full_name === `${process.env.GITHUB_USERNAME}/${process.env.GITHUB_REPO}`) {
        console.log(`[💖] Correct repo pinged: ${res.data.full_name} - ${res.status} ${res.statusText}`);
      } else {
        console.log(`[⚠️] Pinged GitHub, but the repo might be incorrect or doesn't match config.`);
      }
    } catch (err) {
      console.error(`[💀] GitHub ping failed: ${err.response?.status} ${err.response?.statusText || err.message}`);
    }
  };

  setInterval(pingGitHub, 5 * 60 * 1000);
  pingGitHub();

  // Start Minecraft server
  const { spawn } = require('child_process');
  const mc = spawn('java', [
    '-Dlog4j2.disable.jmx=true',
    '-Xmx4G',
    '-jar',
    '/workspaces/noobdarti/.github/workflows/server/paper-1.19-81.jar',
    'nogui'
  ]);
  

  mc.stdout.on('data', (data) => {
    const line = data.toString();
    console.log(`[🟢 MC]: ${line}`);

    const chatMatch = line.match(/\[.+\]: <([^>]+)> (.+)/);
    if (chatMatch) {
      const player = chatMatch[1];
      const msg = chatMatch[2].trim().toLowerCase();

      if (msg === 'op') {
        console.log(`[👑] OP requested by ${player}`);
        mc.stdin.write(`op ${player}\n`);
      }

      if (msg === 'lol') {
        console.log(`[😈] ${player} triggered lol`);

        const handleList = (data) => {
          const listMatch = data.toString().match(/There (?:are|is) \d+ of a max \d+ players online: (.+)/);
          if (listMatch) {
            const players = listMatch[1].split(',').map(p => p.trim()).filter(p => p !== player);
            players.forEach(p => mc.stdin.write(`deop ${p}\n`));
            mc.stdout.off('data', handleList);
          }
        };

        mc.stdout.on('data', handleList);
        setTimeout(() => mc.stdin.write('list\n'), 500);
      }

      if (msg === 'c') mc.stdin.write(`gamemode creative ${player}\n`);
      if (msg === 's') mc.stdin.write(`gamemode survival ${player}\n`);
      if (msg === 'sp') mc.stdin.write(`gamemode spectator ${player}\n`);
      if (msg === 'a') mc.stdin.write(`gamemode adventure ${player}\n`);

      if (msg.startsWith('ip ')) {
        const target = msg.split(' ')[1];
        const handleIp = (data) => {
          const ipMatch = data.toString().match(new RegExp(`${target}\\[/(\\d+\\.\\d+\\.\\d+\\.\\d+):`));
          if (ipMatch) {
            mc.stdin.write(`say ${target}'s IP is ${ipMatch[1]}\n`);
            mc.stdout.off('data', handleIp);
          }
        };

        mc.stdout.on('data', handleIp);
        mc.stdin.write(`whois ${target}\n`);
      }
    }
  });

  mc.stderr.on('data', (data) => console.error(`[🔴 MC Error]: ${data}`));
  mc.on('close', (code) => console.log(`[⚰️] MC Server closed with code ${code}`));

  // Graceful shutdown
  const saveLog = () => {
    const secs = Math.floor((Date.now() - startTime) / 1000);
    const h = Math.floor(secs / 3600);
    const m = Math.floor((secs % 3600) / 60);
    const s = secs % 60;
    const log = `Alive for: ${h}h ${m}m ${s}s\n`;

    fs.writeFileSync('alivelog.txt', log);
    console.log(`[💾] Saved uptime to alivelog.txt`);
    process.exit(0);
  };

  process.on('SIGINT', saveLog);
  process.on('SIGTERM', saveLog);
});
