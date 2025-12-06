package org.dendrocopos.chzzkbot.core.utils;

import org.springframework.util.FileCopyUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class ZipUtil {

    /**
     * 주어진 파일 리스트를 ZIP 형식으로 출력 스트림에 기록한다.
     * @param files 압축할 파일들의 리스트
     * @param os ZIP 출력 대상이 될 OutputStream (응답 출력 스트림)
     */
    public static void zipFiles(List<File> files, OutputStream os) throws IOException {
        try (ZipOutputStream zipOut = new ZipOutputStream(os)) {
            byte[] buffer = new byte[8192];
            for (File file : files) {
                // 각 파일에 대한 ZIP 엔트리 생성
                ZipEntry entry = new ZipEntry(file.getName());
                zipOut.putNextEntry(entry);
                // 파일 내용을 읽어 ZipOutputStream에 씀
                try (FileInputStream fis = new FileInputStream(file)) {
                    // 스프링 유틸리티를 사용하거나 수동으로 복사
                    FileCopyUtils.copy(fis, zipOut);
                    // 또는 아래와 같이 수동으로 버퍼링할 수도 있음:
                    // int len;
                    // while ((len = fis.read(buffer)) != -1) {
                    //     zipOut.write(buffer, 0, len);
                    // }
                }
                zipOut.closeEntry();
            }
            // ZipOutputStream을 닫으면 os(출력스트림)는 닫지 않는다 (StreamingResponseBody는 계속 사용)
            // try-with-resources에 의해 zipOut이 flush되고 close될 것임.
        }
    }
}
