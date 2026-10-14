const { app, BrowserWindow, shell, dialog } = require('electron');
const path = require('node:path');

const APP_ICON = path.join(__dirname, 'assets', process.platform === 'win32' ? 'taxoryn.ico' : 'taxoryn-icon.png');

const APP_URL = 'https://app.taxoryn.com/login';
const TRUSTED_HOSTS = new Set(['app.taxoryn.com']);
let mainWindow;

function isTrustedAppUrl(rawUrl) {
  try {
    const url = new URL(rawUrl);
    return url.protocol === 'https:' && TRUSTED_HOSTS.has(url.hostname);
  } catch {
    return false;
  }
}

function createWindow() {
  mainWindow = new BrowserWindow({
    width: 1440,
    height: 920,
    minWidth: 1024,
    minHeight: 680,
    show: false,
    title: 'Taxoryn',
    icon: APP_ICON,
    autoHideMenuBar: true,
    backgroundColor: '#f8fafc',
    webPreferences: {
      nodeIntegration: false,
      contextIsolation: true,
      sandbox: true,
      webviewTag: false,
    },
  });

  mainWindow.once('ready-to-show', () => mainWindow.show());

  // Keep the desktop application on the official Taxoryn origin.
  mainWindow.webContents.on('will-navigate', (event, targetUrl) => {
    if (!isTrustedAppUrl(targetUrl)) {
      event.preventDefault();
      if (/^https?:/i.test(targetUrl)) shell.openExternal(targetUrl);
    }
  });

  // Open external links in the user's default browser, not in an embedded window.
  mainWindow.webContents.setWindowOpenHandler(({ url }) => {
    if (isTrustedAppUrl(url)) {
      return { action: 'allow' };
    }
    if (/^https?:/i.test(url)) shell.openExternal(url);
    return { action: 'deny' };
  });

  mainWindow.webContents.on('render-process-gone', async () => {
    const result = await dialog.showMessageBox(mainWindow, {
      type: 'warning',
      title: 'Taxoryn needs to reload',
      message: 'The Taxoryn window stopped unexpectedly.',
      buttons: ['Reload Taxoryn', 'Close'],
      defaultId: 0,
      cancelId: 1,
    });
    if (result.response === 0 && mainWindow && !mainWindow.isDestroyed()) {
      mainWindow.loadURL(APP_URL);
    } else {
      app.quit();
    }
  });

  mainWindow.loadURL(APP_URL).catch(async () => {
    if (!mainWindow || mainWindow.isDestroyed()) return;
    const result = await dialog.showMessageBox(mainWindow, {
      type: 'warning',
      title: 'Cannot connect to Taxoryn',
      message: 'Taxoryn requires an internet connection. Check your connection and try again.',
      buttons: ['Retry', 'Close'],
      defaultId: 0,
      cancelId: 1,
    });
    if (result.response === 0 && mainWindow && !mainWindow.isDestroyed()) {
      mainWindow.loadURL(APP_URL);
    } else {
      app.quit();
    }
  });

  mainWindow.on('closed', () => { mainWindow = null; });
}

app.whenReady().then(() => {
  app.setAppUserModelId('com.oryntechnology.taxoryn');
  createWindow();

  app.on('activate', () => {
    if (BrowserWindow.getAllWindows().length === 0) createWindow();
  });
});

app.on('window-all-closed', () => {
  if (process.platform !== 'darwin') app.quit();
});
