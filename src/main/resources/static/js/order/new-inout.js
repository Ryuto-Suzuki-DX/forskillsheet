// ==============================
// CSRF token helper
// ==============================
const CSRF_TOKEN  = document.querySelector('meta[name="_csrf"]')?.content;
const CSRF_HEADER = document.querySelector('meta[name="_csrf_header"]')?.content;
const csrfHeaders = () => (CSRF_TOKEN && CSRF_HEADER ? { [CSRF_HEADER]: CSRF_TOKEN } : {});

document.addEventListener("DOMContentLoaded", () => {
  const form = document.querySelector("form.order-form");
  if (!form) return;

  // ========= 画面状態 =========
  const mode = (form.getAttribute("data-mode") || "IN").toUpperCase(); // "IN" | "OUT"
  const isOutMode = mode === "OUT";
  const idInput = form.querySelector('input[name="id"]');

  // ========= 未保存チェック & 保存中フラグ =========
  const originalValues = {};
  const formInputs = form.querySelectorAll("input, select, textarea");
  formInputs.forEach(i => { if (i.name) originalValues[i.name] = i.value; });

  let isDirty = false;
  let isSubmitting = false;

  const checkDirty = () => {
    isDirty = [...formInputs].some(i => i.name && i.value !== originalValues[i.name]);
  };
  formInputs.forEach(i => {
    i.addEventListener("input", checkDirty);
    i.addEventListener("change", checkDirty);
  });

  const saveButton = form.querySelector('button[type="submit"]');

  const setSubmitEnabled = (enabled) => {
    if (!saveButton) return;
    saveButton.disabled = !enabled;
    saveButton.style.opacity = enabled ? "1" : ".6";
    saveButton.style.cursor = enabled ? "pointer" : "not-allowed";
    saveButton.title = enabled ? "" : "在庫数を超えている入力があります";
  };

  if (saveButton) {
    saveButton.addEventListener("click", (e) => {
      if (isDirty && !confirm("変更内容を保存しますか？")) {
        e.preventDefault();
      } else {
        isDirty = false;
      }
    });
  }

  // ========= ページ離脱（保存時は抑止） =========
  window.addEventListener("beforeunload", (e) => {
    if (isSubmitting) return;
    if (isDirty) { e.preventDefault(); e.returnValue = ""; }
    try {
      const oid = idInput?.value;
      if (oid) {
        const data = JSON.stringify({ orderId: oid });
        const url = `${window.location.origin}/order/exit`;
        navigator.sendBeacon(url, new Blob([data], { type: "application/json" }));
      }
    } catch (err) {
      console.error("離脱通知失敗", err);
    }
  });

  // ========= party 補完 =========
  const partyCodeInput = document.getElementById("partyCode");
  const partyNameInput = document.getElementById("partyName");
  const partyIdHidden  = document.getElementById("partyIdHidden");

  function syncPartyByCode(code) {
    const match = (typeof partyList !== "undefined") ? partyList.find(p => p.partyCode === code) : null;
    if (!match) { if (partyNameInput) partyNameInput.value = ""; if (partyIdHidden) partyIdHidden.value = ""; return; }
    if (partyNameInput) partyNameInput.value = match.partyName ?? "";
    if (partyIdHidden)  partyIdHidden.value  = (match.id ?? "");
  }
  function syncPartyByName(name) {
    const match = (typeof partyList !== "undefined") ? partyList.find(p => p.partyName === name) : null;
    if (!match) { if (partyCodeInput) partyCodeInput.value = ""; if (partyIdHidden) partyIdHidden.value = ""; return; }
    if (partyCodeInput) partyCodeInput.value = match.partyCode ?? "";
    if (partyIdHidden)  partyIdHidden.value  = (match.id ?? "");
  }
  if (partyCodeInput) partyCodeInput.addEventListener("input", () => syncPartyByCode(partyCodeInput.value.trim()));
  if (partyNameInput) partyNameInput.addEventListener("input", () => syncPartyByName(partyNameInput.value.trim()));
  if (partyCodeInput?.value) syncPartyByCode(partyCodeInput.value.trim());
  else if (partyNameInput?.value) syncPartyByName(partyNameInput.value.trim());

  // ========= 登録製品テーブル =========
  const selectedTbody = document.getElementById("selectedProducts");

  function reindexSelectedProducts() {
    if (!selectedTbody) return;
    const rows = selectedTbody.querySelectorAll("tr");
    rows.forEach((row, idx) => {
      row.querySelectorAll("input[name^='productWithDetailsDtos[']").forEach(input => {
        const at = input.name.indexOf("].");
        const field = at >= 0 ? input.name.substring(at + 2) : "";
        input.name = `productWithDetailsDtos[${idx}].${field}`;
      });
    });
  }

  function parseFiniteInt(v) {
    const n = parseInt(v, 10);
    return Number.isFinite(n) ? n : NaN;
  }

  function getRowStock(row) {
    const s = parseFiniteInt(row?.dataset?.stock);
    return Number.isFinite(s) ? s : NaN; // NaN = 在庫不明（チェック対象外）
  }

  function warnHost(tdEl) {
    let host = tdEl.querySelector(".stock-warn");
    if (!host) {
      host = document.createElement("div");
      host.className = "stock-warn";
      host.style.marginTop = "4px";
      host.style.fontSize = "12px";
      host.style.color = "#dc3545";
      host.style.lineHeight = "1.3";
      tdEl.appendChild(host);
    }
    return host;
  }
  function clearWarn(tdEl) {
    const host = tdEl.querySelector(".stock-warn");
    if (host) host.remove();
  }

  function validateRow(row) {
    if (!isOutMode) return true; // 出庫のみチェック

    const qtyInput = row.querySelector('input[type="number"][name$=".quantity"]');
    if (!qtyInput) return true;

    const td = qtyInput.closest("td") || qtyInput.parentElement;
    const stock = getRowStock(row);
    const raw = parseFiniteInt(qtyInput.value || "0");
    let qty = Number.isFinite(raw) ? raw : 0;

    // 0以下は1へクランプ
    if (qty < 1) qty = 1;

    if (!Number.isFinite(stock)) {
      // 在庫不明はスキップ（ブロックしない）
      clearWarn(td);
      qtyInput.value = String(qty);
      qtyInput.classList.remove("over-stock");
      qtyInput.style.outline = "";
      qtyInput.style.background = "";
      qtyInput.removeAttribute("aria-invalid");
      return true;
    }

    if (qty > stock) {
      // その場で在庫上限へクランプしてブロック
      qty = stock;
      qtyInput.value = String(stock);
      const host = warnHost(td);
      const nameTd = row.children[1];
      const name = (nameTd?.textContent || "製品").trim();
      host.textContent = `在庫数（${stock}）を超えていたため、${name} の数量を ${stock} に調整しました。`;
      qtyInput.classList.add("over-stock");
      qtyInput.setAttribute("aria-invalid", "true");
      qtyInput.style.outline = "2px solid #dc3545";
      qtyInput.style.background = "#2a1214";
      return false;
    } else {
      clearWarn(td);
      qtyInput.classList.remove("over-stock");
      qtyInput.removeAttribute("aria-invalid");
      qtyInput.style.outline = "";
      qtyInput.style.background = "";
      qtyInput.value = String(qty);
      return true;
    }
  }

  function validateAll() {
    if (!selectedTbody) { setSubmitEnabled(true); return true; }
    const rows = Array.from(selectedTbody.querySelectorAll("tr"));
    // 1行でも false があれば送信不可
    const allOk = rows.every(r => validateRow(r));
    setSubmitEnabled(allOk);
    return allOk;
  }

  // 初期行の productId/dataset を整備＆重複統合
  (function normalizeInitialRows() {
    if (!selectedTbody) return;
    const rows = selectedTbody.querySelectorAll("tr");

    // もし data-id 未設定なら hidden の id から埋める
    rows.forEach((row) => {
      if (!row.dataset.id) {
        const hid = row.querySelector("input[type='hidden'][name$='.id']");
        if (hid?.value) row.dataset.id = String(hid.value);
      }
    });

    // 重複統合（数量加算）
    const seen = new Map(); // pid -> qtyInput
    Array.from(rows).forEach(row => {
      const pid = row.dataset.id;
      const qtyInput = row.querySelector("input[type='number'][name$='.quantity']");
      if (!pid || !qtyInput) return;
      if (!seen.has(pid)) {
        seen.set(pid, qtyInput);
      } else {
        const prev = seen.get(pid);
        const sum = (parseInt(prev.value || "0", 10) || 0) + (parseInt(qtyInput.value || "0", 10) || 0);
        prev.value = String(sum);
        row.remove();
      }
    });

    reindexSelectedProducts();
    validateAll();
  })();

  // 数量入力：都度検証 & ボタン状態更新
  selectedTbody?.addEventListener("input", (e) => {
    if (e.target.matches('input[type="number"][name$=".quantity"]')) {
      validateRow(e.target.closest("tr"));
      validateAll();
      isDirty = true;
    }
  });

  function findRowByProductId(productId) {
    if (!selectedTbody) return null;
    return Array.from(selectedTbody.querySelectorAll("tr")).find(r => r.dataset.id === String(productId));
  }

  // ========= 製品検索 =========
  const searchBtn = document.getElementById("searchProductBtn");
  if (searchBtn) {
    searchBtn.addEventListener("click", () => {
      const keyword    = document.getElementById("searchKeyword")?.value || "";
      const categoryId = document.getElementById("searchCategory")?.value || "";
      const locationId = document.getElementById("searchLocation")?.value || "";

      const url = `/product/api/search?name=${encodeURIComponent(keyword)}&categoryIds=${encodeURIComponent(categoryId)}&locationIds=${encodeURIComponent(locationId)}`;

      fetch(url)
        .then(res => { if (!res.ok) throw new Error("検索APIエラー"); return res.json(); })
        .then(data => {
          const tbody = document.querySelector("#searchResultsTable tbody");
          if (!tbody) return;
          tbody.innerHTML = "";
          (data || []).forEach(p => {
            const cats = Array.isArray(p.categories) ? p.categories.map(c => c.name).join(", ") : "-";
            const locs = Array.isArray(p.locations)  ? p.locations.map(l => l.name).join(", ")  : "-";
            const stock = parseFiniteInt(p.totalQuantity) || 0;
            const tr = document.createElement("tr");
            tr.innerHTML = `
              <td>${p.productCode ?? ""}</td>
              <td>${p.name ?? ""}</td>
              <td>${cats}</td>
              <td>${locs}</td>
              <td>${stock}</td>
              <td>
                <button type="button" class="add-btn"
                  data-id="${p.id}"
                  data-code="${p.productCode ?? ""}"
                  data-name="${p.name ?? ""}"
                  data-cats="${cats}"
                  data-locs="${locs}"
                  data-stock="${stock}"
                >追加</button>
              </td>
            `;
            tbody.appendChild(tr);
          });
        })
        .catch(err => console.error(err));
    });
  }

  // ========= 追加・削除 =========
  document.addEventListener("click", (e) => {
    // 追加
    const addBtn = e.target.closest?.(".add-btn");
    if (addBtn) {
      const productId   = addBtn.dataset.id;
      const productCode = addBtn.dataset.code || "";
      const productName = addBtn.dataset.name || "";
      const catsText    = addBtn.dataset.cats || "-";
      const locsText    = addBtn.dataset.locs || "-";
      const stock       = parseFiniteInt(addBtn.dataset.stock) || 0;

      const existing = findRowByProductId(productId);
      if (existing) {
        // 既存行に +1（出庫時のみ在庫超過ブロック）
        const qtyInput = existing.querySelector("input[type='number'][name$='.quantity']");
        const cur = parseFiniteInt(qtyInput?.value) || 0;
        const next = cur + 1;

        if (isOutMode) {
          const rowStock = Number.isFinite(getRowStock(existing)) ? getRowStock(existing) : stock;
          if (Number.isFinite(rowStock) && next > rowStock) {
            alert(`在庫数（${rowStock}）を超えるため追加できません。`);
            return;
          }
        }
        qtyInput.value = String(next);
        validateRow(existing);
        validateAll();
        isDirty = true;
        return;
      }

      // 新規行を追加（在庫0 & 出庫なら拒否）
      if (isOutMode && stock <= 0) {
        alert("在庫が 0 のため追加できません。");
        return;
      }

      const index = selectedTbody.querySelectorAll("tr").length;
      const tr = document.createElement("tr");
      tr.dataset.id = String(productId);
      // 在庫を tr に保存（出庫バリデーション用）
      if (isOutMode) tr.dataset.stock = String(stock);

      tr.innerHTML = `
        <td>
          ${productCode}
          <input type="hidden" name="productWithDetailsDtos[${index}].id" value="${productId}">
          <input type="hidden" name="productWithDetailsDtos[${index}].productCode" value="${productCode}">
          <input type="hidden" name="productWithDetailsDtos[${index}].name" value="${productName}">
        </td>
        <td>${productName}</td>
        <td>${catsText}</td>
        <td>${locsText}</td>
        <td>
          <input type="number" name="productWithDetailsDtos[${index}].quantity" value="1" min="1">
        </td>
        <td><button type="button" class="remove-btn">削除</button></td>
      `;
      selectedTbody.appendChild(tr);
      reindexSelectedProducts();

      // 追加直後に検証（クランプ・表示・送信可否）
      validateRow(tr);
      validateAll();
      isDirty = true;
      return;
    }

    // 削除
    const removeBtn = e.target.closest?.(".remove-btn");
    if (removeBtn) {
      removeBtn.closest("tr")?.remove();
      reindexSelectedProducts();
      validateAll();
      isDirty = true;
      return;
    }
  });

  // ========= 送信直前：最終チェック =========
  form.addEventListener("submit", (e) => {
    // ここで最終検証（在庫超過があれば送信ブロック）
    if (!validateAll()) {
      e.preventDefault();
      alert("在庫数を超えている行があります。数量を修正してください。");
      return;
    }
    isSubmitting = true;
    // partyId の補完保険
    if (partyIdHidden && !partyIdHidden.value) {
      if (partyCodeInput?.value) syncPartyByCode(partyCodeInput.value.trim());
      else if (partyNameInput?.value) syncPartyByName(partyNameInput.value.trim());
    }
    isDirty = false;
  });

  // =========================================================
  // 画像モーダル（必要ならここから下は既存のまま）
  // =========================================================
  const openOrderImageBtn = document.getElementById("openOrderImageModal");
  const orderImageModal   = document.getElementById("orderImageModal");
  const modalCloseEls     = orderImageModal ? orderImageModal.querySelectorAll(".image-modal__close, .image-modal__backdrop") : [];
  const opFileInput = orderImageModal?.querySelector("#op-file");
  const opUploadBtn = orderImageModal?.querySelector("#op-upload");
  const opListBody  = orderImageModal?.querySelector("#op-list");

  function orderIdOrThrow() {
    const v = idInput?.value?.trim();
    if (!v || !/^\d+$/.test(v)) throw new Error("order id is not prepared");
    return parseInt(v, 10);
  }
  function updateOrderImageButton(count) {
    if (!openOrderImageBtn) return;
    const base = "添付画像";
    if (count > 0) {
      openOrderImageBtn.classList.add("has-images");
      openOrderImageBtn.textContent = `${base} (${count})`;
    } else {
      openOrderImageBtn.classList.remove("has-images");
      openOrderImageBtn.textContent = base;
    }
  }
  function openModal() { if (orderImageModal) orderImageModal.classList.add("is-open"); }
  function closeModal() {
    if (!orderImageModal) return;
    orderImageModal.classList.remove("is-open");
    if (opListBody) opListBody.innerHTML = "";
    if (opFileInput) opFileInput.value = "";
  }
  async function loadOrderPictures(orderId) {
    if (!opListBody) return [];
    opListBody.innerHTML = '<tr><td colspan="3">読み込み中...</td></tr>';
    try {
      const res = await fetch(`/order/api/${encodeURIComponent(orderId)}/pictures`, { cache: "no-store" });
      if (!res.ok) throw new Error("failed to fetch images");
      const list = await res.json();
      if (!Array.isArray(list) || list.length === 0) {
        opListBody.innerHTML = '<tr><td colspan="3">画像はありません</td></tr>';
        return [];
      }
      opListBody.innerHTML = list.map(pic => {
        const url = pic.filePath;
        const id  = pic.id;
        const fn  = pic.fileName || (url ? url.split('/').pop() : ('ID:' + id));
        return `
          <tr data-id="${id}">
            <td><img src="${url}" alt="${fn}" style="max-width:110px;max-height:80px;object-fit:cover;border-radius:6px;"></td>
            <td><a href="${url}" target="_blank" rel="noopener">${url}</a></td>
            <td><button type="button" class="img-btn op-del" data-id="${id}">削除</button></td>
          </tr>
        `;
      }).join("");
      return list;
    } catch (e) {
      console.error(e);
      opListBody.innerHTML = '<tr><td colspan="3" style="color:#b52a37;">読み込みに失敗しました</td></tr>';
      return [];
    }
  }
  async function uploadOrderPictures(orderId) {
    if (!opFileInput || !opUploadBtn) return;
    const files = Array.from(opFileInput.files || []); if (files.length === 0) return;
    const fd = new FormData(); files.forEach(f => fd.append("files", f));
    opUploadBtn.disabled = true; opUploadBtn.textContent = "アップロード中...";
    try {
      const res = await fetch(`/order/api/${encodeURIComponent(orderId)}/pictures`, {
        method: "POST", body: fd, headers: csrfHeaders()
      });
      if (!res.ok) {
        const text = await res.text().catch(()=> ""); throw new Error(`upload failed: ${res.status} ${res.statusText} ${text}`);
      }
      const list = await loadOrderPictures(orderId);
      updateOrderImageButton(list.length);
      opFileInput.value = "";
    } catch (e) {
      console.error(e); alert(`アップロードに失敗しました\n${e.message}`);
    } finally {
      opUploadBtn.disabled = false; opUploadBtn.textContent = "アップロード";
    }
  }
  async function deleteOrderPicture(orderId, pictureId) {
    if (!confirm("この画像を削除しますか？")) return;
    try {
      const res = await fetch(`/order/api/${encodeURIComponent(orderId)}/pictures/${encodeURIComponent(pictureId)}`, {
        method: "DELETE", headers: csrfHeaders()
      });
      if (!res.ok) throw new Error("delete failed");
      const list = await loadOrderPictures(orderId);
      updateOrderImageButton(list.length);
    } catch (e) {
      console.error(e); alert("削除に失敗しました");
    }
  }
  if (openOrderImageBtn) {
    openOrderImageBtn.addEventListener("click", async (e) => {
      e.preventDefault();
      let oid; try { oid = orderIdOrThrow(); } catch { alert("注文IDの準備に失敗しました。ページを再読み込みしてください。"); return; }
      openModal(); const list = await loadOrderPictures(oid); updateOrderImageButton(list.length);
    });
  }
  if (opUploadBtn) opUploadBtn.addEventListener("click", async () => {
    let oid; try { oid = orderIdOrThrow(); } catch { alert("注文IDがありません"); return; }
    await uploadOrderPictures(oid);
  });
  if (opListBody) {
    opListBody.addEventListener("click", async (ev) => {
      const btn = ev.target.closest(".op-del"); if (!btn) return;
      const pid = btn.getAttribute("data-id"); if (!pid) return;
      let oid; try { oid = orderIdOrThrow(); } catch { alert("注文IDがありません"); return; }
      await deleteOrderPicture(oid, pid);
    });
  }
  if (orderImageModal) {
    const closers = Array.from(modalCloseEls || []);
    closers.forEach(el => el.addEventListener("click", () => closeModal()));
    document.addEventListener("keydown", (e) => {
      if (e.key === "Escape" && orderImageModal.classList.contains("is-open")) closeModal();
    });
  }
  // 初期ボタンバッジ同期
  try {
    const oid = orderIdOrThrow();
    fetch(`/order/api/${encodeURIComponent(oid)}/pictures`, { cache: "no-store" })
      .then(r => r.ok ? r.json() : [])
      .then(list => updateOrderImageButton(Array.isArray(list) ? list.length : 0))
      .catch(() => updateOrderImageButton(0));
  } catch { updateOrderImageButton(0); }
});
