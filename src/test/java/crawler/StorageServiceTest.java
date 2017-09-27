package crawler;

import com.google.common.jimfs.Configuration;
import com.google.common.jimfs.Jimfs;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

public class StorageServiceTest {

    private final Page page = Page.initial(new CrawlTask("http://en.wikipedia.org/wiki/Bruce_Willis", 2, p -> {
    }));
    private final byte[] defaultBody = "body".getBytes();

    private FileSystem fs;
    private StorageService service;

    @Before
    public void setUp() throws IOException {
        fs = Jimfs.newFileSystem(
                Configuration.unix().toBuilder().setAttributeViews("posix", "unix").build()
        );

        Path dataDir = fs.getPath("/data").toAbsolutePath();
        Files.createDirectory(dataDir);
        service = new StorageService(dataDir);
    }

    @Test
    public void fileSuccessfullyCreated() throws IOException {
        service.store(page, defaultBody);

        assertThat(
                new String(Files.readAllBytes(fs.getPath("/data/en/b/r/bruce_willis.html")), StandardCharsets.UTF_8),
                equalTo("body")
        );
    }

    @Test
    public void fileOverwritten() throws IOException {
        Files.createDirectories(fs.getPath("/data/en/b/r/"));
        Files.createFile(fs.getPath("/data/en/b/r/bruce_willis.html"));
        Files.write(fs.getPath("/data/en/b/r/bruce_willis.html"), "previous content".getBytes());

        service.store(page, defaultBody);

        assertThat(
                new String(Files.readAllBytes(fs.getPath("/data/en/b/r/bruce_willis.html")), StandardCharsets.UTF_8),
                equalTo("body")
        );
    }
}
