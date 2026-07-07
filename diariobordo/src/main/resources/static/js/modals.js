/* modals.js
 * Handles open/close for:
 *   - #modal-encerrar   (Encerrar Sprint confirmation)
 *   - #modal-excluir    (Excluir Equipe with name confirmation)
 *   - #modal-nova-sprint (4-step Nova Sprint stepper)
 *
 * Form submissions are plain Thymeleaf <form> POSTs – no JS needed for those.
 */
(function () {
  'use strict';

  /* ── helpers ────────────────────────────────────────────────────────── */
  function openModal(id) {
    var el = document.getElementById(id);
    if (el) { el.style.display = 'grid'; document.body.style.overflow = 'hidden'; }
  }
  function closeModal(id) {
    var el = document.getElementById(id);
    if (el) { el.style.display = 'none'; document.body.style.overflow = ''; }
  }

  /* close when clicking the backdrop itself */
  document.addEventListener('click', function (e) {
    if (e.target.classList.contains('modal-backdrop')) {
      e.target.style.display = 'none';
      document.body.style.overflow = '';
    }
  });

  /* ── Encerrar Sprint ─────────────────────────────────────────────────── */
  var btnEnd = document.getElementById('btn-encerrar-sprint');
  if (btnEnd) btnEnd.addEventListener('click', function () { openModal('modal-encerrar'); });

  var btnCancelEnd = document.getElementById('btn-cancelar-encerrar');
  if (btnCancelEnd) btnCancelEnd.addEventListener('click', function () { closeModal('modal-encerrar'); });

  /* ── Excluir Equipe ──────────────────────────────────────────────────── */
  document.querySelectorAll('.btn-excluir-equipe').forEach(function (btn) {
    btn.addEventListener('click', function () {
      var name   = btn.getAttribute('data-team-name') || '';
      var teamId = btn.getAttribute('data-team-id')   || '';

      /* update modal copy */
      var span = document.getElementById('excluir-team-name');
      if (span) span.textContent = name;

      var hiddenId = document.getElementById('excluir-team-id');
      if (hiddenId) hiddenId.value = teamId;

      /* reset confirmation input */
      var oldInput = document.getElementById('confirm-team-name');
      var btnConfirm = document.getElementById('btn-confirmar-excluir');
      if (oldInput && btnConfirm) {
        btnConfirm.disabled = true;
        /* clone to remove stale listeners */
        var newInput = oldInput.cloneNode(true);
        newInput.value = '';
        oldInput.parentNode.replaceChild(newInput, oldInput);
        newInput.addEventListener('input', function () {
          btnConfirm.disabled = (newInput.value.trim() !== name.trim());
        });
      }

      openModal('modal-excluir');
    });
  });

  var btnCancelExcluir = document.getElementById('btn-cancelar-excluir');
  if (btnCancelExcluir) btnCancelExcluir.addEventListener('click', function () { closeModal('modal-excluir'); });

  /* ── Nova Sprint – 4-step stepper ───────────────────────────────────── */
  var TOTAL = 4;
  var step  = 1;

  function gotoStep(n) {
    step = Math.max(1, Math.min(TOTAL, n));
    for (var i = 1; i <= TOTAL; i++) {
      var content = document.getElementById('sprint-step-' + i);
      if (content) content.style.display = (i === step) ? 'block' : 'none';

      var dot = document.getElementById('step-dot-' + i);
      if (dot) {
        dot.classList.toggle('is-active',  i <= step);
        dot.classList.toggle('is-current', i === step);
      }
      var line = document.getElementById('step-line-' + i);
      if (line) line.classList.toggle('active', i < step);
    }

    var btnVol = document.getElementById('btn-sprint-voltar');
    var btnPro = document.getElementById('btn-sprint-proximo');
    var btnCri = document.getElementById('btn-sprint-criar');
    if (btnVol) btnVol.style.display = step > 1           ? 'inline-flex' : 'none';
    if (btnPro) btnPro.style.display = step < TOTAL       ? 'inline-flex' : 'none';
    if (btnCri) btnCri.style.display = step === TOTAL     ? 'inline-flex' : 'none';

    if (step === TOTAL) syncReview();
  }

  function syncReview() {
    var nameEl  = document.getElementById('sprint-name');
    var startEl = document.getElementById('sprint-start');
    var endEl   = document.getElementById('sprint-end');
    setText('review-sprint-name', nameEl  ? (nameEl.value  || '(sem nome)') : '');
    setText('review-start',  startEl ? startEl.value : '');
    setText('review-end',    endEl   ? endEl.value   : '');
    if (startEl && endEl && startEl.value && endEl.value) {
      var days = Math.max(0,
        Math.round((new Date(endEl.value) - new Date(startEl.value)) / 86400000) + 1);
      setText('review-duration', days + ' dias');
    }
  }

  function setText(id, val) {
    var el = document.getElementById(id);
    if (el) el.textContent = val;
  }

  function validateStep() {
    if (step === 1) {
      var n = document.getElementById('sprint-name');
      if (!n || !n.value.trim()) { if (n) n.focus(); return false; }
    }
    if (step === 2) {
      var s = document.getElementById('sprint-start');
      var e = document.getElementById('sprint-end');
      if (!s || !e || !s.value || !e.value) return false;
      var days = Math.round((new Date(e.value) - new Date(s.value)) / 86400000) + 1;
      if (days < 3) { if (e) e.focus(); return false; }
    }
    return true;
  }

  var btnNova = document.getElementById('btn-nova-sprint');
  if (btnNova) {
    btnNova.addEventListener('click', function () {
      openModal('modal-nova-sprint');
      gotoStep(1);
    });
  }

  var btnCancelSprint = document.getElementById('btn-cancelar-sprint');
  if (btnCancelSprint) btnCancelSprint.addEventListener('click', function () { closeModal('modal-nova-sprint'); });

  var btnPro = document.getElementById('btn-sprint-proximo');
  if (btnPro) btnPro.addEventListener('click', function () { if (validateStep()) gotoStep(step + 1); });

  var btnVol = document.getElementById('btn-sprint-voltar');
  if (btnVol) btnVol.addEventListener('click', function () { gotoStep(step - 1); });

  /* duration display */
  var startIn = document.getElementById('sprint-start');
  var endIn   = document.getElementById('sprint-end');
  var durEl   = document.getElementById('sprint-duration');

  function updateDuration() {
    if (!startIn || !endIn || !durEl || !startIn.value || !endIn.value) return;
    var d = Math.max(0, Math.round((new Date(endIn.value) - new Date(startIn.value)) / 86400000) + 1);
    durEl.textContent = d + ' dias';
  }
  if (startIn) startIn.addEventListener('change', updateDuration);
  if (endIn)   endIn.addEventListener('change',   updateDuration);

  /* preset buttons (data-sprint-preset="7|14|21") */
  document.querySelectorAll('[data-sprint-preset]').forEach(function (btn) {
    btn.addEventListener('click', function () {
      var days = parseInt(btn.getAttribute('data-sprint-preset'), 10);
      var today = new Date();
      var end   = new Date(today);
      end.setDate(end.getDate() + days - 1);
      if (startIn) startIn.value = toISO(today);
      if (endIn)   endIn.value   = toISO(end);
      updateDuration();
    });
  });

  function toISO(d) {
    return d.getFullYear() + '-' +
      String(d.getMonth() + 1).padStart(2, '0') + '-' +
      String(d.getDate()).padStart(2, '0');
  }
}());
