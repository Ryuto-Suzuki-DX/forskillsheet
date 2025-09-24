// /static/js/user/user.js
document.addEventListener("DOMContentLoaded", function () {
  // ==== 要素参照 ====
  const deleteForm = document.getElementById("deleteForm");
  const deleteBtn = document.getElementById("deleteBtn");
  const editForm = document.getElementById("editForm");
  const editBtn = document.getElementById("editBtn");

  const deleteUserIdInput = document.getElementById("deleteUserId");
  const editUserIdInput = document.getElementById("editUserId");

  // ログインユーザー自身は削除不可にするために meta の content を読む
  const loggedInUserId = document.getElementById("loggedInUserId")?.content ?? null;

  // テーブル行
  const rows = document.querySelectorAll("tr[data-id]");

  // 現在選択中の行
  const getSelectedRow = () => document.querySelector("tr.selected");

  // 選択セット共通処理
  function setSelection(row) {
    // 既存の選択解除
    document.querySelectorAll("tr.selected").forEach(tr => tr.classList.remove("selected"));

    // 新しい選択
    row.classList.add("selected");
    const selectedId = row.getAttribute("data-id");

    // hidden に反映
    if (editUserIdInput) editUserIdInput.value = selectedId;
    if (deleteUserIdInput) deleteUserIdInput.value = selectedId;

    // ボタン活性/非活性
    if (editBtn) editBtn.disabled = false;

    if (deleteBtn) {
      // 自分自身（ログインID）なら削除無効化
      const shouldDisable =
        loggedInUserId != null && String(selectedId) === String(loggedInUserId);
      deleteBtn.disabled = shouldDisable;
    }
  }

  // ==== 行クリックで選択 ====
  rows.forEach(row => {
    row.addEventListener("click", function () {
      setSelection(row);
    });

    // ダブルクリックで編集へ
    row.addEventListener("dblclick", function () {
      setSelection(row);
      if (editForm) editForm.submit();
    });
  });

  // ==== 編集ボタン 事前チェック ====
  if (editBtn && editForm) {
    editBtn.addEventListener("click", function (event) {
      const selectedId = editUserIdInput?.value;
      if (!selectedId) {
        event.preventDefault();
        alert("編集するユーザーを選択してください。");
        return;
      }
      // 何もしなければ通常通り GET /user/edit に遷移
    });
  }

  

  // ==== 削除ボタン 確認ダイアログ ====
  if (deleteBtn && deleteForm) {
    deleteBtn.addEventListener("click", function (event) {
      // 1) 未選択チェック
      const selectedRow = getSelectedRow();
      const selectedId = deleteUserIdInput?.value;

      if (!selectedRow || !selectedId) {
        event.preventDefault();
        alert("削除するユーザーを選択してください。");
        return;
      }

      // 2) ログインユーザー自身は削除不可（保険）
      if (loggedInUserId != null && String(selectedId) === String(loggedInUserId)) {
        event.preventDefault();
        alert("ログイン中のユーザーは削除できません。");
        return;
      }

      // 3) 表示用にユーザー名などを拾ってメッセージに含める（任意）
      //  テーブルが [ID, username, name, role, ...] 構成なら username は2列目(index 1)
      let usernameForMessage = "";
      try {
        const usernameCell = selectedRow.cells[1]; // 0:ID, 1:username と仮定
        usernameForMessage = usernameCell ? `（${usernameCell.textContent.trim()}）` : "";
      } catch (_) {
        // 取得できなくても特に問題なし
      }

      // 4) 確認ダイアログ
      const ok = confirm(`ユーザーID ${selectedId}${usernameForMessage} を削除しますか？`);
      if (!ok) {
        // キャンセル → 送信中止
        event.preventDefault();
        return;
      }

      // OK → そのまま POST /user/delete 送信（CSRF は HTML に既に埋め込み済み）
    });
  }
});
