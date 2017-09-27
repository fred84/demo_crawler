package crawler;

import org.junit.Before;
import org.junit.Test;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.function.Consumer;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.only;
import static org.mockito.Mockito.verify;


public class CrawlerServiceTest {

    private static class CurrentThreadExecutor implements Executor {
        @Override
        public void execute(Runnable r) {
            r.run();
        }
    }

    private final Executor executor = new CurrentThreadExecutor();

    private DownloadService downloadService = mock(DownloadService.class);
    private StorageService storageService = mock(StorageService.class);
    private ParserService parserService = mock(ParserService.class);
    private IndexService indexService = mock(IndexService.class);
    private CrawlService crawler;
    @SuppressWarnings("unchecked")
    private Consumer<CrawlTask.CallbackParams> callback = mock(Consumer.class);

    @Before
    public void setUp() {
        crawler = new CrawlService(
                executor, executor,
                downloadService, parserService, storageService, indexService,
                callback, 1
        );
    }

    @Test
    public void failOnFirstStep() throws ExecutionException, InterruptedException {
        doThrow(new RuntimeException("download failure")).when(downloadService).getBodyBytes(any());

        crawler.download(new CrawlRequest("https://en.wikipedia.org/wiki/Bruce_Willis", 1)).get();

        verify(storageService, never()).store(any(), any(byte[].class));
        verify(parserService, never()).parse(any(), any());
        verify(indexService, never()).index(anyString(), any());
        verify(callback, only()).accept(
                new CrawlTask.CallbackParams("https://en.wikipedia.org/wiki/Bruce_Willis", 1, 1)
        );
    }

    @Test
    public void success() throws ExecutionException, InterruptedException {
        doReturn("page content".getBytes()).when(downloadService).getBodyBytes(any());

        crawler.download(new CrawlRequest("https://en.wikipedia.org/wiki/Bruce_Willis", 1)).get();

        verify(callback, only()).accept(
                new CrawlTask.CallbackParams("https://en.wikipedia.org/wiki/Bruce_Willis", 1, 0)
        );
    }
}