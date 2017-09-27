package crawler;

import org.junit.Test;

import java.util.function.Consumer;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasToString;
import static org.junit.Assert.assertThat;
import static org.junit.internal.matchers.ThrowableMessageMatcher.hasMessage;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.only;
import static org.mockito.Mockito.verify;

public class CrawlTaskTest {

    private final Consumer<CrawlTask.CallbackParams> defaultCallback = params -> {
    };

    @Test
    public void emptyUrl() {
        Exception exc = assertThrows(IllegalArgumentException.class, () -> new CrawlTask(null, 1, defaultCallback));
        assertThat(exc, hasMessage(equalTo("Url for crawling should be non-null")));
    }

    @Test
    public void invalidUrl() {
        Exception exc = assertThrows(IllegalArgumentException.class, () -> new CrawlTask("abc", 1, defaultCallback));
        assertThat(exc, hasMessage(equalTo("Unable to parse url")));
    }

    @Test
    public void otherDomain() {
        Exception exc = assertThrows(IllegalArgumentException.class, () -> new CrawlTask("http://example.com", 1, defaultCallback));
        assertThat(exc, hasMessage(equalTo("Domain should be [wikipedia.org], but [example.com] given")));
    }

    @Test
    public void nonArticle() {
        Exception exc = assertThrows(
                IllegalArgumentException.class,
                () -> new CrawlTask("http://en.wikipedia.org/some/other/path", 1, defaultCallback)
        );
        assertThat(exc, hasMessage(equalTo("Article path should start with '/wiki/', but [/some/other/path] given")));
    }

    @Test
    public void file() {
        Exception exc = assertThrows(
                IllegalArgumentException.class,
                () -> new CrawlTask("http://en.wikipedia.org/wiki/File:abc.jpg", 1, defaultCallback)
        );
        assertThat(exc, hasMessage(equalTo("Files and Special resources are not supported")));
    }

    @Test
    public void dropFragment() {
        CrawlTask req = new CrawlTask("https://en.wikipedia.org/wiki/Bruce_Willis#1980", 1, defaultCallback);
        assertThat(req.getUrl(), hasToString(equalTo("https://en.wikipedia.org/wiki/Bruce_Willis")));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void incrementDecrementAndCallback() {
        String url = "https://en.wikipedia.org/wiki/Bruce_Willis";
        Consumer<CrawlTask.CallbackParams> callback = mock(Consumer.class);

        CrawlTask req = new CrawlTask(url, 1, callback);
        req.increment();
        req.increment();
        req.decrement();
        req.decrementExceptionally();

        verify(callback, only()).accept(new CrawlTask.CallbackParams("https://en.wikipedia.org/wiki/Bruce_Willis", 2, 1));
    }

    @Test
    public void toStringValue() {
        CrawlTask req = new CrawlTask("https://en.wikipedia.org/wiki/Bruce_Willis", 1, defaultCallback);
        req.increment();
        req.increment();
        req.increment();
        req.decrement();
        req.decrement();

        assertThat(req, hasToString(equalTo("CrawlTask for url [https://en.wikipedia.org/wiki/Bruce_Willis] status: 1/3")));
    }
}
