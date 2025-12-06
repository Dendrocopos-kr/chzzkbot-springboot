package org.dendrocopos.chzzkbot.files.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "app.file")
public class FileStorageProperties {
    private List<String> basePath;
    private List<String> displayName;   // ★ 추가

    public List<Path> getBasePathAsPaths() {
        return basePath.stream()
                .map(Path::of)
                .collect(Collectors.toList());
    }

    public List<String> getDisplayName() {
        return displayName;
    }
}
