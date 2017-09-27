package crawler;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.springframework.stereotype.Service;

import java.io.IOException;

@Service
public class DownloadService {

    class HttpError extends RuntimeException {
        HttpError(String url, int code) {
            super("Error downloading url [" + url + "]; code: " + code);
        }

        HttpError(String url, Exception cause) {
            super("Error downloading url [" + url + "]", cause);
        }
    }

    private final OkHttpClient client = new OkHttpClient();

    byte[] getBodyBytes(Page page) {
        try {
            Response response = client
                    .newCall(new Request.Builder().url(page.getUrl()).build())
                    .execute();

            return convertResponseToBytes(page, response);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private byte[] convertResponseToBytes(Page attempt, Response response) {
        try {
            if (response.code() != 200) {
                throw new HttpError(attempt.getUrl().toString(), response.code());
            }

            try {
                return response.body().bytes();
            } catch (IOException e) {
                throw new HttpError(attempt.getUrl().toString(), e);
            }
        } finally {
            response.close();
        }
    }
}
