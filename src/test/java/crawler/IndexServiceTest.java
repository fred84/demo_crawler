package crawler;

import org.junit.Test;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import java.io.ByteArrayInputStream;
import java.io.IOException;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.internal.matchers.ThrowableMessageMatcher.hasMessage;
import static org.junit.jupiter.api.Assertions.assertThrows;


public class IndexServiceTest {

    private final IndexService service = new IndexService();


    @Test
    public void findEmpty() {
        assertThat(service.find("anything"), is(empty()));
    }

    @Test
    public void termSupportsOnlyWords() {
        Exception exc = assertThrows(IllegalArgumentException.class, () -> service.find("1900"));
        assertThat(exc, hasMessage(equalTo("Term should contain only letters and dash, but [1900] given")));
    }

    @Test
    public void indexAndFind() throws ParserConfigurationException, IOException, SAXException {
        service.index(
                "bruce_willis.html",
                createDocument(
                "<body><div>Out of scope</div><div class='mw-parser-output'>" +
                        "<img alt='text1' src=\"1.gif\"/> " +
                        "<p>Bruce is very popular <a>actor</a>.</p> 1900." +
                        "</div></body>"
        ));

        assertThat(service.find("very"), contains("bruce_willis.html"));
        assertThat(service.find("ACTOR"), contains("bruce_willis.html"));
        assertThat(service.find("out"), is(empty()));
    }

    @Test
    public void indexRussian() throws ParserConfigurationException, IOException, SAXException {
        service.index(
                "history.html",
                createDocument(
                        "<body><div class='mw-parser-output'>" +
                                "Популярная детская сказка." +
                                "<p>И очень интересная. <a>Салтыков-Щедрин</a>.</p>" +
                                "</div></body>"
                ));

        assertThat(service.find("и"), contains("history.html"));
        assertThat(service.find("Салтыков-Щедрин"), contains("history.html"));
    }

    @Test
    public void indexBoth() throws ParserConfigurationException, IOException, SAXException {
        service.index(
                "bruce.html",
                createDocument("<body><div class='mw-parser-output'>yandex</div></body>")
        );
        service.index(
                "history.html",
                createDocument("<body><div class='mw-parser-output'>yandex</div></body>")
        );

        assertThat(service.find("yandex"), containsInAnyOrder("bruce.html", "history.html"));
    }

    private Document createDocument(String text) throws ParserConfigurationException, IOException, SAXException {
        return DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(new ByteArrayInputStream(text.getBytes()));
    }

}
