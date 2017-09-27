package crawler;

import okhttp3.HttpUrl;

import javax.annotation.concurrent.ThreadSafe;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

@ThreadSafe
class CrawlTask {

    static class CallbackParams {

        final String url;
        final int total;
        final int failed;

        CallbackParams(String url, int total, int failed) {
            assert null != url : "url is mandatory param";
            this.url = url;
            this.total = total;
            this.failed = failed;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }

            CallbackParams that = (CallbackParams) o;

            return total == that.total && failed == that.failed && url.equals(that.url);
        }

        @Override
        public int hashCode() {
            int result = url.hashCode();
            result = 31 * result + total;
            result = 31 * result + failed;
            return result;
        }

        @Override
        public String toString() {
            return "CrawlTask for url [" + url + "] completed. Failed urls: " + failed + ". Total urls: " + total;
        }
    }

    private final AtomicInteger urlsInProcessCount = new AtomicInteger(0);
    private final AtomicInteger failedUrlsCount = new AtomicInteger(0);
    private final AtomicInteger totalUrlsCount = new AtomicInteger(0);
    private final HttpUrl url;
    private final Consumer<CallbackParams> callback;
    private final int depth;

    CrawlTask(String urlStr, int depth, Consumer<CallbackParams> callback) {
        assert null != callback : "callback is mandatory param";

        if (null == urlStr) {
            throw new IllegalArgumentException("Url for crawling should be non-null");
        }

        this.depth = depth;

        HttpUrl url = HttpUrl.parse(urlStr);
        validateUrl(url);
        if (url.fragment() != null) {
            url = new HttpUrl.Builder().scheme(url.scheme()).host(url.host()).encodedPath(url.encodedPath()).build();
        }

        this.url = url;
        this.callback = callback;
    }

    static void validateUrl(HttpUrl url) {
        if (null == url) {
            throw new IllegalArgumentException("Unable to parse url");
        }

        if (null == url.topPrivateDomain() || !url.topPrivateDomain().endsWith("wikipedia.org")) {
            throw new IllegalArgumentException("Domain should be [wikipedia.org], but [" + url.topPrivateDomain() + "] given");
        }

        if (url.pathSegments().size() < 2 || !url.pathSegments().get(0).equals("wiki")) {
            throw new IllegalArgumentException("Article path should start with '/wiki/', but [" + url.encodedPath() + "] given");
        }

        if (url.pathSegments().get(1) == null || url.pathSegments().get(1).equals("")) {
            throw new IllegalArgumentException("Article should not be empty");
        }

        if (url.encodedPath().contains("File:") || url.encodedPath().contains("Special:") || url.encodedPath().contains("Template:")) {
            throw new IllegalArgumentException("Files and Special resources are not supported");
        }
    }

    HttpUrl getUrl() {
        return url;
    }

    int getDepth() {
        return depth;
    }

    void increment() {
        urlsInProcessCount.incrementAndGet();
        totalUrlsCount.incrementAndGet();
    }

    void decrement() {
        assert urlsInProcessCount.get() > 0 : "No urls in progress found";

        if (0 == urlsInProcessCount.decrementAndGet()) {
            callback.accept(new CallbackParams(url.toString(), totalUrlsCount.get(), failedUrlsCount.get()));
        }
    }

    void decrementExceptionally() {
        assert urlsInProcessCount.get() > 0 : "No urls in progress found";
        failedUrlsCount.incrementAndGet();
        decrement();
    }

    @Override
    public String toString() {
        return "CrawlTask for url [" + url + "] status: " + urlsInProcessCount + "/" + totalUrlsCount;
    }
}