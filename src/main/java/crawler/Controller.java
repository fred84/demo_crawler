package crawler;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.Set;

@RestController
public class Controller {

    private static class ErrorResponse {

        private final String error;

        ErrorResponse(String message) {
            this.error = message;
        }

        String getError() {
            return error;
        }
    }

    private final CrawlService service;
    private final IndexService indexService;

    Controller(@Autowired CrawlService service, @Autowired IndexService indexService) {
        this.service = service;
        this.indexService = indexService;
    }

    @RequestMapping(value = "/create", method = RequestMethod.PUT)
    @ResponseStatus(value = HttpStatus.ACCEPTED)
    public void create(@RequestBody CrawlRequest request) {
        service.download(request);
    }

    @RequestMapping(value = "/urls", method = RequestMethod.GET)
    public Set<String> urls() {
        return service.getUrls();
    }

    @RequestMapping(value = "/count", method = RequestMethod.GET)
    public int count() {
        return service.getUrls().size();
    }

    @RequestMapping(value = "/find/{word}", method = RequestMethod.GET)
    public Set<String> find(@PathVariable(value = "word") String word) {
        return indexService.find(word);
    }

    @ResponseStatus(value = HttpStatus.BAD_REQUEST)
    @ExceptionHandler(IllegalArgumentException.class)
    public ErrorResponse handleValidation(Exception e) {
        return new ErrorResponse(e.getMessage());
    }
}