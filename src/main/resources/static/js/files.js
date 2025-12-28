function initFilesPage() {
    // ---------- 아이콘 처리 ----------
    const icons = document.querySelectorAll(".file-type-icon-inner");
    const iconMap = {
        folder: "fa-folder",
        png: "fa-file-image", jpg: "fa-file-image", jpeg: "fa-file-image",
        gif: "fa-file-image", webp: "fa-file-image", svg: "fa-file-image",
        mp4: "fa-file-video", mkv: "fa-file-video", webm: "fa-file-video",
        mp3: "fa-file-audio", flac: "fa-file-audio",
        pdf: "fa-file-pdf", doc: "fa-file-word", docx: "fa-file-word",
        xls: "fa-file-excel", xlsx: "fa-file-excel",
        ppt: "fa-file-powerpoint", pptx: "fa-file-powerpoint",
        txt: "fa-file-lines", md: "fa-file-lines",
        zip: "fa-file-zipper", rar: "fa-file-zipper", "7z": "fa-file-zipper",
        default: "fa-file-alt"
    };

    icons.forEach(icon => {
        const filename = icon.dataset.filename || "";
        const lastDot = filename.lastIndexOf(".");
        const ext = (lastDot !== -1 && lastDot < filename.length - 1)
            ? filename.substring(lastDot + 1).toLowerCase()
            : "";
        const faClass = iconMap[ext] || iconMap.default;
        icon.className = "file-type-icon-inner fas " + faClass;
    });

    const table = document.querySelector(".file-table");
    if (!table) return;

    const tbody = table.querySelector("tbody");
    const headers = table.querySelectorAll("th[data-sort]");
    let currentSort = { key: null, dir: "asc" };

    // ---------- 정렬 ----------
    headers.forEach(th => {
        th.addEventListener("click", () => {
            const key = th.dataset.sort;
            const dir =
                currentSort.key === key && currentSort.dir === "asc"
                    ? "desc"
                    : "asc";
            currentSort = { key, dir };

            headers.forEach(h => h.classList.remove("asc", "desc"));
            th.classList.add(dir);

            // preview row 제거 (정렬 전 정리)
            tbody.querySelectorAll("tr.filename-preview").forEach(r => r.remove());
            tbody.querySelectorAll("tr.active").forEach(r => r.classList.remove("active"));

            const rows = Array.from(tbody.querySelectorAll("tr"))
                .filter(tr => tr.dataset[key] !== undefined && !tr.classList.contains("section-row") && !tr.classList.contains("empty-row"));

            rows.sort((a, b) => {
                let va = a.dataset[key];
                let vb = b.dataset[key];

                if (!isNaN(va) && !isNaN(vb)) { // number
                    va = Number(va);
                    vb = Number(vb);
                } else {
                    va = (va ?? "").toString().toLowerCase();
                    vb = (vb ?? "").toString().toLowerCase();
                }

                if (va < vb) return dir === "asc" ? -1 : 1;
                if (va > vb) return dir === "asc" ? 1 : -1;
                return 0;
            });

            rows.forEach(tr => tbody.appendChild(tr));
        });
    });

    // ---------- 모바일: 선택(active) 1개 + 파일명 풀뷰 ----------
    table.addEventListener("click", (e) => {
        if (window.innerWidth > 768) return;

        const row = e.target.closest("tbody tr");
        if (!row) return;
        if (row.classList.contains("section-row") || row.classList.contains("empty-row") || row.classList.contains("filename-preview")) return;

        // 버튼/링크 클릭은 선택 토글로 취급하지 않음 (다운로드/재생)
        if (e.target.closest("a")) return;

        const currentActive = tbody.querySelector("tr.active");
        const currentPreview = tbody.querySelector("tr.filename-preview");

        // 같은 row를 다시 누르면 닫기
        if (currentActive === row) {
            row.classList.remove("active");
            if (currentPreview) currentPreview.remove();
            return;
        }

        // 기존 active/preview 제거
        if (currentActive) currentActive.classList.remove("active");
        if (currentPreview) currentPreview.remove();

        // 새 active 설정
        row.classList.add("active");

        const fullName = row.dataset.fullname || row.dataset.name || "";
        const preview = document.createElement("tr");
        preview.className = "filename-preview";

        const td = document.createElement("td");
        td.colSpan = 5; // 컬럼 수(종류/이름/크기/수정일/다운로드) 기준. 크기 숨겨도 colSpan은 그대로 두는 게 안전
        td.innerHTML = `<div class="filename-preview-box">${escapeHtml(fullName)}</div>`;

        preview.appendChild(td);

        // row 바로 아래에 삽입
        row.insertAdjacentElement("afterend", preview);
    });

    function escapeHtml(str) {
        return (str ?? "").replace(/[&<>"']/g, (m) => ({
            "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;", "'": "&#39;"
        }[m]));
    }
}

// Turbo/일반 모두
document.addEventListener("DOMContentLoaded", initFilesPage);
document.addEventListener("turbo:load", initFilesPage);
