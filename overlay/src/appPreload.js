'use strict';

const { contextBridge, ipcRenderer } = require('electron');

// The app window may do exactly these things. Each handler below returns nothing that would expose ipcRenderer itself.
contextBridge.exposeInMainWorld('macromindApp', {
  getSettings: () => ipcRenderer.invoke('app:get-settings'),
  saveSettings: (settings) => ipcRenderer.invoke('app:save-settings', settings),
  getStatus: () => ipcRenderer.invoke('app:get-status'),
  getVersion: () => ipcRenderer.invoke('app:get-version'),
  previewOverlay: () => ipcRenderer.invoke('app:preview-overlay'),
  onStatus: (callback) => {
    ipcRenderer.on('app:status', (_event, status) => callback(status));
  },
});
