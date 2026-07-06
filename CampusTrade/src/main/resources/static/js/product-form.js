(() => {
  const input = document.querySelector("[data-image-upload-input]");
  const status = document.querySelector("[data-image-upload-status]");
  const list = document.querySelector("[data-image-upload-list]");

  if (!input || !status || !list) {
    return;
  }

  const maxFiles = Number(input.dataset.maxFiles || 5);
  let selectedFiles = [];

  const fileKey = (file) => `${file.name}:${file.size}:${file.lastModified}`;

  const syncInputFiles = () => {
    if (typeof DataTransfer === "undefined") {
      return;
    }
    const transfer = new DataTransfer();
    selectedFiles.forEach((file) => transfer.items.add(file));
    input.files = transfer.files;
  };

  const render = () => {
    status.textContent = `${selectedFiles.length}/${maxFiles}枚`;
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
    for (const file of Array.from(input.files)) {
      if (selectedFiles.length >= maxFiles) {
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
