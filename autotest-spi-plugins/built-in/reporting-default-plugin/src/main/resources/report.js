(() => {
  const search = document.querySelector('[data-report-search]');
  const filter = document.querySelector('[data-report-filter]');
  const rows = [...document.querySelectorAll('[data-report-row]')];
  const apply = () => {
    const term = (search?.value || '').toLowerCase();
    const state = filter?.value || 'ALL';
    rows.forEach(row => {
      const matchesText = row.textContent.toLowerCase().includes(term);
      const matchesState = state === 'ALL' || row.dataset.status === state || row.dataset.classification === state;
      row.classList.toggle('hidden', !(matchesText && matchesState));
    });
  };
  search?.addEventListener('input', apply);
  filter?.addEventListener('change', apply);
  apply();
})();
