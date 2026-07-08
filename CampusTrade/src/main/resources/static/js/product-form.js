(() => {
  const input = document.querySelector("[data-image-upload-input]");
  const status = document.querySelector("[data-image-upload-status]");
  const list = document.querySelector("[data-image-upload-list]");
  const existingImageItems = Array.from(document.querySelectorAll("[data-existing-image-item]"));
  const deletedImageIds = new Set();
  const csrfInput = document.querySelector('input[name="_csrf"]');
  const csrfHeader = document.querySelector('meta[name="_csrf_header"]')?.content || "X-CSRF-TOKEN";
  const csrfToken = csrfInput?.value;

  if (!input || !status || !list) {
    return;
  }

  const maxFiles = Number(input.dataset.maxFiles || 5);
  let selectedFiles = [];

  const fileKey = (file) => `${file.name}:${file.size}:${file.lastModified}`;

  const remainingExistingCount = () => existingImageItems
    .filter((item) => !deletedImageIds.has(item.dataset.imageId))
    .length;

  const availableNewFileCount = () => Math.max(maxFiles - remainingExistingCount(), 0);

  const deleteExistingImage = async (deleteUrl) => {
    const headers = {
      "X-Requested-With": "XMLHttpRequest"
    };
    if (csrfToken) {
      headers[csrfHeader] = csrfToken;
    }
    const response = await fetch(deleteUrl, {
      method: "POST",
      headers
    });
    if (!response.ok) {
      throw new Error("Failed to delete image");
    }
  };

  const syncInputFiles = () => {
    if (typeof DataTransfer === "undefined") {
      return;
    }
    const transfer = new DataTransfer();
    selectedFiles.forEach((file) => transfer.items.add(file));
    input.files = transfer.files;
  };

  const trimSelectedFiles = () => {
    const available = availableNewFileCount();
    if (selectedFiles.length > available) {
      selectedFiles = selectedFiles.slice(0, available);
      syncInputFiles();
    }
  };

  const render = () => {
    const available = availableNewFileCount();
    status.textContent = existingImageItems.length > 0
      ? `追加 ${selectedFiles.length}/${available}枚`
      : `${selectedFiles.length}/${maxFiles}枚`;
    list.replaceChildren();
    selectedFiles.forEach((file, index) => {
      const item = document.createElement("li");
      item.className = "selected-file-item";

      const name = document.createElement("span");
      name.textContent = file.name;

      const remove = document.createElement("button");
      remove.type = "button";
      remove.textContent = "削除";
      remove.dataset.removeIndex = String(index);

      item.append(name, remove);
      list.append(item);
    });
  };

  input.addEventListener("change", () => {
    const existingKeys = new Set(selectedFiles.map(fileKey));
    const available = availableNewFileCount();
    for (const file of Array.from(input.files)) {
      if (selectedFiles.length >= available) {
        break;
      }
      if (!existingKeys.has(fileKey(file))) {
        selectedFiles.push(file);
        existingKeys.add(fileKey(file));
      }
    }
    syncInputFiles();
    render();
  });

  existingImageItems.forEach((item) => {
    const imageId = item.dataset.imageId;
    if (!imageId) {
      return;
    }

    item.querySelector("[data-existing-image-delete]")?.addEventListener("click", async (event) => {
      if (!window.confirm(item.dataset.deleteMessage || "この画像を削除しますか？")) {
        return;
      }
      const button = event.currentTarget;
      button.disabled = true;
      item.classList.add("is-marked-for-delete");
      try {
        await deleteExistingImage(item.dataset.deleteUrl);
        deletedImageIds.add(imageId);
        item.hidden = true;
        trimSelectedFiles();
        render();
      } catch (error) {
        item.classList.remove("is-marked-for-delete");
        button.disabled = false;
        window.alert("画像を削除できませんでした。画面を再読み込みしてからもう一度お試しください。");
      }
    });
  });

  list.addEventListener("click", (event) => {
    const remove = event.target.closest("[data-remove-index]");
    if (!remove) {
      return;
    }
    selectedFiles.splice(Number(remove.dataset.removeIndex), 1);
    syncInputFiles();
    render();
  });

  render();
})();
