package crawler;

import okhttp3.HttpUrl;

import javax.annotation.concurrent.ThreadSafe;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@ThreadSafe
class Page {

    private final HttpUrl url;
    private final int depth;
    private final CrawlTask task;

    static Page initial(CrawlTask request) {
        assert null != request : "task is mandatory param";

        return new Page(request, request.getUrl(), 1);
    }

    static Page nested(String urlStr, Page parent) {
        assert null != urlStr : "urlStr is mandatory param";
        assert null != parent : "parent attempt is mandatory param";

        HttpUrl url = new HttpUrl.Builder()
                .scheme(parent.url.scheme())
                .host(parent.url.host())
                .encodedPath(urlStr.split("#")[0]) // in wikipedia anchors are only used to reference to inner document parts
                .build();

        CrawlTask.validateUrl(url);

        return new Page(parent.task, url, parent.depth + 1);
    }

    private Page(CrawlTask request, HttpUrl url, int depth) {
        this.url = url;
        this.depth = depth;
        this.task = request;
    }

    boolean isMaxDepthReached() {
        return depth >= task.getDepth();
    }

    String getFirstSubdomain() { // in most cases it should be language
        String[] hostParts = url.host().split(Pattern.quote("."));

        if (hostParts[0].equals("wikipedia")) {
            return "www";
        } else {
            return hostParts[0];
        }
    }

    Path getRelativePath() {
        String name = url
                .pathSegments()
                .stream()
                .skip(1)
                .map(String::toLowerCase)
                .collect(Collectors.joining("_"));

        String alphaDigitName = name.replaceAll("[^a-zа-я0-9]+", "").replaceAll("/", "_");
        switch (alphaDigitName.length()) {
            case 1:
                return Paths.get(getFirstSubdomain(), name + ".html");
            case 2:
                return Paths.get(getFirstSubdomain(), alphaDigitName.substring(0, 1), name + ".html");
            default:
                return Paths.get(getFirstSubdomain(), alphaDigitName.substring(0, 1), alphaDigitName.substring(1, 2), name + ".html");
        }
    }

    void start() {
        task.increment();
    }

    void complete() {
        task.decrement();
    }

    void completeExceptionally() {
        task.decrementExceptionally();
    }

    HttpUrl getUrl() {
        return url;
    }

    String getCanonicalForm() {
        return getFirstSubdomain() + "/" + url.pathSegments().get(1).toLowerCase();
    }

    int getDepth() {
        return depth;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        Page attempt = (Page) o;

        return depth == attempt.depth && url.equals(attempt.url) && task.equals(attempt.task);
    }

    @Override
    public int hashCode() {
        int result = url.hashCode();
        result = 31 * result + depth;
        result = 31 * result + task.hashCode();
        return result;
    }

    @Override
    public String toString() {
        return "Page for url: [" + url + "] at depth " + depth + " for task [" + task + ']';
    }
}
