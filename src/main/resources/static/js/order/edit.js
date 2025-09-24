// ==============================
// CSRF token helper（最上部に定義）
// ==============================
const CSRF_TOKEN  = document.querySelector('meta[name="_csrf"]')?.content;
const CSRF_HEADER = document.querySelector('meta[name="_csrf_header"]')?.content;
// ヘッダオブジェクトを返す（トークン未取得なら空オブジェクト）
const csrfHeaders = () => (CSRF_TOKEN && CSRF_HEADER ? { [CSRF_HEADER]: CSRF_TOKEN } : {});

document.addEventListener("DOMContentLoaded", () => {
  const form = document.querySelector("form.order-form");
  if (!form) return;

  // === 追加：IN/OUT 判定（なければ IN 扱い） ===
  const mode = (form.getAttribute("data-mode") || "IN").toUpperCase(); // "IN" | "OUT"
  const isOutMode = mode === "OUT";

  // 役割・状態
  const screenFit = document.querySelector(".screen-fit");
  const role = {
    isGeneral: String(form.getAttribute("data-is-general")) === "true",
  };
  const state = {
    get isCompleted() {
      const sel = form.querySelector('select[name="situation"]');
      return (sel?.value || "") === "完了";
    }
  };

  // ========== 未保存チェック ==========
  const originalValues = {};
  const formInputs = form.querySelectorAll("input, select, textarea");
  formInputs.forEach(i => { if (i.name) originalValues[i.name] = i.value; });

  let isDirty = false;
  const checkDirty = () => {
    isDirty = [...formInputs].some(i => i.name && i.value !== originalValues[i.name]);
  };
  formInputs.forEach(i => { i.addEventListener("input", checkDirty); i.addEventListener("change", checkDirty); });

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
      if (isDirty) {
        if (!confirm("変更内容を保存しますか？")) e.preventDefault();
        else isDirty = false;
      }
    });
  }
  window.addEventListener("beforeunload", (e) => {
    if (isDirty) { e.preventDefault(); e.returnValue = ""; }
  });

  // ========== 選択済み製品テーブル ==========
  const selectedTbody = document.getElementById("selectedProducts");

  // name を productWithDetailsDtos[<idx>].<field> に振り直す
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

  // 同じ製品IDの行を探す
  function findRowByProductId(productId) {
    if (!selectedTbody) return null;
    return Array.from(selectedTbody.querySelectorAll("tr")).find(r => r.dataset.id === String(productId));
  }

  // ===== 在庫チェック系ユーティリティ =====
  const parseFiniteInt = (v) => {
    const n = parseInt(v, 10);
    return Number.isFinite(n) ? n : NaN;
  };
  const getRowStock = (row) => {
    const s = parseFiniteInt(row?.dataset?.stock);
    return Number.isFinite(s) ? s : NaN; // NaN = 在庫不明（チェック対象外）
  };
  const warnHost = (tdEl) => {
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
  };
  const clearWarn = (tdEl) => {
    const host = tdEl.querySelector(".stock-warn");
    if (host) host.remove();
  };

  // 行単位の在庫検証（OUT のみ）
  function validateRow(row) {
    if (!isOutMode) return true; // IN はチェックしない
    const qtyInput = row.querySelector('input[type="number"][name$=".quantity"]');
    if (!qtyInput) return true;

    const td = qtyInput.closest("td") || qtyInput.parentElement;
    const stock = getRowStock(row);
    const raw = parseFiniteInt(qtyInput.value || "0");
    let qty = Number.isFinite(raw) ? raw : 0;

    // 0以下は1へクランプ
    if (qty < 1) qty = 1;

    if (!Number.isFinite(stock)) {
      // 在庫不明はチェック対象外（ブロックしない）
      clearWarn(td);
      qtyInput.value = String(qty);
      qtyInput.classList.remove("over-stock");
      qtyInput.style.outline = "";
      qtyInput.style.background = "";
      qtyInput.removeAttribute("aria-invalid");
      return true;
    }

    if (qty > stock) {
      // 即クランプ（ブロック）
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
    const ok = rows.every(r => validateRow(r));
    setSubmitEnabled(ok);
    return ok;
  }

  // 既存の重複行を統合（初期1回）
  (function normalizeDuplicatesOnce() {
    if (!selectedTbody) return;
    const seen = new Map(); // productId -> {row, qtyInput}
    const rows = Array.from(selectedTbody.querySelectorAll("tr"));

    // data-id 補完（hiddenから）
    rows.forEach(row => {
      if (!row.dataset.id) {
        const hid = row.querySelector('input[type="hidden"][name$=".id"]');
        if (hid?.value) row.dataset.id = String(hid.value);
      }
    });

    rows.forEach(row => {
      const pid = row.dataset.id;
      const qtyInput = row.querySelector("input[type='number'][name$='.quantity']");
      const qty = qtyInput ? parseInt(qtyInput.value || "0", 10) : 0;
      if (!seen.has(pid)) {
        seen.set(pid, { row, qtyInput });
      } else {
        const first = seen.get(pid);
        const baseQty = parseInt(first.qtyInput.value || "0", 10);
        first.qtyInput.value = String(baseQty + qty);
        row.remove();
      }
    });
    reindexSelectedProducts();
    validateAll();
  })();

  // 数量入力時：即検証
  selectedTbody?.addEventListener("input", (e) => {
    if (e.target.matches('input[type="number"][name$=".quantity"]')) {
      validateRow(e.target.closest("tr"));
      validateAll();
      isDirty = true;
    }
  });

  // ========== 製品検索 ==========
  const searchBtn = document.getElementById("searchProductBtn");
  if (searchBtn) {
    searchBtn.addEventListener("click", () => {
      // 完了 or GENERAL なら動かさない（UIは非表示だが二重防御）
      if (state.isCompleted || role.isGeneral) return;

      const keyword    = document.getElementById("searchKeyword").value;
      const categoryId = document.getElementById("searchCategory").value;
      const locationId = document.getElementById("searchLocation").value;

      const url = `/product/api/search?name=${encodeURIComponent(keyword)}&categoryIds=${encodeURIComponent(categoryId)}&locationIds=${encodeURIComponent(locationId)}`;

      fetch(url)
        .then(res => { if (!res.ok) throw new Error("検索APIエラー"); return res.json(); })
        .then(data => {
          const tbody = document.querySelector("#searchResultsTable tbody");
          if (!tbody) return;
          tbody.innerHTML = "";

          data.forEach(p => {
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

  // ========== 追加・削除 ==========
  document.addEventListener("click", (e) => {
    const lockedForProducts = role.isGeneral || state.isCompleted;

    // 追加
    if (e.target.classList && e.target.classList.contains("add-btn")) {
      if (lockedForProducts) return;
      const productId   = e.target.dataset.id;
      const productCode = e.target.dataset.code || "";
      const productName = e.target.dataset.name || "";
      const catsText    = e.target.dataset.cats || "-";
      const locsText    = e.target.dataset.locs || "-";
      const stock       = parseFiniteInt(e.target.dataset.stock) || 0;

      const existing = findRowByProductId(productId);
      if (existing) {
        const qtyInput = existing.querySelector("input[type='number'][name$='.quantity']");
        const cur = parseFiniteInt(qtyInput?.value) || 0;
        const next = cur + 1;

        // 出庫時は在庫超過をブロック
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

      // 新規行
      if (isOutMode && stock <= 0) {
        alert("在庫が 0 のため追加できません。");
        return;
      }

      const index = selectedTbody.querySelectorAll("tr").length;
      const tr = document.createElement("tr");
      tr.dataset.id = String(productId);
      if (isOutMode) tr.dataset.stock = String(stock); // 出庫時のみチェックに使用

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
        <td><input type="number" name="productWithDetailsDtos[${index}].quantity" value="1" min="1"></td>
        <td><button type="button" class="remove-btn">削除</button></td>
      `;
      selectedTbody.appendChild(tr);
      reindexSelectedProducts();
      validateRow(tr);   // 追加直後に検証（クランプされる場合あり）
      validateAll();     // 送信可否を同期
      isDirty = true;
      applyLockState();  // 既存ロック処理も反映（数量readonly等）
    }

    // 削除
    if (e.target.classList && e.target.classList.contains("remove-btn")) {
      if (lockedForProducts) return;
      e.target.closest("tr").remove();
      reindexSelectedProducts();
      validateAll();
      isDirty = true;
    }
  });

  // ========== 企業コード ⇄ 企業名 補完 ==========
  const partyCodeInput = document.getElementById("partyCode");
  const partyNameInput = document.getElementById("partyName");
  if (partyCodeInput && partyNameInput && typeof partyList !== "undefined") {
    partyCodeInput.addEventListener("input", function () {
      const code  = this.value.trim();
      const match = partyList.find(p => p.partyCode === code);
      partyNameInput.value = match ? match.partyName : "";
    });
    partyNameInput.addEventListener("input", function () {
      const name  = this.value.trim();
      const match = partyList.find(p => p.partyName === name);
      partyCodeInput.value = match ? match.partyCode : "";
    });
  }

  // =========================================================
  // 注文画像モーダル（一覧／複数アップロード／削除）+ 件数でボタン色
  // =========================================================
  const ORDER_ID = (() => {
    const el = form.querySelector('input[name="id"]');
    const v = el ? el.value : "";
    return v && String(v).trim().length > 0 ? v : null;
  })();

  const openOrderImageBtn = document.getElementById("openOrderImageModal");
  const orderImageModal   = document.getElementById("orderImageModal");
  const opFileInput = orderImageModal?.querySelector("#op-file");
  const opUploadBtn = orderImageModal?.querySelector("#op-upload");
  const opListBody  = orderImageModal?.querySelector("#op-list");
  const modalCloseEls = orderImageModal ? orderImageModal.querySelectorAll(".image-modal__close, .image-modal__backdrop") : [];

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

  async function syncOrderImageButton() {
    if (!ORDER_ID || !openOrderImageBtn) return [];
    try {
      const res = await fetch(`/order/api/${encodeURIComponent(ORDER_ID)}/pictures`, { cache: "no-store" });
      if (!res.ok) throw new Error("fetch failed");
      const list = await res.json();
      updateOrderImageButton(Array.isArray(list) ? list.length : 0);
      return Array.isArray(list) ? list : [];
    } catch {
      updateOrderImageButton(0);
      return [];
    }
  }

  function openModal() {
    if (!orderImageModal) return;
    if (state.isCompleted || role.isGeneral) return; // 二重防御
    orderImageModal.classList.add("is-open");
    if (opFileInput) opFileInput.value = "";
    loadOrderPictures().then(list => updateOrderImageButton(list.length));
  }
  function closeModal() {
    if (!orderImageModal) return;
    orderImageModal.classList.remove("is-open");
    if (opListBody) opListBody.innerHTML = "";
    if (opFileInput) opFileInput.value = "";
  }

  async function loadOrderPictures() {
    if (!opListBody) return [];
    opListBody.innerHTML = '<tr><td colspan="3">読み込み中...</td></tr>';
    try {
      const res = await fetch(`/order/api/${encodeURIComponent(ORDER_ID)}/pictures`, { cache: "no-store" });
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
            <td><img src="${url}" alt="${fn}"></td>
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

  async function uploadOrderPictures() {
    if (!opFileInput || !opUploadBtn) return;
    if (state.isCompleted || role.isGeneral) return;

    const files = Array.from(opFileInput.files || []);
    if (files.length === 0) return;

    const fd = new FormData();
    files.forEach(f => fd.append("files", f));

    opUploadBtn.disabled = true;
    opUploadBtn.textContent = "アップロード中...";
    try {
      const res = await fetch(`/order/api/${encodeURIComponent(ORDER_ID)}/pictures`, {
        method: "POST",
        body: fd,
        headers: csrfHeaders()
      });
      if (!res.ok) {
        const text = await res.text().catch(()=>"");
        throw new Error(`upload failed: ${res.status} ${res.statusText} ${text}`);
      }
      const list = await loadOrderPictures();
      updateOrderImageButton(list.length);
      opFileInput.value = "";
    } catch (e) {
      console.error(e);
      alert(`アップロードに失敗しました\n${e.message}`);
    } finally {
      opUploadBtn.disabled = false;
      opUploadBtn.textContent = "アップロード";
    }
  }

  async function deleteOrderPicture(pictureId) {
    if (state.isCompleted || role.isGeneral) return;
    if (!confirm("この画像を削除しますか？")) return;
    try {
      const res = await fetch(
        `/order/api/${encodeURIComponent(ORDER_ID)}/pictures/${encodeURIComponent(pictureId)}`,
        { method: "DELETE", headers: csrfHeaders() }
      );
      if (!res.ok) throw new Error("delete failed");
      const list = await loadOrderPictures();
      updateOrderImageButton(list.length);
    } catch (e) {
      console.error(e);
      alert("削除に失敗しました");
    }
  }

  if (openOrderImageBtn && !ORDER_ID) {
    openOrderImageBtn.disabled = true;
    openOrderImageBtn.title = "注文が未保存のため画像を扱えません";
  }
  if (openOrderImageBtn) openOrderImageBtn.addEventListener("click", openModal);
  modalCloseEls.forEach(el => el.addEventListener("click", closeModal));
  if (opUploadBtn) opUploadBtn.addEventListener("click", uploadOrderPictures);
  if (opListBody) {
    opListBody.addEventListener("click", (ev) => {
      const btn = ev.target.closest(".op-del");
      if (!btn) return;
      const pid = btn.getAttribute("data-id");
      if (pid) deleteOrderPicture(pid);
    });
  }
  document.addEventListener("keydown", (e) => {
    if (e.key === "Escape" && orderImageModal?.classList.contains("is-open")) closeModal();
  });
  syncOrderImageButton();

  // =========================================================
  // 完了/GENERAL ロック & disabled ミラー(hidden)で送信値を保証
  // =========================================================
  const els = {
    partyCode:   form.querySelector('#partyCode'),
    partyName:   form.querySelector('#partyName'),
    tracking:    form.querySelector('[name="trackingNumber"]'),
    deliveryDate:form.querySelector('[name="deliveryDate"]'),
    adminId:     form.querySelector('[name="adminId"]'),
    locationId:  form.querySelector('[name="locationId"]'),
    adminNote:   form.querySelector('[name="adminNote"]'),
    qNote:       form.querySelector('[name="qualityInspectorNote"]'),
    wNote:       form.querySelector('[name="warehouseWorkerNote"]'),
    worker:      form.querySelector('[name="warehouseWorkerId"]'),
    inspector:   form.querySelector('[name="qualityInspectorId"]'),
    situation:   form.querySelector('[name="situation"]'),
    productQtyInputs: () => form.querySelectorAll('input[type="number"][name$=".quantity"]'),
    removeButtons:    () => form.querySelectorAll('.remove-btn'),
  };

  function setDisabled(el, disabled) { if (el) el.disabled = !!disabled; }
  function setReadonly(el, ro) { if (el) el.readOnly = !!ro; }

  // disabled要素の代替として、同名のhiddenを生成/更新する
  function syncHiddenMirror(name, control) {
    if (!control || !name) return;
    let hid = form.querySelector(`input[type="hidden"][data-mirror="${name}"]`);
    if (control.disabled) {
      if (!hid) {
        hid = document.createElement('input');
        hid.type = 'hidden';
        hid.name = control.name;               // 同名でサーバに送る
        hid.setAttribute('data-mirror', name); // 管理用
        form.appendChild(hid);
      }
      hid.value = control.value ?? '';
    } else {
      if (hid) hid.remove();
    }
  }
  function syncAllMirrors() {
    syncHiddenMirror('partyCode',          els.partyCode);
    syncHiddenMirror('partyName',          els.partyName);
    syncHiddenMirror('trackingNumber',     els.tracking);
    syncHiddenMirror('deliveryDate',       els.deliveryDate);
    syncHiddenMirror('adminId',            els.adminId);
    syncHiddenMirror('locationId',         els.locationId);
    syncHiddenMirror('warehouseWorkerId',  els.worker);
    syncHiddenMirror('qualityInspectorId', els.inspector);
    // ※ textarea は readonly 運用なのでミラー不要
  }

  function applyLockState() {
    const completed = state.isCompleted;

    // 下段を隠す/出す
    if (screenFit) {
      if (role.isGeneral || completed) screenFit.classList.add('no-bottom');
      else screenFit.classList.remove('no-bottom');
    }

    // 状況は常に編集可
    setDisabled(els.situation, false);

    if (role.isGeneral) {
      // GENERAL: 作業者/検品者/それぞれのメモ/状況のみ編集可
      setDisabled(els.partyCode, true);
      setDisabled(els.partyName, true);
      setDisabled(els.tracking,  true);
      setDisabled(els.deliveryDate, true);
      setDisabled(els.adminId,   true);
      setDisabled(els.locationId,true);
      setReadonly(els.adminNote, true);

      setDisabled(els.worker,    completed);
      setDisabled(els.inspector, completed);
      setReadonly(els.qNote,     completed);
      setReadonly(els.wNote,     completed);

      els.productQtyInputs().forEach(i => { i.readOnly = true; i.classList.add('is-readonly'); });
      els.removeButtons().forEach(b => b.style.display = 'none');

      if (openOrderImageBtn) openOrderImageBtn.disabled = true;

    } else {
      // ADMIN
      if (completed) {
        // 完了時：状況以外は編集不可（配達日も不可）
        setDisabled(els.partyCode, true);
        setDisabled(els.partyName, true);
        setDisabled(els.tracking,  true);
        setDisabled(els.deliveryDate, true);
        setDisabled(els.adminId,   true);
        setDisabled(els.locationId,true);

        setReadonly(els.adminNote, true);
        setDisabled(els.worker,    true);
        setDisabled(els.inspector, true);
        setReadonly(els.qNote,     true);
        setReadonly(els.wNote,     true);

        els.productQtyInputs().forEach(i => { i.readOnly = true; i.classList.add('is-readonly'); });
        els.removeButtons().forEach(b => b.style.display = 'none');

        if (openOrderImageBtn) openOrderImageBtn.disabled = true;

      } else {
        // ADMIN & 未完了：全て編集可（配達日も可）
        [els.partyCode, els.partyName, els.tracking, els.deliveryDate, els.adminId, els.locationId]
          .forEach(el => setDisabled(el, false));
        setReadonly(els.adminNote, false);
        setDisabled(els.worker, false);
        setDisabled(els.inspector, false);
        setReadonly(els.qNote, false);
        setReadonly(els.wNote, false);

        els.productQtyInputs().forEach(i => { i.readOnly = false; i.classList.remove('is-readonly'); });
        els.removeButtons().forEach(b => b.style.display = '');

        if (openOrderImageBtn && ORDER_ID) openOrderImageBtn.disabled = false;
      }
    }

    // disabled にしたコントロールは必ず hidden ミラーに値を入れる
    syncAllMirrors();

    // ロック状態変更後にも在庫検証を一応走らせておく
    validateAll();
  }

  // 初期適用
  applyLockState();

  // 状況変更で動的適用
  if (els.situation) {
    els.situation.addEventListener('change', () => {
      applyLockState();
    });
  }

  // 送信直前に最終ミラー同期 + 在庫最終チェック（ブロック）
  form.addEventListener('submit', (e) => {
    syncAllMirrors();
    if (!validateAll()) {
      e.preventDefault();
      alert("在庫数を超えている行があります。数量を修正してください。");
      return;
    }
    // beforeunload 抑止
    isDirty = false;
  });
});
