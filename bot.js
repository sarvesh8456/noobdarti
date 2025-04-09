const mineflayer = require('mineflayer');
const { HttpsProxyAgent } = require('https-proxy-agent');
const fs = require('fs');

// Configuration
const config = {
  SERVER: '0.tcp.in.ngrok.io:11384',
  PROXY_FILE: 'proxies.txt',
  BOT_COUNT: 10,
  VERSION: '1.19',
  RECONNECT_DELAY: 60000,
  JOIN_DELAY: 5000
};

class BotManager {
  constructor() {
    this.proxies = this.loadProxies();
    this.startBots();
  }

  loadProxies() {
    try {
      return fs.readFileSync(config.PROXY_FILE, 'utf-8')
        .split('\n')
        .map(p => p.trim())
        .filter(p => p.includes(':'));
    } catch (e) {
      console.error('Failed to load proxies:', e.message);
      return [];
    }
  }

  startBots() {
    console.log(`Starting ${config.BOT_COUNT} bots...`);
    for (let i = 0; i < Math.min(config.BOT_COUNT, this.proxies.length); i++) {
      setTimeout(() => this.createBot(i), i * config.JOIN_DELAY);
    }
  }

  createBot(botId) {
    const [host, port] = config.SERVER.split(':');
    const proxy = this.proxies[botId % this.proxies.length];
    const username = `Player${Math.floor(Math.random() * 1000)}`;

    console.log(`[Bot ${botId}] Connecting as ${username} via ${proxy}`);

    const bot = mineflayer.createBot({
      host,
      port: parseInt(port),
      username,
      version: config.VERSION,
      auth: 'offline',
      agent: new HttpsProxyAgent(`http://${proxy}`),
      hideErrors: false
    });

    this.setupBot(bot, botId, username);
  }

  setupBot(bot, botId, username) {
    bot.on('login', () => {
      console.log(`[Bot ${botId}] Connected successfully`);
    });

    bot.on('spawn', () => {
      console.log(`[Bot ${botId}] Spawned in world`);
      setTimeout(() => {
        bot.chat('/register password password');
        setTimeout(() => bot.chat('/login password'), 2000);
      }, 10000);
    });

    bot.on('kicked', (reason) => {
      console.log(`[Bot ${botId}] Kicked: ${JSON.stringify(reason)}`);
      setTimeout(() => this.createBot(botId), config.RECONNECT_DELAY);
    });

    bot.on('error', (err) => {
      console.error(`[Bot ${botId}] Error: ${err.message}`);
      setTimeout(() => this.createBot(botId), config.RECONNECT_DELAY);
    });
  }
}

new BotManager();