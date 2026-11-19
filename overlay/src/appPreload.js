'use strict';

const { contextBridge, ipcRenderer } = require('electron');

// The app window may do exactly four things. Each handler below returns nothing that would expose ipcRenderer itself.
contextBridge.exposeInMainWorld('macromindApp', {
  getSettings: () => ipcRenderer.invoke('app:get-settings'),
  saveSettings: (settings) => ipcRenderer.invoke('app:save-settings', settings),
  getStatus: () => ipcRenderer.invoke('app:get-status'),
  onStatus: (callback) => {
    ipcRenderer.on('app:status', (_event, status) => callback(status));
  },
});
