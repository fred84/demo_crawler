package crawler;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Service
public class StorageService {

    private final Path downloadDir;

    @Autowired
    StorageService(@Value("${app.download.dir}") String dir) {
        this(Paths.get(dir));
    }

    StorageService(Path dir) {
        assert Files.isDirectory(dir) : "path should be directory";
        assert Files.isWritable(dir) : "path should be writable";

        downloadDir = dir;
    }

    void store(Page page, byte[] body) {
        assert page != null : "attempt is mandatory param";
        assert body.length > 0 : "file content should not be empty";

        try {
            // .toString() call is workaround for providermismatch exception. Probably there is better solution
            Path filePath = downloadDir.resolve(page.getRelativePath().toString());
            Files.createDirectories(filePath.getParent());

            // this is not atomic but we believe this happens only after application reload
            if (!Files.exists(filePath)) {
                Files.createFile(filePath);
            }

            Files.write(filePath, body);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
