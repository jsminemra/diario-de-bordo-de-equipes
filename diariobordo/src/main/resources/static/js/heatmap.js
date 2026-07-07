/* heatmap.js
 * Reads data-level / data-date / data-fiz / data-impedimento from .heatmap-cell elements
 * and shows a tooltip on hover.
 * Data is rendered server-side via Thymeleaf (th:attr on each cell).
 */
(function () {
  'use strict';

  var heatmapEl = document.querySelector('.heatmap');
  if (!heatmapEl) return;

  /* tooltip node – appended to the heatmap's parent so it positions correctly */
  var tip = document.createElement('div');
  tip.className = 'tooltip';
  heatmapEl.parentElement.style.position = 'relative';
  heatmapEl.parentElement.appendChild(tip);

  heatmapEl.addEventListener('mouseleave', function () {
    tip.style.display = 'none';
  });

  heatmapEl.querySelectorAll('.heatmap-cell').forEach(function (cell) {
    cell.addEventListener('mouseenter', function () {
      var level = cell.getAttribute('data-level');
      if (level === 'future') { tip.style.display = 'none'; return; }

      var dateStr = cell.getAttribute('data-date') || '';
      var fiz     = cell.getAttribute('data-fiz') || '';
      var imp     = cell.getAttribute('data-impedimento') || '';
      var lvl     = parseInt(level, 10) || 0;

      var html = '<div class="t-date">' + dateStr + '</div>';
      if (lvl > 0 && fiz) {
        html += '<div style="font-weight:600;margin-bottom:2px">' + fiz + '</div>';
        if (imp) {
          html += '<div style="color:#94A3B8;font-size:10.5px">Impedimento: ' + imp + '</div>';
        }
      } else {
        html += '<div style="color:#CBD5E1">Sem registro neste dia.</div>';
      }
      tip.innerHTML = html;

      var rect  = cell.getBoundingClientRect();
      var pRect = heatmapEl.getBoundingClientRect();

      tip.style.display = 'block';
      tip.style.left = (rect.left - pRect.left + rect.width / 2) + 'px';
      tip.style.top  = (rect.top  - pRect.top  - 6) + 'px';
    });

    cell.addEventListener('mouseleave', function () {
      tip.style.display = 'none';
    });
  });
}());
