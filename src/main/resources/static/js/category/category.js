document.addEventListener("DOMContentLoaded", function () {
  // === 要素参照 ===
  const editForm = document.getElementById("editForm");
  const deleteForm = document.getElementById("deleteForm");
  const editBtn = document.getElementById("editBtn");
  const deleteBtn = document.getElementById("deleteBtn");

  const editCategoryIdInput = document.getElementById("editCategoryId");
  const deleteCategoryIdInput = document.getElementById("deleteCategoryId");

  // 選択可能な行（th:each で生成された tr）
  const rows = document.querySelectorAll("tr[data-id]");

  // === 選択解除・設定関数 ===
  function clearSelection() {
    document.querySelectorAll("tr.selected").forEach(tr => tr.classList.remove("selected"));
  }

  function setSelection(row) {
    clearSelection();
    row.classList.add("selected");

    const selectedId = row.getAttribute("data-id");
    console.log(selectedId);

    // hiddenに反映
    if (editCategoryIdInput) editCategoryIdInput.value = selectedId;
    if (deleteCategoryIdInput) deleteCategoryIdInput.value = selectedId;

    // ボタン有効化
    if (editBtn) editBtn.disabled = false;
    if (deleteBtn) deleteBtn.disabled = false;
  }

  function getSelectedRow() {
    return document.querySelector("tr.selected");
  }

  // === 行クリック/ダブルクリックイベント ===
  rows.forEach(row => {
    // シングルクリック：選択
    row.addEventListener("click", () => {
      setSelection(row);
    });

    // ダブルクリック：即編集画面へ
    row.addEventListener("dblclick", () => {
      setSelection(row);
      console.log("ダブルクリック送信直前のID:", editCategoryIdInput.value);
      if (editForm) editForm.submit();
      
    });
  });

  // === 編集ボタン ===
  if (editBtn && editForm) {
    editBtn.addEventListener("click", (e) => {
      console.log("送信直前のID:", editCategoryIdInput.value);
      if (!editCategoryIdInput?.value) {
        e.preventDefault();
        alert("編集するカテゴリを選択してください。");
      }
    });
  }

  // === 削除ボタン ===
  if (deleteBtn && deleteForm) {
    deleteBtn.addEventListener("click", (e) => {
      const selectedRow = getSelectedRow();
      const selectedId = deleteCategoryIdInput?.value;

      if (!selectedRow || !selectedId) {
        e.preventDefault();
        alert("削除するカテゴリを選択してください。");
        return;
      }

      // 表示用データ（IDと名前）
      const code = selectedRow.cells[0]?.textContent?.trim() ?? "";
      const name = selectedRow.cells[1]?.textContent?.trim() ?? "";

      const ok = confirm(`カテゴリーID ${code} / カテゴリー名 ${name} を削除しますか？（内部ID: ${selectedId}）`);
      if (!ok) e.preventDefault();
    });
  }
});
