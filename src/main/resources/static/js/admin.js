// Native details keeps the navigation usable even when JavaScript is unavailable.
const navigation = document.querySelector('.admin-navigation');
const mobile = window.matchMedia('(max-width: 800px)');
function syncNavigation() { if (navigation) navigation.open = !mobile.matches; }
syncNavigation();
mobile.addEventListener('change', syncNavigation);
document.addEventListener('keydown', event => {
  if (event.key === 'Escape' && mobile.matches && navigation?.open) {
    navigation.open = false;
    navigation.querySelector('summary').focus();
  }
});

// Section search reuses the real navigation; no dead shortcuts or duplicated routes.
const searchDialog = document.querySelector('.admin-search-dialog');
const sectionInput = document.querySelector('#admin-section-search');
const sectionResults = document.querySelector('.admin-search-results');
const sections = [...document.querySelectorAll('.sidebar-nav a')];
const normalize = value => value.normalize('NFD').replace(/[\u0300-\u036f]/g, '').toLowerCase();
function renderSections() {
  if (!sectionResults) return;
  const matches = sections.filter(link => normalize(link.textContent).includes(normalize(sectionInput.value)));
  sectionResults.replaceChildren(...matches.map(link => {
    const result = document.createElement('a'); result.href = link.href;
    result.textContent = link.textContent.trim(); return result;
  }));
  document.querySelector('.admin-search-empty').hidden = matches.length > 0;
}
function openSectionSearch() { if (!searchDialog) return; sectionInput.value = ''; renderSections(); searchDialog.showModal(); sectionInput.focus(); }
document.querySelector('[data-admin-search]')?.addEventListener('click', openSectionSearch);
document.querySelector('[data-admin-search-close]')?.addEventListener('click', () => searchDialog.close());
sectionInput?.addEventListener('input', renderSections);
sectionInput?.addEventListener('keydown', event => { if (event.key === 'Enter') { event.preventDefault(); sectionResults.querySelector('a')?.click(); } });
document.addEventListener('keydown', event => { if ((event.ctrlKey || event.metaKey) && event.key.toLowerCase() === 'k') { event.preventDefault(); if (!searchDialog?.open) openSectionSearch(); } });
// Monochrome icons keep the operational menu easy to scan.
const iconPaths = ['M3 10l9-7 9 7v11H3z','M4 5h16v16H4z M8 3v4 M16 3v4','M2 7h12v12H2z M14 11h5l3 4v4h-8 M6 19v2 M18 19v2','M4 4h16v12H9l-5 4z','M4 8h16v12H4z M8 8V5h8v3','M3 7l9-4 9 4-9 4z M3 7v12l9 4 9-4V7 M12 11v12','M3 4h18v16H3z M3 9h18 M9 9v11','M8 3h8v6l4 4v8H4v-8l4-4z','M3 5h7l3 3h8v13H3z','M12 3l3 6 6 1-5 5 1 7-5-3-5 3 1-7-5-5 6-1z','M4 5h16v16H4z M4 10h16 M8 3v4 M16 3v4','M5 8h14v8H5z M9 3v5 M15 16v5'];
sections.slice(0,12).forEach((link,index) => {
 const svg = document.createElementNS('http://www.w3.org/2000/svg','svg'); svg.setAttribute('viewBox','0 0 24 24');svg.setAttribute('class','admin-nav-icon');svg.setAttribute('aria-hidden','true');
 const path=document.createElementNS(svg.namespaceURI,'path');path.setAttribute('d',iconPaths[index]);svg.append(path);link.prepend(svg);
});
