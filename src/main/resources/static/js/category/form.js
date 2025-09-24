document.addEventListener("DOMContentLoaded", () => {
    const form = document.getElementById("editForm");
    if (!form) return;

    // ===== ここから既存の処理 =====

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

    // ✅ 戻るボタンなどに対応（保存ボタンは除外）
    document.querySelectorAll("button.navigable").forEach(btn => {
        if (btn.getAttribute("form") !== "editForm") {
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

    // ★追加: 戻るボタン専用の「OKで保存 / キャンセルで移動」制御
    //   - HTML 側で以下のように back-btn を付け、遷移先を data-href で指定してください
    //     <button type="button" class="navigable back-btn" data-href="/category/search">戻る</button>
    const backBtn = document.querySelector("button.back-btn");
    if (backBtn) {
        backBtn.addEventListener("click", (e) => {
            const goTo = backBtn.getAttribute("data-href") || "/";

            if (!isDirty) {
                // 未変更ならそのまま移動
                window.location.href = goTo;
                return;
            }

            // 変更あり: 保存してから戻るかどうか
            const saveFirst = confirm("変更があります。保存してから戻りますか？\n［OK］保存して戻る / ［キャンセル］保存せずに戻る");
            if (saveFirst) {
                // OK → 保存（フォーム送信）。サーバ側のPRGで戻り先に遷移させてください
                e.preventDefault();
                form.submit();
            } else {
                // キャンセル → 保存せず指定先へ移動
                e.preventDefault();
                window.location.href = goTo;
            }
        });
    }

    // ★追加: ページ離脱時の警告（ブラウザの戻る/閉じるもカバー）
    window.addEventListener("beforeunload", (e) => {
        if (isDirty) {
            e.preventDefault();
            e.returnValue = ""; // Chrome/Edgeで標準ダイアログを出すため必須
        }
    });
});
