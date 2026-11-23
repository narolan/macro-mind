'use strict';

const { toViewModel } = require('./viewModel');

const PREVIEW_MS = 6000;

// Sample advice with no icon and no enemy numbers: it only shows what the overlay looks like.
const PREVIEW_VIEW = toViewModel({
  visible: true,
  advice: [
    { item: 'Plated Steelcaps', reasoning: 'Preview: the enemy team deals mostly physical damage.', priority: 'CRITICAL', confidence: 'EXACT' },
    { item: 'Executioner’s Calling', reasoning: 'Preview: the enemy team has heavy healing.', priority: 'RECOMMENDED', confidence: 'ESTIMATED' },
    { item: 'Control Ward', reasoning: 'Preview: nothing urgent, a ward is always useful.', priority: 'SITUATIONAL', confidence: 'EXACT' },
  ],
});

module.exports = { PREVIEW_VIEW, PREVIEW_MS };
