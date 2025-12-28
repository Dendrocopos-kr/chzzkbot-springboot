package org.dendrocopos.chzzkbot.files.services;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.apache.tika.Tika;
import org.dendrocopos.chzzkbot.files.DTO.FileItem;
import org.dendrocopos.chzzkbot.files.Entity.DownloadLog;
import org.dendrocopos.chzzkbot.files.config.FileStorageProperties;
import org.dendrocopos.chzzkbot.files.enums.DownloadType;
import org.dendrocopos.chzzkbot.files.repository.DownloadLogRepository;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.PathResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.ResourceRegion;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.io.*;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Service
@RequiredArgsConstructor
public class FileService {

    private final DownloadLogRepository logRepository;

    private final FileStorageProperties fileStorageProperties;

    private final Tika tika = new Tika();

    /**
     * 디렉토리 내 파일/폴더 목록을 반환
     */
    public List<FileItem> listDirectory(String encodedPath) {
        try {
            Path target = decodeAndResolve(encodedPath);

            if (!Files.exists(target) || !Files.isDirectory(target)) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Directory not found");
            }

            List<FileItem> results = new ArrayList<>();

            try (DirectoryStream<Path> stream = Files.newDirectoryStream(target)) {
                for (Path path : stream) {
                    boolean isDir = Files.isDirectory(path);
                    results.add(FileItem.builder()
                            .name(path.getFileName().toString())
                            .fullPath(toLogicalPath(path))       // ★ 변경
                            .parentPath(toLogicalPath(target))   // 필요하면 가상 부모 경로도
                            .directory(isDir)
                            .size(isDir ? 0 : Files.size(path))
                            .humanSize(humanReadableSize(Files.size(path)))   // ★ 여기 추가
                            .lastModified(Files.getLastModifiedTime(path).toInstant())
                            .mimeType(isDir ? null : tika.detect(path))
                            .downloadable(!isDir)
                            .build());
                }
            }

            // 폴더 우선, 이름순 정렬
            results.sort(Comparator
                    .comparing(FileItem::isDirectory).reversed()
                    .thenComparing(FileItem::getName, String.CASE_INSENSITIVE_ORDER));

            return results;

        } catch (IOException e) {
            throw new RuntimeException("Failed to list directory", e);
        }
    }

    /**
     * 파일 다운로드 및 로그 저장
     */
    public ResponseEntity<InputStreamResource> downloadFile(String encodedPath, String clientIp) {
        Path path = decodeAndResolve(encodedPath);

        if (!Files.exists(path) || Files.isDirectory(path)) {
            saveLog(path, clientIp, false,DownloadType.DOWNLOAD);
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "File not found");
        }

        try {
            FileItem fileItem = toFileItem(path);
            InputStream inputStream = Files.newInputStream(path);
            InputStreamResource resource = new InputStreamResource(inputStream);

            String filename = path.getFileName().toString();

            ContentDisposition disposition = ContentDisposition.attachment()
                    .filename(filename, StandardCharsets.UTF_8)
                    .build();

            HttpHeaders headers = new HttpHeaders();
            headers.setContentDisposition(disposition);
            headers.setContentType(
                    fileItem.getMimeType() != null
                            ? MediaType.parseMediaType(fileItem.getMimeType())
                            : MediaType.APPLICATION_OCTET_STREAM
            );

            saveLog(path, clientIp, true,DownloadType.DOWNLOAD);

            return ResponseEntity.ok()
                    .headers(headers)
                    .contentLength(fileItem.getSize())
                    .body(resource);

        } catch (IOException e) {
            saveLog(path, clientIp, false,DownloadType.DOWNLOAD);
            throw new RuntimeException("File download error", e);
        }
    }

    /**
     * 다운로드 로그 저장
     */
    private void saveLog(Path path, String ip, boolean success, DownloadType type) {
        try {
            FileItem item = null;
            if (Files.exists(path) && !Files.isDirectory(path)) {
                item = toFileItem(path);
            }

            DownloadLog log = DownloadLog.builder()
                    .fileName(item != null ? item.getName() : path.getFileName().toString())
                    .fullPath(path.toAbsolutePath().toString())
                    .size(item != null ? item.getSize() : 0L)
                    .mimeType(item != null ? item.getMimeType() : null)
                    .lastModified(item != null ? item.getLastModified() : null)
                    .clientIp(ip)
                    .downloadedAt(Instant.now())
                    .success(success)
                    .type(type)
                    .build();

            logRepository.save(log);
        } catch (IOException e) {
            // 여기서 로그 남기기 (logger.warn 등)
        }
    }

    /**
     * 파일 Path → FileItem 변환
     */
    private FileItem toFileItem(Path path) throws IOException {
        return FileItem.builder()
                .name(path.getFileName().toString())
                .fullPath(toLogicalPath(path))       // ★ 변경
                .parentPath(toLogicalPath(path.getParent()))   // 필요하면 가상 부모 경로도
                .parentPath(path.getParent().toAbsolutePath().toString())
                .directory(false)
                .size(Files.size(path))
                .humanSize(humanReadableSize(Files.size(path)))   // ★ 여기 추가
                .lastModified(Files.getLastModifiedTime(path).toInstant())
                .mimeType(tika.detect(path))
                .downloadable(true)
                .build();
    }

    /**w
     * 인코딩된 경로 → 실제 Path 변환 및 루트 경로 제한 검사
     */
    private Path decodeAndResolve(String encodedPath) {
        String decoded = URLDecoder.decode(encodedPath, StandardCharsets.UTF_8);
        return toPhysicalPath(decoded);   // ★ 가상 → 실제
    }

    public Path resolvePath(String requestedPath) {
        if (requestedPath == null || requestedPath.isBlank()) {
            // 루트는 컨트롤러에서 따로 처리 (편집영상/2024/2025 목록)
            return null;
        }
        return toPhysicalPath(requestedPath);
    }


    public List<FileItem> listFiles(Path directory) {
        if (!Files.isDirectory(directory)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "디렉토리가 아닙니다.");
        }

        try (Stream<Path> paths = Files.list(directory)) {
            return paths
                    .map(p -> {
                        try {
                            return FileItem.builder()
                                    .name(p.getFileName().toString())
                                    .fullPath(toLogicalPath(p))
                                    .directory(Files.isDirectory(p))
                                    .size(Files.isDirectory(p) ? 0L : Files.size(p))
                                    .humanSize(humanReadableSize(Files.size(p)))   // ★ 여기 추가
                                    .lastModified(Files.getLastModifiedTime(p).toInstant())
                                    .build();
                        } catch (IOException e) {
                            throw new UncheckedIOException(e);
                        }
                    })
                    .sorted(Comparator.comparing(FileItem::isDirectory).reversed()
                            .thenComparing(FileItem::getName, String.CASE_INSENSITIVE_ORDER))
                    .toList();
        } catch (IOException e) {
            throw new UncheckedIOException("디렉토리 목록을 불러올 수 없습니다.", e);
        }
    }

    public ResponseEntity<StreamingResponseBody> streamFile(String logicalPath, HttpHeaders requestHeaders, String clientIp) throws IOException {
        Path physicalPath = resolvePath(logicalPath);

        if (physicalPath == null || !Files.exists(physicalPath) || Files.isDirectory(physicalPath)) {
            saveLog(physicalPath != null ? physicalPath : Paths.get(""), clientIp, false, DownloadType.STREAM);
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "파일이 존재하지 않습니다.");
        }

        long fileSize = Files.size(physicalPath);

        // mimeType (probe 실패 시 mp4로 폴백)
        String mimeType = Files.probeContentType(physicalPath);
        MediaType mediaType = (mimeType != null) ? MediaType.parseMediaType(mimeType) : MediaType.valueOf("video/mp4");

        // Range 파싱
        String rangeHeader = requestHeaders.getFirst(HttpHeaders.RANGE);

        long start = 0;
        long end = fileSize - 1;

        if (rangeHeader != null && rangeHeader.startsWith("bytes=")) {
            // 예: bytes=0- , bytes=100-200
            String[] parts = rangeHeader.substring("bytes=".length()).split("-", 2);

            try {
                if (!parts[0].isBlank()) start = Long.parseLong(parts[0]);
                if (parts.length > 1 && !parts[1].isBlank()) end = Long.parseLong(parts[1]);
            } catch (NumberFormatException ignore) {
                // 잘못된 Range면 416
                HttpHeaders h = new HttpHeaders();
                h.set(HttpHeaders.CONTENT_RANGE, "bytes */" + fileSize);
                return ResponseEntity.status(HttpStatus.REQUESTED_RANGE_NOT_SATISFIABLE).headers(h).body(outputStream -> {});
            }

            // 범위 보정/검증
            if (start < 0 || start >= fileSize || end < start) {
                HttpHeaders h = new HttpHeaders();
                h.set(HttpHeaders.CONTENT_RANGE, "bytes */" + fileSize);
                return ResponseEntity.status(HttpStatus.REQUESTED_RANGE_NOT_SATISFIABLE).headers(h).body(outputStream -> {});
            }
            if (end >= fileSize) end = fileSize - 1;
        }

        long contentLength = end - start + 1;

        // STREAM 로그는 "첫 요청(=Range 없음)"일 때만 찍던 기존 정책 유지
        if (rangeHeader == null) {
            saveLog(physicalPath, clientIp, true, DownloadType.STREAM);
        }

        StreamingResponseBody body = getStreamingResponseBody(start, end, physicalPath);

        HttpHeaders respHeaders = new HttpHeaders();
        respHeaders.setContentType(mediaType);
        respHeaders.set(HttpHeaders.ACCEPT_RANGES, "bytes");
        respHeaders.setContentLength(contentLength);

        // Range 요청이면 206 + Content-Range
        if (rangeHeader != null) {
            respHeaders.set(HttpHeaders.CONTENT_RANGE, "bytes " + start + "-" + end + "/" + fileSize);
            return ResponseEntity.status(HttpStatus.PARTIAL_CONTENT).headers(respHeaders).body(body);
        }

        // Range 없으면 200
        return ResponseEntity.ok().headers(respHeaders).body(body);
    }

    private static StreamingResponseBody getStreamingResponseBody(long start, long end, Path physicalPath) {
        final long fStart = start;
        final long fEnd = end;

        StreamingResponseBody body = outputStream -> {
            try (RandomAccessFile raf = new RandomAccessFile(physicalPath.toFile(), "r")) {
                raf.seek(fStart);

                byte[] buffer = new byte[8192];
                long remaining = (fEnd - fStart) + 1;

                while (remaining > 0) {
                    int read = raf.read(buffer, 0, (int) Math.min(buffer.length, remaining));
                    if (read == -1) break;
                    outputStream.write(buffer, 0, read);
                    remaining -= read;
                }
            }
        };
        return body;
    }

    public void downloadAsZip(List<String> encodedPaths, String clientIp,
                              HttpServletResponse response) throws IOException {
        if (encodedPaths == null || encodedPaths.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "선택된 파일이 없습니다.");
        }

        List<Path> targets = encodedPaths.stream()
                .map(this::decodeAndResolve)   // base-path 체크 포함
                .toList();

        // ZIP 파일명은 공통 상위 경로 기준으로 정하거나, timestamp 기반으로
        String zipFileName = "download-" + System.currentTimeMillis() + ".zip";

        response.setStatus(HttpStatus.OK.value());
        response.setContentType("application/zip");
        response.setHeader(HttpHeaders.CONTENT_DISPOSITION,
                "attachment; filename=\"" + URLEncoder.encode(zipFileName, StandardCharsets.UTF_8) + "\"");

        try (ZipOutputStream zos = new ZipOutputStream(response.getOutputStream())) {
            for (Path target : targets) {
                if (Files.isDirectory(target)) {
                    Files.walk(target)
                            .filter(Files::isRegularFile)
                            .forEach(p -> putEntryAndLog(zos, p, clientIp, target));
                } else if (Files.isRegularFile(target)) {
                    putEntryAndLog(zos, target, clientIp, target.getParent());
                }
            }
        }
    }

    private void putEntryAndLog(ZipOutputStream zos, Path file,
                                String clientIp, Path base) {
        try {
            String entryName = base.relativize(file).toString().replace("\\", "/");
            zos.putNextEntry(new ZipEntry(entryName));
            Files.copy(file, zos);
            zos.closeEntry();

            saveLog(file, clientIp, true, DownloadType.ZIP);
        } catch (IOException e) {
            saveLog(file, clientIp, false, DownloadType.ZIP);
            throw new UncheckedIOException(e);
        }
    }

    private String humanReadableSize(long size) {
        if (size <= 0) return "-";

        String[] units = {"B", "KB", "MB", "GB", "TB"};
        int unitIndex = (int) (Math.log10(size) / Math.log10(1024));
        double readableSize = size / Math.pow(1024, unitIndex);

        return String.format("%.1f %s", readableSize, units[unitIndex]);
    }

    /**
     * 실제 물리 경로 → 화면/URL용 가상 경로 (편집영상/xxx, 2024/xxx ...)
     */
    private String toLogicalPath(Path physicalPath) {
        Path normalized = physicalPath.toAbsolutePath().normalize();

        List<Path> bases = fileStorageProperties.getBasePathAsPaths().stream()
                .map(p -> p.toAbsolutePath().normalize())
                .toList();

        List<String> names = fileStorageProperties.getDisplayName();

        for (int i = 0; i < bases.size(); i++) {
            Path base = bases.get(i);
            if (normalized.startsWith(base)) {
                Path rel = base.relativize(normalized);          // base 이하의 상대 경로
                String rootName = names.get(i);                  // 편집영상 / 2024 / 2025

                if (rel.getNameCount() == 0) {
                    return rootName;                             // 루트 디렉토리 자체
                } else {
                    return rootName + "/" + rel.toString().replace("\\", "/");
                }
            }
        }

        throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                "등록되지 않은 파일 경로입니다.");
    }

    /**
     * 가상 경로 (편집영상/xxx) → 실제 물리 경로
     */
    private Path toPhysicalPath(String logicalPath) {
        if (logicalPath == null || logicalPath.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "경로가 없습니다.");
        }

        Path logical = Paths.get(logicalPath).normalize();   // ex) 편집영상/aaa/bbb.mp4
        if (logical.getNameCount() == 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "잘못된 경로입니다.");
        }

        String rootName = logical.getName(0).toString();     // 편집영상 / 2024 / 2025

        List<String> names = fileStorageProperties.getDisplayName();
        List<Path> bases = fileStorageProperties.getBasePathAsPaths();

        int idx = -1;
        for (int i = 0; i < names.size(); i++) {
            if (names.get(i).equals(rootName)) {
                idx = i;
                break;
            }
        }

        if (idx < 0) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "존재하지 않는 루트입니다.");
        }

        Path base = bases.get(idx).toAbsolutePath().normalize();

        // 루트 바로 아래면 base 자체
        if (logical.getNameCount() == 1) {
            return base;
        }

        Path rel = logical.subpath(1, logical.getNameCount());    // 편집영상 / [여기부터 실제 경로]
        Path physical = base.resolve(rel).normalize();

        // ../ 같은 걸로 루트 밖으로 나가는 공격 방지
        if (!physical.startsWith(base)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "허용되지 않은 경로입니다.");
        }

        return physical;
    }

}