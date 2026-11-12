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
