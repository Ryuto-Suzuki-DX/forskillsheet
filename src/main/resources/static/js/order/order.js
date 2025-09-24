// /static/js/order/order.js
document.addEventListener("DOMContentLoaded", () => {
    // === 要素取得 ===
    const editForm = document.getElementById("editForm");
    const deleteForm = document.getElementById("deleteForm");
    const editBtn = document.getElementById("editBtn");
    const deleteBtn = document.getElementById("deleteBtn");

    const editOrderIdInput = document.getElementById("editOrderId");
    const deleteOrderIdInput = document.getElementById("deleteOrderId");

    const rows = document.querySelectorAll(".order-table tbody tr[data-id]");

    // 選択中行取得
    const getSelectedRow = () => document.querySelector(".order-table tbody tr.selected");

    // 選択処理
    const setSelection = (row) => {
        document.querySelectorAll(".order-table tbody tr.selected").forEach(tr => {
            tr.classList.remove("selected");
        });

        row.classList.add("selected");
        const selectedId = row.getAttribute("data-id");

        if (editOrderIdInput) editOrderIdInput.value = selectedId;
        if (deleteOrderIdInput) deleteOrderIdInput.value = selectedId;

        if (editBtn) editBtn.disabled = false;
        if (deleteBtn) deleteBtn.disabled = false;
    };

    // === 行クリックイベント ===
    rows.forEach(row => {
        row.addEventListener("click", () => setSelection(row));

        row.addEventListener("dblclick", () => {
            setSelection(row);
            if (editForm) editForm.submit();
        });
    });

    // === 編集ボタン動作 ===
    if (editBtn && editForm) {
        editBtn.addEventListener("click", (e) => {
            if (!editOrderIdInput?.value) {
                e.preventDefault();
                alert("編集する入出庫を選択してください。");
            }
        });
    }

    // === 削除ボタン動作 ===
    if (deleteBtn && deleteForm) {
        deleteBtn.addEventListener("click", (e) => {
            const selectedRow = getSelectedRow();
            const selectedId = deleteOrderIdInput?.value;

            if (!selectedRow || !selectedId) {
                e.preventDefault();
                alert("削除する入出庫を選択してください。");
                return;
            }

            const code = selectedRow.cells[0]?.textContent?.trim() || "";
            const party = selectedRow.cells[2]?.textContent?.trim() || "";

            const ok = confirm(`入出庫コード ${code} / 企業名 ${party} を削除しますか？（ID: ${selectedId}）`);
            if (!ok) e.preventDefault();
        });
    }

    // 初期状態は無効化
    if (editBtn) editBtn.disabled = true;
    if (deleteBtn) deleteBtn.disabled = true;
});
