package Model;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class Result {

    String queryId;
    String jsonResponse;
    Map<String, Float> resourcesRet;

    private static final JSONParser parser = new JSONParser();

    public Result(String queryId) {
        this.queryId = queryId;
        this.resourcesRet = new LinkedHashMap<>();
    }

    public void parseEntities() throws ParseException {

        JSONObject jsonResponse = (JSONObject) parser.parse(this.jsonResponse);
        JSONObject results = (JSONObject) jsonResponse.get("results");
        JSONArray entities = (JSONArray) results.get("entities");

        for (int i = 0; i < entities.size(); i++) {
            JSONObject resultNode = (JSONObject) entities.get(i);
            resourcesRet.put(resultNode.get("entity").toString().replace("http://dbpedia.org/resource/", "dbpedia:"),
                    Float.parseFloat(resultNode.get("score").toString()));
        }

    }

    public void setJsonResponse(String jsonResponse) {
        this.jsonResponse = jsonResponse;
    }

    public Map<String, Float> getResourcesRet() {
        return this.resourcesRet;
    }

}
