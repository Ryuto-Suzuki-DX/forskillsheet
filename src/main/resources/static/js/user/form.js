// /static/js/user/form.js
document.addEventListener("DOMContentLoaded", () => {
  const form = document.getElementById("editForm");
  if (!form) return;

  // ====== 変更検知（既存ロジック） ======
  const originalValues = {};
  const inputs = form.querySelectorAll("input, select, textarea");

  inputs.forEach(input => {
    if (input.name) {
      originalValues[input.name] = input.value;
    }
  });

  let isDirty = false;

  inputs.forEach(input => {
    input.addEventListener("input", () => {
      isDirty = [...inputs].some(i =>
        i.name && i.value !== originalValues[i.name]
      );
    });
  });

  // ====== 保存ボタン用（既存） ======
  const saveButton = document.querySelector('button[type="submit"][form="editForm"]');
  if (saveButton) {
    saveButton.addEventListener("click", (e) => {
      if (isDirty) {
        const result = confirm("変更内容を保存しますか？");
        if (!result) {
          e.preventDefault();
        } else {
          isDirty = false; // 明示解除
        }
      }
    });
  }

  // ====== ナビゲーション保護（既存） ======
  document.querySelectorAll("a.navigable").forEach(link => {
    link.addEventListener("click", e => {
      if (isDirty) {
        const result = confirm("保存されていない変更があります。移動してもよろしいですか？");
        if (!result) e.preventDefault();
      }
    });
  });

  document.querySelectorAll("button.navigable").forEach(btn => {
    if (btn.getAttribute("form") !== "editForm") {
      btn.addEventListener("click", e => {
        if (isDirty) {
          const result = confirm("保存されていない変更があります。移動してもよろしいですか？");
          if (!result) e.preventDefault();
        }
      });
    }
  });

  // ====== ここから重複チェックの追加 ======
  // [追加] フォームモード推定（タイトルの文言で判定：編集/新規）
  const formMode =
    document.querySelector(".form-title")?.textContent?.includes("編集") ? "edit" : "new"; // [追加]

  // [追加] ユーザー名入力/ヘルプ要素取得（HTML側でid付与済み）
  const usernameInput = document.getElementById("usernameInput") || form.querySelector('input[name="username"]'); // [追加]
  const usernameHelp  = document.getElementById("usernameHelp"); // [追加]

  // [追加] API呼び出し関数（軽量GET、CSRF不要）
  async function checkUsernameExists(name) { // [追加]
    if (!name) return false;
    const resp = await fetch(`/user/api/exists?username=${encodeURIComponent(name)}`, { method: "GET" });
    if (!resp.ok) throw new Error("ユーザー名チェックに失敗");
    const data = await resp.json();
    return !!data.exists;
  }

  // [追加] ライブチェック（新規時のみ）
  let checkTimer; // [追加]
  if (usernameInput) { // [追加]
    usernameInput.addEventListener("input", () => { // [追加]
      if (formMode === "edit") return; // 編集では重複チェック不要
      clearTimeout(checkTimer);
      checkTimer = setTimeout(async () => {
        try {
          const exists = await checkUsernameExists(usernameInput.value.trim());
          if (exists) {
            usernameHelp && (usernameHelp.textContent = "このユーザー名はすでに使われています。"); // [追加]
            usernameInput.classList.add("is-invalid"); // [追加]
          } else {
            usernameHelp && (usernameHelp.textContent = ""); // [追加]
            usernameInput.classList.remove("is-invalid"); // [追加]
          }
        } catch {
          // ネットワーク失敗時は黙ってスルー（送信時に再チェック）
        }
      }, 300);
    });
  }

  // [修正後] 送信前フック：新規時は重複なら送信ブロック
form.addEventListener("submit", async (e) => {
  if (formMode !== "new" || !usernameInput) {
    isDirty = false;
    return;
  }
  e.preventDefault(); // いったん止める
  const name = usernameInput.value.trim();
  try {
    const exists = await checkUsernameExists(name);
    if (exists) {
      alert("このユーザー名は既に存在します。別の名前を入力してください。");
      usernameInput.focus();
      usernameInput.classList.add("is-invalid");
      usernameHelp && (usernameHelp.textContent = "別のユーザー名を入力してください。");
      return; // 送信しない
    }
  } catch {
    // [変更] ここで続行確認はせず、常にエラー扱いで送信中止
    alert("ユーザー名が重複しているため、登録ができません。");
    return; // 送信しない
  }
  // 重複なし → 送信再開
  isDirty = false;
  form.submit();
});

  // ====== 既存：送信時に isDirty を解除 ======
  form.addEventListener("submit", () => {
    isDirty = false;
  });
});
