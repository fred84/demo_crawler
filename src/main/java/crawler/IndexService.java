package crawler;

import com.google.common.collect.ImmutableSet;
import jdk.nashorn.internal.ir.annotations.Immutable;
import org.springframework.stereotype.Service;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.stream.Collectors;

@Service
public class IndexService {

    @Immutable
    private static class Term {

        private final String term;

        static boolean isValid(String term) {
            return clean(term).length() > 0;
        }

        Term(String term) {
            assert isValid(term);
            this.term = clean(term);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }

            Term term1 = (Term) o;
            return term.equals(term1.term);
        }

        @Override
        public int hashCode() {
            return term.hashCode();
        }

        @Override
        public String toString() {
            return "Term [" + term + "]";
        }

        private static String clean(String str) {
            return str.toLowerCase().replaceAll("[^a-zа-я\\-]", "");
        }
    }

    @Immutable
    static class ParsedDocument {

        private final String fileName;
        private final Set<Term> words;

        ParsedDocument(String fileName, Set<Term> words) {
            this.fileName = fileName;
            this.words = ImmutableSet.copyOf(words);
        }

        boolean contains(Term term) {
            return words.contains(term);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }

            ParsedDocument that = (ParsedDocument) o;
            return fileName.equals(that.fileName);
        }

        @Override
        public int hashCode() {
            return 31 * fileName.hashCode();
        }
    }

    // assume page is indexed again only after application reload
    private Set<ParsedDocument> parsedDocuments = Collections.synchronizedSet(new HashSet<>());

    private final ThreadLocal<XPathExpression> expression = ThreadLocal.withInitial(() -> {
        try {
            return XPathFactory.newInstance().newXPath().compile("//div[@class='mw-parser-output']//text()");
        } catch (XPathExpressionException e) {
            throw new RuntimeException(e);
        }
    });

    Set<String> find(String str) {
        if (!Term.isValid(str)) {
            throw new IllegalArgumentException("Term should contain only letters and dash, but [" + str + "] given");
        }

        Term term = new Term(str);

        return parsedDocuments
                .stream()
                .filter(d -> d.contains(term))
                .map(d -> d.fileName)
                .collect(Collectors.toSet());
    }

    // todo pass path instead string
    void index(String url, Document doc) {
        try {
            NodeList nodes = (NodeList) expression.get().evaluate(doc, XPathConstants.NODESET);

            List<String> texts = new LinkedList<>();
            for (int i = 0; i < nodes.getLength(); i++) {
                texts.add(nodes.item(i).getTextContent());
            }
            tokenize(url, texts);
        } catch (XPathExpressionException e) {
            throw new RuntimeException(e);
        }
    }

    private void tokenize(String url, List<String> texts) {
        final Set<Term> words = new HashSet<>();
        for (String text: texts) {
            StringTokenizer tokenizer = new StringTokenizer(text, " \t\n\r\f,.:;?![]'");
            while (tokenizer.hasMoreTokens()) {
                String token = tokenizer.nextToken();
                if (Term.isValid(token)) {
                    words.add(new Term(token));
                }
            }
        }

        parsedDocuments.add(new ParsedDocument(url, words));
    }
}
