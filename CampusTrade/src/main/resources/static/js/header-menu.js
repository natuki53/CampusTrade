(() => {
  const drawer = document.querySelector("[data-menu-drawer]");
  const backdrop = document.querySelector("[data-menu-backdrop]");
  const openButtons = document.querySelectorAll("[data-menu-open]");
  const closeButtons = document.querySelectorAll("[data-menu-close]");
  const themeButtons = document.querySelectorAll("[data-theme-option]");
  const themes = new Set(["aqua", "pink", "purple", "lime"]);

  if (!drawer || !backdrop || openButtons.length === 0) {
    return;
  }

  const focusableSelector = [
    "a[href]",
    "button:not([disabled])",
    "input:not([disabled])",
    "select:not([disabled])",
    "textarea:not([disabled])",
    "[tabindex]:not([tabindex='-1'])"
  ].join(",");

  let lastFocusedElement = null;

  const getFocusableElements = () => Array.from(drawer.querySelectorAll(focusableSelector))
    .filter((element) => element.offsetParent !== null);

  const getStoredTheme = () => {
    try {
      const savedTheme = localStorage.getItem("campustrade-theme");
      return themes.has(savedTheme) ? savedTheme : "aqua";
    } catch (error) {
      return "aqua";
    }
  };

  const saveTheme = (theme) => {
    try {
      localStorage.setItem("campustrade-theme", theme);
    } catch (error) {
      // Keep the menu usable when browser storage is unavailable.
    }
  };

  const setTheme = (theme) => {
    const nextTheme = themes.has(theme) ? theme : "aqua";
    document.body.dataset.theme = nextTheme;
    themeButtons.forEach((button) => {
      const isActive = button.dataset.themeOption === nextTheme;
      button.classList.toggle("is-active", isActive);
      button.setAttribute("aria-pressed", String(isActive));
    });
    saveTheme(nextTheme);
  };

  const setMenuOpen = (open) => {
    drawer.classList.toggle("is-open", open);
    drawer.setAttribute("aria-hidden", String(!open));
    backdrop.hidden = !open;
    document.body.classList.toggle("has-menu-open", open);
    openButtons.forEach((button) => button.setAttribute("aria-expanded", String(open)));

    if (open) {
      lastFocusedElement = document.activeElement;
      const firstFocusable = getFocusableElements()[0];
      if (firstFocusable) {
        firstFocusable.focus();
      }
      return;
    }

    if (lastFocusedElement && typeof lastFocusedElement.focus === "function") {
      lastFocusedElement.focus();
    }
  };

  openButtons.forEach((button) => {
    button.addEventListener("click", () => setMenuOpen(true));
  });

  closeButtons.forEach((button) => {
    button.addEventListener("click", () => setMenuOpen(false));
  });

  backdrop.addEventListener("click", () => setMenuOpen(false));

  drawer.addEventListener("click", (event) => {
    if (event.target.closest("a")) {
      setMenuOpen(false);
    }
  });

  themeButtons.forEach((button) => {
    button.addEventListener("click", () => setTheme(button.dataset.themeOption));
  });

  document.addEventListener("keydown", (event) => {
    if (!drawer.classList.contains("is-open")) {
      return;
    }

    if (event.key === "Escape") {
      setMenuOpen(false);
      return;
    }

    if (event.key !== "Tab") {
      return;
    }

    const focusableElements = getFocusableElements();
    if (focusableElements.length === 0) {
      event.preventDefault();
      return;
    }

    const firstElement = focusableElements[0];
    const lastElement = focusableElements[focusableElements.length - 1];

    if (event.shiftKey && document.activeElement === firstElement) {
      event.preventDefault();
      lastElement.focus();
    } else if (!event.shiftKey && document.activeElement === lastElement) {
      event.preventDefault();
      firstElement.focus();
    }
  });

  setTheme(getStoredTheme());
})();
