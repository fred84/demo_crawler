package crawler;

import org.htmlcleaner.DomSerializer;
import org.htmlcleaner.HtmlCleaner;
import org.htmlcleaner.TagNode;
import org.springframework.stereotype.Service;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
class ParserService {

    private final HtmlCleaner cleaner = new HtmlCleaner();

    private final ThreadLocal<XPathExpression> expression = ThreadLocal.withInitial(() -> {
        try {
            // assume all article links do not contain domain name
            return XPathFactory.newInstance().newXPath().compile("//div[@class='mw-parser-output']//a[starts-with(@href, '/wiki/')]");
        } catch (XPathExpressionException e) {
            throw new RuntimeException(e);
        }
    });

    ParserService() {
    }

    Document convertToDoc(byte[] body) {
        try {

            TagNode node = cleaner.clean(new ByteArrayInputStream(body));
            return new DomSerializer(cleaner.getProperties(), true).createDOM(node);
        } catch (IOException | ParserConfigurationException e) {
            throw new RuntimeException(e);
        }
    }

    Set<Page> parse(Page page, Document xmlDocument) {
        if (page.isMaxDepthReached()) {
            return Collections.emptySet();
        }

        try {
            return findLinksInDocument(xmlDocument)
                    .stream()
                    .map(n -> n.getAttributes().getNamedItem("href").getTextContent()) // href attribute presence already checked in xpath
                    .filter(url -> !url.contains("File:"))
                    .filter(url -> !url.contains("Special:"))
                    .filter(url -> !url.contains("Template:"))
                    .filter(url -> !url.contains("Help:"))
                    .map(url -> Page.nested(url, page))
                    .collect(Collectors.toSet());
        } catch (XPathExpressionException e) {
            throw new RuntimeException(e);
        }
    }

    private List<Node> findLinksInDocument(Document xmlDocument) throws XPathExpressionException {
        NodeList nodes = (NodeList) expression.get().evaluate(xmlDocument, XPathConstants.NODESET);
        List<Node> links = new ArrayList<>(nodes.getLength());
        for (int i = 0; i < nodes.getLength(); i++) {
            links.add(nodes.item(i));
        }
        return links;
    }
}