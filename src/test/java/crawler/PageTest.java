package crawler;

import okhttp3.HttpUrl;
import org.junit.Test;

import java.nio.file.Paths;

import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.Assert.assertThat;

public class PageTest {


    private final Page parentEnglish = Page.initial(
            new CrawlTask("https://en.wikipedia.org/wiki/Bruce_Willis", 1, p -> {})
    );
    private final Page parentFrench = Page.initial(
            new CrawlTask("https://fr.wikipedia.org/wiki/Bruce_Willis", 1, p -> {})
    );

    @Test
    public void languageResolution() {
        assertThat(
                Page.nested("/wiki/Emmy_Award", parentEnglish).getUrl(),
                equalTo(HttpUrl.parse("https://en.wikipedia.org/wiki/Emmy_Award"))
        );

        assertThat(
                Page.nested("/wiki/Emmy_Award", parentFrench).getUrl(),
                equalTo(HttpUrl.parse("https://fr.wikipedia.org/wiki/Emmy_Award"))
        );
    }

    @Test
    public void anchorsRemoved() {
        assertThat(
                Page.nested("/wiki/Emmy_Award#Emmy_statuette", parentEnglish).getUrl(),
                equalTo(HttpUrl.parse("https://en.wikipedia.org/wiki/Emmy_Award"))
        );
    }

    @Test
    public void firstSubDomain() {
        assertThat(
                parentEnglish.getFirstSubdomain(),
                equalTo("en")
        );
    }

    @Test
    public void toFileName() {
        assertThat(
                parentEnglish.getRelativePath(),
                equalTo(Paths.get("en", "b", "r", "bruce_willis.html"))
        );

        assertThat(
                Page.initial(new CrawlTask("https://wikipedia.org/wiki/B", 1, p -> {
                })).getRelativePath(),
                equalTo(Paths.get("www", "b.html"))
        );

        assertThat(
                Page.initial(new CrawlTask("https://en.wikipedia.org/wiki/B_W", 1, p -> {
                })).getRelativePath(),
                equalTo(Paths.get("en", "b", "b_w.html"))
        );

        assertThat(
                Page.initial(new CrawlTask("http://en.wikipedia.org/wiki/Alliance_90/The_Greens", 1, p -> {
                })).getRelativePath(),
                equalTo(Paths.get("en", "a", "l", "alliance_90_the_greens.html"))
        );


    }
}
