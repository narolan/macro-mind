'use strict';

const CORNERS = ['top-right', 'top-left', 'bottom-right', 'bottom-left'];

/**
 * Where the overlay window goes on a display's work area (the part not covered by the taskbar).
 * An unknown corner means top right. The window is never placed above or left of the work area.
 */
function overlayBounds(workArea, corner, size, margin) {
  const place = CORNERS.includes(corner) ? corner : 'top-right';
  const right = place.endsWith('right');
  const bottom = place.startsWith('bottom');
  const x = right ? workArea.x + workArea.width - size.width - margin : workArea.x + margin;
  const y = bottom ? workArea.y + workArea.height - size.height - margin : workArea.y + margin;
  return { x: Math.max(x, workArea.x), y: Math.max(y, workArea.y), width: size.width, height: size.height };
}

module.exports = { overlayBounds };
