// /static/js/location/form.js
document.addEventListener("DOMContentLoaded", () => {
  const form = document.getElementById("editForm");
  if (!form) return;

  // ===== ここから既存の処理（未保存検知） =====
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

  // ✅ 保存ボタンへの対応（form属性で紐づくボタン）
  const saveButton = document.querySelector('button[type="submit"][form="editForm"]');
  if (saveButton) {
    saveButton.addEventListener("click", (e) => {
      if (isDirty) {
        const result = confirm("変更内容を保存しますか？");
        if (!result) {
          e.preventDefault();
        } else {
          isDirty = false; // ✅ 明示的に解除
        }
      }
    });
  }

  // ✅ <a href="..."> に対応
  document.querySelectorAll("a.navigable").forEach(link => {
    link.addEventListener("click", e => {
      if (isDirty) {
        const result = confirm("保存されていない変更があります。移動してもよろしいですか？");
        if (!result) {
          e.preventDefault();
        }
      }
    });
  });

  // ✅ 戻る以外の .navigable ボタンに対応（保存ボタンは除外）
  document.querySelectorAll("button.navigable").forEach(btn => {
    if (btn.getAttribute("form") !== "editForm" && !btn.classList.contains("back-btn")) {
      btn.addEventListener("click", e => {
        if (isDirty) {
          const result = confirm("保存されていない変更があります。移動してもよろしいですか？");
          if (!result) {
            e.preventDefault();
          }
        }
      });
    }
  });

  // ✅ 念のため：form送信時にも isDirty を解除
  form.addEventListener("submit", () => {
    isDirty = false;
  });
  // ===== ここまで既存の処理 =====

  // ★追加: 戻るボタン専用（OKで保存 / キャンセルで移動）
  const backBtn = document.querySelector("button.back-btn");
  if (backBtn) {
    backBtn.addEventListener("click", (e) => {
      const goTo = backBtn.getAttribute("data-href") || "/";
      if (!isDirty) { window.location.href = goTo; return; }
      const saveFirst = confirm("変更があります。保存してから戻りますか？\n［OK］保存して戻る / ［キャンセル］保存せずに戻る");
      if (saveFirst) { e.preventDefault(); form.submit(); }
      else { e.preventDefault(); window.location.href = goTo; }
    });
  }

  // ★★追加: ここから重複チェック（送信時に必ず実施）★★
  const formMode =
    document.querySelector(".form-title")?.textContent?.includes("編集") ? "edit" : "new";
  const nameInput = document.getElementById("locationNameInput") || form.querySelector('input[name="name"]');
  const nameHelp  = document.getElementById("locationNameHelp");

  async function checkNameExists(name) {
    if (!name) return false;
    const resp = await fetch(`/location/api/exists?name=${encodeURIComponent(name)}`, { method: "GET" });
    if (!resp.ok) throw new Error("check failed");
    const data = await resp.json();
    return !!data.exists;
  }

  // 任意：ライブチェック（新規時のみ）
  let timer;
  nameInput?.addEventListener("input", () => {
    if (formMode === "edit") return;
    clearTimeout(timer);
    timer = setTimeout(async () => {
      try {
        const exists = await checkNameExists(nameInput.value.trim());
        if (exists) {
          nameHelp && (nameHelp.textContent = "この管理場所名はすでに使われています。");
          nameInput.classList.add("is-invalid");
        } else {
          nameHelp && (nameHelp.textContent = "");
          nameInput.classList.remove("is-invalid");
        }
      } catch {
        // 送信時にもう一度チェックするのでここは無視
      }
    }, 300);
  });

  // 送信前フック：新規時は重複なら止める／チェック失敗でも止める
  form.addEventListener("submit", async (e) => {
    if (formMode !== "new" || !nameInput) { isDirty = false; return; }
    e.preventDefault();
    const val = nameInput.value.trim();
    try {
      const exists = await checkNameExists(val);
      if (exists) {
        alert("この管理場所名は既に存在します。別の名前を入力してください。");
        nameInput.focus();
        nameInput.classList.add("is-invalid");
        nameHelp && (nameHelp.textContent = "別の管理場所名を入力してください。");
        return; // 送信しない
      }
    } catch {
      // ← ユーザー要件：「だめだったら、とめる」
      alert("管理場所名が重複しているため、登録できません。");
      return; // 送信しない
    }
    // 重複なし → 送信
    isDirty = false;
    form.submit();
  });

  // ★追加: ページ離脱時の警告
  window.addEventListener("beforeunload", (e) => {
    if (isDirty) { e.preventDefault(); e.returnValue = ""; }
  });
});
