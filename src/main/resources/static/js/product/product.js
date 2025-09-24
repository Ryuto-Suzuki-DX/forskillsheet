// /static/js/product/product.js
document.addEventListener("DOMContentLoaded", () => {
  // === 要素取得 ===
  const editForm = document.getElementById("editForm");
  const deleteForm = document.getElementById("deleteForm");
  const editBtn = document.getElementById("editBtn");
  const deleteBtn = document.getElementById("deleteBtn");

  const editProductIdInput = document.getElementById("editProductId");
  const deleteProductIdInput = document.getElementById("deleteProductId");

  const rows = document.querySelectorAll("tbody tr[data-id]");

  // 選択中行取得
  const getSelectedRow = () => document.querySelector("tbody tr.selected");

  // 選択処理
  const setSelection = (row) => {
    // 既存選択解除（オレンジ背景も消す）
    document.querySelectorAll("tbody tr.selected").forEach(tr => {
      tr.classList.remove("selected");
    });

    // 新選択
    row.classList.add("selected"); // ← CSSで背景オレンジに
    const selectedId = row.getAttribute("data-id");

    // hiddenに反映
    if (editProductIdInput) editProductIdInput.value = selectedId;
    if (deleteProductIdInput) deleteProductIdInput.value = selectedId;

    // ボタン活性化
    if (editBtn) editBtn.disabled = false;
    if (deleteBtn) deleteBtn.disabled = false;
  };

  // === 行クリックイベント設定 ===
  rows.forEach(row => {
    // 単クリック → 選択＆オレンジ背景
    row.addEventListener("click", () => setSelection(row));

    // ダブルクリック → 選択して即編集フォーム送信
    row.addEventListener("dblclick", () => {
      setSelection(row);
      if (editForm) editForm.submit();
    });
  });

  // === 編集ボタン動作 ===
  if (editBtn && editForm) {
    editBtn.addEventListener("click", (e) => {
      if (!editProductIdInput?.value) {
        e.preventDefault();
        alert("編集する製品を選択してください。");
      }
    });
  }

  // === 削除ボタン動作 ===
  if (deleteBtn && deleteForm) {
    deleteBtn.addEventListener("click", (e) => {
      const selectedRow = getSelectedRow();
      const selectedId = deleteProductIdInput?.value;

      if (!selectedRow || !selectedId) {
        e.preventDefault();
        alert("削除する製品を選択してください。");
        return;
      }

      // 表示用の製品コード・製品名取得
      let code = selectedRow.cells[0]?.textContent?.trim() || "";
      let name = selectedRow.cells[1]?.textContent?.trim() || "";

      const ok = confirm(`製品コード ${code} / 製品名 ${name} を削除しますか？（ID: ${selectedId}）`);
      if (!ok) e.preventDefault();
    });
  }

  // === 初期状態でボタン無効化 ===
  if (editBtn) editBtn.disabled = true;
  if (deleteBtn) deleteBtn.disabled = true;
});
