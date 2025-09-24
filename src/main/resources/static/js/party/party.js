document.addEventListener("DOMContentLoaded", function () {
  // === 要素参照 ===
  const editForm = document.getElementById("editForm");
  const deleteForm = document.getElementById("deleteForm");
  const editBtn = document.getElementById("editBtn");
  const deleteBtn = document.getElementById("deleteBtn");

  const editPartyIdInput = document.getElementById("editPartyId");
  const deletePartyIdInput = document.getElementById("deletePartyId");

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

    // hiddenに反映
    if (editPartyIdInput) editPartyIdInput.value = selectedId;
    if (deletePartyIdInput) deletePartyIdInput.value = selectedId;

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
      if (editForm) editForm.submit();
    });
  });

  // === 編集ボタン ===
  if (editBtn && editForm) {
    editBtn.addEventListener("click", (e) => {
      if (!editPartyIdInput?.value) {
        e.preventDefault();
        alert("編集するカテゴリを選択してください。");
      }
    });
  }

  // === 削除ボタン ===
  if (deleteBtn && deleteForm) {
    deleteBtn.addEventListener("click", (e) => {
      const selectedRow = getSelectedRow();
      const selectedId = deletePartyIdInput?.value;

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
