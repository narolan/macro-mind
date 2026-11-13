'use strict';

const { contextBridge, ipcRenderer } = require('electron');

const VIEW_MODEL_CHANNEL = 'view-model'; // must match the channel main.js sends on

contextBridge.exposeInMainWorld('macromind', {
  // Block body on purpose: ipcRenderer.on() returns the ipcRenderer, which must not reach the page.
  onViewModel: (callback) => {
    ipcRenderer.on(VIEW_MODEL_CHANNEL, (_event, viewModel) => callback(viewModel));
  },
});
