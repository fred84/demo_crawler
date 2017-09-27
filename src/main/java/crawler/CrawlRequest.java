package crawler;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

class CrawlRequest {

    private final String url;
    private final int depth;

    @JsonCreator
    CrawlRequest(@JsonProperty("url") String url, @JsonProperty("depth") int depth) {
        this.url = url;
        this.depth = depth;
    }

    String getUrl() {
        return url;
    }

    int getDepth() {
        return depth;
    }
}
