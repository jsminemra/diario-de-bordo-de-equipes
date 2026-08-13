/* heatmap.js
 * Reads data-level / data-date / data-fiz / data-impedimento from .heatmap-cell elements
 * and shows a tooltip on hover.
 * Data is rendered server-side via Thymeleaf (th:attr on each cell).
 *
 * Suporta qualquer número de heatmaps na mesma página (ex.: visões
 * "gerais" com um heatmap compacto por membro da equipe) — um único
 * tooltip fixo é reaproveitado para todas as células.
 */
(function () {
  'use strict';

  var cells = document.querySelectorAll('.heatmap-cell');
  if (!cells.length) return;

  var tip = document.createElement('div');
  tip.className = 'tooltip';
  tip.style.position = 'fixed';
  document.body.appendChild(tip);

  cells.forEach(function (cell) {
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

      var rect = cell.getBoundingClientRect();
      tip.style.display = 'block';
      tip.style.left = (rect.left + rect.width / 2) + 'px';
      tip.style.top = (rect.top - 6) + 'px';
    });

    cell.addEventListener('mouseleave', function () {
      tip.style.display = 'none';
    });
  });
}());
