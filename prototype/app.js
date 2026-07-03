(function () {
  const viewButtons = document.querySelectorAll("[data-target]");
  const views = document.querySelectorAll(".view");
  const navButtons = document.querySelectorAll(".nav-link");
  const detailStatus = document.querySelector(".detail-status");
  const detailActions = document.querySelector(".detail-actions");
  const transactionPanel = document.querySelector(".transaction-panel");
  const nameInput = document.getElementById("preview-name");
  const priceInput = document.getElementById("preview-price");
  const pickupLocationInput = document.getElementById("pickup-location");
  const pickupConsultInput = document.getElementById("pickup-consult");
  const nameOutput = document.getElementById("name-output");
  const priceOutput = document.getElementById("price-output");
  const pickupOutput = document.querySelector("[data-pickup-preview]");
  const authMessage = document.querySelector("[data-auth-message]");
  const loginButton = document.querySelector("[data-login-action]");
  const loginPill = document.querySelector(".login-pill");
  const menuOpenButton = document.querySelector("[data-menu-open]");
  const menuCloseButton = document.querySelector("[data-menu-close]");
  const menuDrawer = document.querySelector("[data-menu-drawer]");
  const menuBackdrop = document.querySelector("[data-menu-backdrop]");
  const themeButtons = document.querySelectorAll("[data-theme-option]");

  const protectedViews = new Set(["sell", "mypage", "admin"]);
  const themes = new Set(["aqua", "pink", "purple", "lime"]);
  let isAuthenticated = false;
  let authRedirectTarget = "home";
  let currentStatus = "OPEN";

  function getStoredTheme() {
    try {
      const savedTheme = localStorage.getItem("campustrade-theme");
      return themes.has(savedTheme) ? savedTheme : "aqua";
    } catch (error) {
      return "aqua";
    }
  }

  function saveTheme(theme) {
    try {
      localStorage.setItem("campustrade-theme", theme);
    } catch (error) {
      // The prototype still works if browser storage is unavailable.
    }
  }

  function setTheme(theme) {
    const nextTheme = themes.has(theme) ? theme : "aqua";
    document.body.dataset.theme = nextTheme;
    themeButtons.forEach((button) => {
      const isActive = button.dataset.themeOption === nextTheme;
      button.classList.toggle("is-active", isActive);
      button.setAttribute("aria-pressed", String(isActive));
    });
    saveTheme(nextTheme);
  }

  function setMenuOpen(open) {
    if (!menuDrawer || !menuOpenButton || !menuBackdrop) return;
    menuDrawer.classList.toggle("is-open", open);
    menuDrawer.setAttribute("aria-hidden", String(!open));
    menuOpenButton.setAttribute("aria-expanded", String(open));
    menuBackdrop.hidden = !open;
    if (open && menuCloseButton) {
      menuCloseButton.focus();
    }
  }

  function showView(name) {
    views.forEach((view) => {
      view.classList.toggle("is-active", view.dataset.view === name);
    });
    navButtons.forEach((button) => {
      button.classList.toggle("is-active", button.dataset.target === name);
    });
    window.location.hash = name;
    window.scrollTo({ top: 0, behavior: "smooth" });
  }

  function updateAuthUi() {
    if (!loginPill) return;
    loginPill.textContent = isAuthenticated ? "ログアウト" : "ログイン";
  }

  function showLoginPrompt(target, message) {
    authRedirectTarget = target || "home";
    setMenuOpen(false);
    if (authMessage) {
      authMessage.hidden = false;
      authMessage.textContent = message || "出品や購入にはログインが必要です。";
    }
    showView("auth");
  }

  function navigateTo(target, options = {}) {
    if (!target) return;

    if (target === "auth" && isAuthenticated && options.fromHeader) {
      isAuthenticated = false;
      authRedirectTarget = "home";
      updateAuthUi();
      renderDetailState();
      showView("home");
      return;
    }

    if (!isAuthenticated && protectedViews.has(target)) {
      const messages = {
        sell: "出品するにはログインが必要です。",
        mypage: "マイページを見るにはログインが必要です。",
        admin: "管理画面を使うにはログインが必要です。"
      };
      showLoginPrompt(target, messages[target]);
      return;
    }

    if (target === "auth" && !options.keepMessage && authMessage) {
      authMessage.hidden = true;
    }

    setMenuOpen(false);
    showView(target);
  }

  function renderDetailState() {
    const statusLabels = {
      OPEN: "出品中",
      LOCKED: "取引中",
      CLOSED: "取引完了"
    };
    const statusClasses = {
      OPEN: "badge detail-status open",
      LOCKED: "badge detail-status locked",
      CLOSED: "badge detail-status closed"
    };

    detailStatus.textContent = statusLabels[currentStatus];
    detailStatus.className = statusClasses[currentStatus];

    let actionMarkup = "";
    if (currentStatus === "OPEN") {
      if (isAuthenticated) {
        actionMarkup = `
          <button class="primary-button" type="button">購入申し込み</button>
        `;
      } else {
        actionMarkup = `
          <button class="primary-button" type="button" data-login-required="purchase">ログインして購入</button>
        `;
      }
    } else if (currentStatus === "LOCKED") {
      actionMarkup = `
        <button class="primary-button" type="button">取引メッセージ</button>
        <button class="secondary-button" type="button">取引完了</button>
        <button class="danger-button" type="button">キャンセル</button>
      `;
    } else {
      actionMarkup = `
        <button class="secondary-button" type="button">取引履歴を見る</button>
      `;
    }

    detailActions.innerHTML = actionMarkup;
    detailActions.querySelectorAll("[data-target]").forEach((button) => {
      button.addEventListener("click", () => navigateTo(button.dataset.target));
    });
    detailActions.querySelectorAll("[data-login-required]").forEach((button) => {
      button.addEventListener("click", () => {
        showLoginPrompt("detail", "購入申し込みにはログインが必要です。");
      });
    });

    if (currentStatus === "LOCKED" || currentStatus === "CLOSED") {
      transactionPanel.innerHTML = `
        <div class="section-heading compact">
          <div>
            <p class="eyebrow">取引</p>
            <h2 id="transaction-title">取引メッセージ</h2>
          </div>
        </div>
        <div class="message-list">
          <div class="message-row">
            <strong>ゆうき</strong>
            <p>本日17時に図書館前で受け取れます。</p>
            <span>12:08</span>
          </div>
          <div class="message-row seller">
            <strong>なつ</strong>
            <p>承知しました。入口横の掲示板付近で待っています。</p>
            <span>12:14</span>
          </div>
        </div>
        <form class="inline-form">
          <label for="trade-message">メッセージ</label>
          <input id="trade-message" type="text" placeholder="受け渡し内容を入力">
          <button type="button">送信</button>
        </form>
      `;
    } else {
      transactionPanel.innerHTML = `
        <div class="section-heading compact">
          <div>
            <p class="eyebrow">取引</p>
            <h2 id="transaction-title">取引メッセージ</h2>
          </div>
        </div>
        <div class="locked-placeholder">
          <i data-lucide="lock-keyhole"></i>
          <p>購入申し込み後、出品者と購入者に表示されます。</p>
        </div>
      `;
    }

    if (window.lucide) {
      window.lucide.createIcons();
    }
  }

  function updatePreview() {
    if (nameInput && nameOutput) {
      nameOutput.textContent = nameInput.value || "商品名";
    }
    if (priceInput && priceOutput) {
      const value = Number(priceInput.value || 0).toLocaleString("ja-JP");
      priceOutput.textContent = `¥${value}`;
    }
    if (pickupLocationInput && pickupConsultInput && pickupOutput) {
      const isConsult = pickupConsultInput.checked;
      pickupLocationInput.disabled = isConsult;
      const location = pickupLocationInput.value.trim();
      pickupOutput.textContent = isConsult
        ? "受け渡しは要相談"
        : `受け渡しは${location || "要相談"}を予定`;
    }
  }

  viewButtons.forEach((button) => {
    button.addEventListener("click", (event) => {
      const target = button.dataset.target;
      if (!target) return;
      event.preventDefault();
      navigateTo(target, { fromHeader: button === loginPill });
    });
  });

  if (menuOpenButton) {
    menuOpenButton.addEventListener("click", () => setMenuOpen(true));
  }

  if (menuCloseButton) {
    menuCloseButton.addEventListener("click", () => setMenuOpen(false));
  }

  if (menuBackdrop) {
    menuBackdrop.addEventListener("click", () => setMenuOpen(false));
  }

  themeButtons.forEach((button) => {
    button.addEventListener("click", () => setTheme(button.dataset.themeOption));
  });

  document.addEventListener("keydown", (event) => {
    if (event.key === "Escape") {
      setMenuOpen(false);
    }
  });

  if (loginButton) {
    loginButton.addEventListener("click", () => {
      isAuthenticated = true;
      updateAuthUi();
      renderDetailState();
      if (authMessage) authMessage.hidden = true;
      navigateTo(authRedirectTarget);
    });
  }

  document.querySelectorAll(".thumb").forEach((thumb) => {
    thumb.addEventListener("click", () => {
      document.querySelectorAll(".thumb").forEach((item) => item.classList.remove("is-active"));
      thumb.classList.add("is-active");
    });
  });

  document.querySelectorAll(".upload-slot").forEach((slot) => {
    slot.addEventListener("click", () => {
      slot.classList.toggle("is-filled");
      const label = slot.querySelector("span");
      if (label) {
        label.textContent = slot.classList.contains("is-filled") ? "写真追加済み" : "写真を追加";
      }
    });
  });

  [nameInput, priceInput, pickupLocationInput].forEach((input) => {
    if (input) input.addEventListener("input", updatePreview);
  });

  if (pickupConsultInput) {
    pickupConsultInput.addEventListener("change", updatePreview);
  }

  const hash = window.location.hash.replace("#", "");
  if (hash && document.querySelector(`[data-view="${hash}"]`)) {
    navigateTo(hash);
  }

  setTheme(getStoredTheme());
  updatePreview();
  updateAuthUi();
  renderDetailState();

  window.addEventListener("load", () => {
    if (window.lucide) {
      window.lucide.createIcons();
    }
  });
})();
