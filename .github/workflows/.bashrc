# 🌟 Auto-run alive.js when Codespace starts
if [ -d "/workspaces/noobdarti" ]; then
  cd /workspaces/noobdarti
  git pull origin main
  nohup node alive.js > /dev/null 2>&1 &
  echo "[🔥] alive.js launched on startup!"
fi
