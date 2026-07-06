(() => {
  document.querySelectorAll("[data-purchase-confirm]").forEach((form) => {
    form.addEventListener("submit", (event) => {
      if (!window.confirm(form.dataset.purchaseConfirm)) {
        event.preventDefault();
      }
    });
  });
})();
