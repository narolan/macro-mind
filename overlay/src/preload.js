'use strict';

const { contextBridge, ipcRenderer } = require('electron');

contextBridge.exposeInMainWorld('macromind', {
  onViewModel: (callback) => ipcRenderer.on('view-model', (_event, vm) => callback(vm)),
});
