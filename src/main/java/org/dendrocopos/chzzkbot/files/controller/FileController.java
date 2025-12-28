package org.dendrocopos.chzzkbot.files.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.dendrocopos.chzzkbot.files.DTO.FileItem;
import org.dendrocopos.chzzkbot.files.config.FileStorageProperties;
import org.dendrocopos.chzzkbot.files.services.FileService;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.ResourceRegion;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;
import java.util.List;

@Controller
@RequiredArgsConstructor
@RequestMapping("/files")
public class FileController {

    private final FileService fileService;
    private final FileStorageProperties fileStorageProperties;

    @GetMapping("/watch")
    public String watch(@RequestParam("path") String path, Model model) {
        String decodedPath = URLDecoder.decode(path, StandardCharsets.UTF_8);
        model.addAttribute("path", decodedPath);
        return "common/video-view"; // templates/video-view.html
    }

    /**
     * 파일 탐색기 화면 렌더링
     */
    @GetMapping("/view")
    public String viewFiles(@RequestParam(required = false) String path, Model model) {
        model.addAttribute("currentPage", "vod");
        String logicalPath = (path != null && !path.isBlank())
                ? URLDecoder.decode(path, StandardCharsets.UTF_8)
                : null;

        List<FileItem> files;
        String parentPathEncoded;
        String displayPath;
        boolean atRoot;

        if (logicalPath == null) {
            atRoot = true;

            List<String> rootNames = fileStorageProperties.getDisplayName();
            files = rootNames.stream()
                    .map(name -> FileItem.builder()
                            .name(name)
                            .fullPath(name)      // 논리 경로
                            .directory(true)
                            .size(0L)
                            .humanSize("-")
                            .build())
                    .toList();

            parentPathEncoded = null;
            displayPath = "root";
        } else {
            atRoot = false;

            Path physicalPath = fileService.resolvePath(logicalPath);
            files = fileService.listFiles(physicalPath);

            Path logical = Paths.get(logicalPath);
            Path parentLogical = logical.getParent();
            parentPathEncoded = (parentLogical == null)
                    ? null
                    : URLEncoder.encode(parentLogical.toString(), StandardCharsets.UTF_8);

            displayPath = "root/" + logicalPath.replace("\\", "/");
        }

        model.addAttribute("currentPath", displayPath);
        model.addAttribute("parentPath", parentPathEncoded);
        model.addAttribute("files", files);
        model.addAttribute("atRoot", atRoot);   // ★ 추가


        return "common/files";
    }

    /**
     * 개별 파일 다운로드
     */
    @GetMapping("/download")
    @ResponseBody
    public ResponseEntity<InputStreamResource> downloadFile(@RequestParam("path") String filePath,
                                                            HttpServletRequest request) {
        String clientIp = extractClientIp(request);
        return fileService.downloadFile(filePath, clientIp);
    }


    @GetMapping("/stream")
    @ResponseBody
    public ResponseEntity<StreamingResponseBody> stream(
            @RequestParam("path") String path,
            @RequestHeader HttpHeaders headers,
            HttpServletRequest request
    ) throws IOException {

        String decodedPath = URLDecoder.decode(path, StandardCharsets.UTF_8);
        String clientIp = extractClientIp(request);

        return fileService.streamFile(decodedPath, headers, clientIp);
    }


    /**
     * 여러 파일을 압축하여 다운로드
     */
    @PostMapping("/zip")
    public void downloadAsZip(@RequestBody List<String> encodedPaths,
                              HttpServletRequest request,
                              HttpServletResponse response) throws IOException {
        String clientIp = extractClientIp(request);
        fileService.downloadAsZip(encodedPaths, clientIp, response);
    }


    private String extractClientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0];
        }
        return request.getRemoteAddr();
    }

}
