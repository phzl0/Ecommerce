try {
  if (localStorage.getItem("level-commerce-color-mode") === "dark")
    document.documentElement.dataset.colorMode = "dark";
} catch {}
