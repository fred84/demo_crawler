package crawler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.w3c.dom.Document;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

@Service
class CrawlService {

    private final Executor downloadExecutor;
    private final Executor parseAndIndexExecutor;

    private final DownloadService downloadService;
    private final ParserService parserService;
    private final StorageService storageService;
    private final IndexService indexService;

    private final Logger log = LoggerFactory.getLogger(this.getClass());
    private final Set<String> urls = Collections.synchronizedSet(new HashSet<>());
    private final Consumer<CrawlTask.CallbackParams> callback;
    private final int maxDepth;

    @Autowired
    CrawlService(
            @Autowired DownloadService downloadService,
            @Autowired ParserService parserService,
            @Autowired StorageService storageService,
            @Autowired IndexService indexService,
            @Value("${app.download.pool.size}") int downloadPoolSize,
            @Value("${app.parse.pool.size}") int savePoolSize,
            @Value("${app.download.max_depth}") int maxDepth
    ) {
        this.downloadService = downloadService;
        this.parserService = parserService;
        this.storageService = storageService;
        this.indexService = indexService;
        this.maxDepth = maxDepth;

        downloadExecutor = Executors.newFixedThreadPool(downloadPoolSize);
        parseAndIndexExecutor = Executors.newFixedThreadPool(savePoolSize);
        callback = p -> log.info(p.toString());
    }

    // for testing purpose
    CrawlService(
            Executor downloadExecutor,
            Executor parseAndIndexExecutor,
            DownloadService downloadService,
            ParserService parserService,
            StorageService storageService,
            IndexService indexService,
            Consumer<CrawlTask.CallbackParams> callback,
            int maxDepth

    ) {
        this.downloadExecutor = downloadExecutor;
        this.parseAndIndexExecutor = parseAndIndexExecutor;
        this.downloadService = downloadService;
        this.parserService = parserService;
        this.storageService = storageService;
        this.indexService = indexService;
        this.callback = callback;
        this.maxDepth = maxDepth;
    }

    Set<String> getUrls() {
        return urls;
    }

    // rejection for too much concurrent requests omitted
    CompletableFuture download(CrawlRequest req) {
        if (req.getDepth() <= 0 || req.getDepth() > maxDepth) {
            throw new IllegalArgumentException("Depth should be between [0," + maxDepth + "], but [" + req.getDepth() + "] given");
        }

        Page page = Page.initial(new CrawlTask(req.getUrl(), req.getDepth(), callback));

        log.info("[" + page.getUrl() + "] with depth [" + req.getDepth() + "] submitted");

        if (isAlreadyDownloaded(page)) {
            page.start();
            page.complete();
            return CompletableFuture.completedFuture(null);
        }

        return download(page);
    }

    private boolean isAlreadyDownloaded(Page page) {
        return urls.contains(page.getCanonicalForm());
    }

    private CompletableFuture download(final Page page) {
        if (isAlreadyDownloaded(page)) {
            return CompletableFuture.completedFuture(null);
        }

        urls.add(page.getCanonicalForm());
        page.start();

        return CompletableFuture
                // retries are currently not supported
                .supplyAsync(() -> downloadService.getBodyBytes(page), downloadExecutor)
                .thenApplyAsync(bytes -> {
                    storageService.store(page, bytes);
                    // we can launch indexing separately so it do not interfere with crawling
                    Document doc = parserService.convertToDoc(bytes);

                    indexService.index(page.getRelativePath().toString(), doc);
                    return parserService.parse(page, doc);
                }, parseAndIndexExecutor)
                .thenAccept(pages -> {
                    pages.forEach(this::download);
                    page.complete();
                })
                .exceptionally(e -> {
                    log.error("error during processing [" + page.getUrl() + "]", e);
                    page.completeExceptionally();
                    urls.remove(page.getCanonicalForm());
                    return null;
                });
    }
}
