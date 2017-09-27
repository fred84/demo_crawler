package crawler;

import org.junit.Test;
import org.xml.sax.SAXParseException;

import java.util.Set;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.junit.internal.matchers.ThrowableCauseMatcher.hasCause;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class ParserServiceTest {

    private final int MAX_DEPTH = 2;

    private ParserService service = new ParserService();
    private Page initialPage = Page.initial(new CrawlTask("http://en.wikipedia.org/wiki/1", 2, p -> {
    }));

    @Test
    public void noLinks() {
        String body = "<html>no links here</html>";

        Set<Page> result = service.parse(initialPage, service.convertToDoc(body.getBytes()));

        assertThat(result, is(empty()));
    }

    @Test
    public void linksOutOfContentZone() {
        String body = "<html><a href='https://en.wikipedia.org/wiki/1'>1</a><div class='mw-parser-output'>no links here</div></html>";

        Set<Page> result = service.parse(initialPage, service.convertToDoc(body.getBytes()));

        assertThat(result, is(empty()));
    }

    @Test
    public void skipNonWikipediaLinks() {
        String body = "<html><div class='mw-parser-output'><a href='https://hh.ru/parser'>1</a></div></html>";

        Set<Page> result = service.parse(initialPage, service.convertToDoc(body.getBytes()));

        assertThat(result, is(empty()));
    }

    @Test
    public void skipNonArticleLinks() {
        String body = "<html><div class='mw-parser-output'><a href='https://en.wikipedia.org/File:image.gif'>1</a></div></html>";

        Set<Page> result = service.parse(initialPage, service.convertToDoc(body.getBytes()));

        assertThat(result, is(empty()));
    }

    @Test
    public void noHref() {
        String body = "<html><div class='mw-parser-output'><p><a>1</a></p></div></html>";

        Set<Page> result = service.parse(initialPage, service.convertToDoc(body.getBytes()));

        assertThat(result, is(empty()));
    }

    @Test
    public void maxNestingReached() {
        String body = "<html><div class='mw-parser-output'><p><a href='/wiki/interesting'>1</a></p></div></html>";

        Page nested = Page.nested("/wiki/2", initialPage);
        Set<Page> result = service.parse(nested, service.convertToDoc(body.getBytes()));

        assertThat(result, is(empty()));
    }

    @Test
    public void imageIgnored() {
        String body = "<html><div class='mw-parser-output'><p><a href='/wiki/File:1.jpg'>1</a></p></div></html>";

        Set<Page> result = service.parse(initialPage, service.convertToDoc(body.getBytes()));

        assertThat(result, is(empty()));
    }

    @Test
    public void nestedLink() {
        String body = "<html><div class='mw-parser-output'><p><a href='/wiki/interesting'>1</a></p></div></html>";

        Set<Page> result = service.parse(initialPage, service.convertToDoc(body.getBytes()));

        assertThat(result, containsInAnyOrder(Page.nested("/wiki/interesting", initialPage)));
    }

    @Test
    public void firstChild() {
        String body = "<html><div class='mw-parser-output'><a href='/wiki/interesting'>1</a></div></html>";

        Set<Page> result = service.parse(initialPage, service.convertToDoc(body.getBytes()));

        assertThat(result, containsInAnyOrder(Page.nested("/wiki/interesting", initialPage)));
    }
}
