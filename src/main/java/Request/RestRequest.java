package Request;

import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.json.simple.parser.JSONParser;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

public class RestRequest {


    private String host;
    private String port;
    private String url;

    private final CloseableHttpClient httpClient = HttpClients.createDefault();

    public RestRequest(String host, String port) {
        this.host = host;
        this.port = port;
        this.url = "http://" + host + ":" + port;
    }

    public String getJsonQueryResults(String query, String datasetId, String type, int size) throws UnsupportedEncodingException {

        /* Initiate a new GET request */
        HttpGet request = new HttpGet(url + "?query=" + URLEncoder.encode(query, "UTF-8") + "&id=" + datasetId + "&type=" + type + "&size=" + size);
        request.addHeader("Content-Type", "application/json; charset=utf-8");

        System.out.println(request.getURI());

        /* Fetch & return (json) response as a String */
        try (CloseableHttpResponse response = httpClient.execute(request)) {

            HttpEntity entity = response.getEntity();

            if (entity != null) {
                return EntityUtils.toString(entity);
            }

            return null;

        } catch (IOException e) {
            System.err.println("Error, on fetching response from the REST API: " + e.getMessage());
            System.exit(-1);
        }

        return null;

    }

}
