/* board.js — drag-and-drop das User Stories entre colunas do quadro Kanban,
 * e ciclo de status por clique nas Tasks dentro de cada card. */
(function () {
  'use strict';

  var STATUS_CYCLE = ['A_FAZER', 'EM_ANDAMENTO', 'CONCLUIDA'];
  var STATUS_LABELS = { A_FAZER: 'A Fazer', EM_ANDAMENTO: 'Em Andamento', CONCLUIDA: 'Concluída' };
  var draggedCard = null;
  var draggedOriginColumn = null;
  var draggedNextSibling = null;

  function updateColumnCounts() {
    document.querySelectorAll('.board-column').forEach(function (col) {
      var count = col.querySelectorAll('.story-card').length;
      var badge = col.querySelector('.board-column-header .badge');
      if (badge) badge.textContent = count;
    });
  }

  /* ── drag-and-drop das User Stories ── */
  document.querySelectorAll('.story-card').forEach(function (card) {
    card.addEventListener('dragstart', function () {
      draggedCard = card;
      draggedOriginColumn = card.parentElement;
      draggedNextSibling = card.nextElementSibling;
      card.classList.add('dragging');
    });
    card.addEventListener('dragend', function () {
      card.classList.remove('dragging');
      draggedCard = null;
    });
  });

  document.querySelectorAll('.board-column').forEach(function (col) {
    col.addEventListener('dragover', function (e) {
      e.preventDefault();
      col.classList.add('drag-over');
    });
    col.addEventListener('dragleave', function () {
      col.classList.remove('drag-over');
    });
    col.addEventListener('drop', function (e) {
      e.preventDefault();
      col.classList.remove('drag-over');
      if (!draggedCard) return;

      var storyId = draggedCard.getAttribute('data-story-id');
      var newStatus = col.getAttribute('data-status');
      var originColumn = draggedOriginColumn;

      col.appendChild(draggedCard);
      updateColumnCounts();

      fetch('/tasks/stories/' + storyId + '/status', {
        method: 'POST',
        credentials: 'same-origin',
        headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
        body: new URLSearchParams({ status: newStatus })
      }).then(function (r) {
        if (!r.ok) throw new Error('falha ao atualizar status');
      }).catch(function () {
        /* reverte para a coluna original em caso de falha */
        if (draggedNextSibling) {
          originColumn.insertBefore(draggedCard, draggedNextSibling);
        } else {
          originColumn.appendChild(draggedCard);
        }
        updateColumnCounts();
      });
    });
  });

  /* ── ciclo de status das Tasks (clique) ── */
  document.querySelectorAll('.task-status-btn').forEach(function (btn) {
    btn.addEventListener('click', function () {
      var taskId = btn.getAttribute('data-task-id');
      var current = btn.getAttribute('data-task-status');
      var next = STATUS_CYCLE[(STATUS_CYCLE.indexOf(current) + 1) % STATUS_CYCLE.length];
      var row = btn.closest('.task-row');
      var titleEl = row ? row.querySelector('.task-row-title') : null;

      var previousStatus = current;
      btn.setAttribute('data-task-status', next);
      btn.textContent = STATUS_LABELS[next];
      if (titleEl) titleEl.classList.toggle('done', next === 'CONCLUIDA');

      fetch('/tasks/tasks/' + taskId + '/status', {
        method: 'POST',
        credentials: 'same-origin',
        headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
        body: new URLSearchParams({ status: next })
      }).then(function (r) {
        if (!r.ok) throw new Error('falha ao atualizar status');
      }).catch(function () {
        btn.setAttribute('data-task-status', previousStatus);
        btn.textContent = STATUS_LABELS[previousStatus];
        if (titleEl) titleEl.classList.toggle('done', previousStatus === 'CONCLUIDA');
      });
    });
  });
}());
