document.addEventListener("turbo:load", () => {
    const icons = document.querySelectorAll(".file-type-icon-inner");
    if (!icons.length) return;

    const iconMap = {
        // 특수 타입
        folder: "fa-folder",

        // 이미지
        png: "fa-file-image",
        jpg: "fa-file-image",
        jpeg: "fa-file-image",
        gif: "fa-file-image",
        webp: "fa-file-image",
        svg: "fa-file-image",

        // 동영상 / 오디오
        mp4: "fa-file-video",
        mkv: "fa-file-video",
        webm: "fa-file-video",
        mp3: "fa-file-audio",
        flac: "fa-file-audio",

        // 문서류
        pdf: "fa-file-pdf",
        doc: "fa-file-word",
        docx: "fa-file-word",
        xls: "fa-file-excel",
        xlsx: "fa-file-excel",
        ppt: "fa-file-powerpoint",
        pptx: "fa-file-powerpoint",
        txt: "fa-file-lines",
        md: "fa-file-lines",

        // 압축
        zip: "fa-file-zipper",
        rar: "fa-file-zipper",
        "7z": "fa-file-zipper",

        // 기본값
        default: "fa-file-alt"
    };

    icons.forEach(icon => {
        // 폴더 우선 처리
        if (icon.dataset.kind === "folder") {
            icon.className = "file-type-icon-inner fas " + iconMap.folder;
            return;
        }

        const filename = icon.dataset.filename || "";
        let ext = "";

        const lastDot = filename.lastIndexOf(".");
        if (lastDot !== -1 && lastDot < filename.length - 1) {
            ext = filename.substring(lastDot + 1).toLowerCase();
        }

        const faClass = iconMap[ext] || iconMap.default;
        icon.className = "file-type-icon-inner fas " + faClass;
    });
});