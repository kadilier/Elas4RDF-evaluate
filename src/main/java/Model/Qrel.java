package Model;

import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

public class Qrel {

    /* key : 'resourceID' */
    String queryId;
    Map<String, Integer> resources;

    public Qrel(String queryId) {
        this.queryId = queryId;
        this.resources = new HashMap<>();
    }

    public void sortResources() {
        this.resources = this.resources.entrySet().stream().sorted(Map.Entry.comparingByValue(Comparator.reverseOrder())).
                collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (e1, e2) -> e1, LinkedHashMap::new));
    }

    public void addResource(String resourceText, Integer relScore) {
        this.resources.put(resourceText, relScore);
    }

    public Map<String, Integer> getResources() {
        return this.resources;
    }

    public boolean containsResource(String resource) {
        return this.resources.containsKey(resource);
    }

    public int getRelevance(String resource) {
        return this.resources.get(resource);
    }

    public String getQueryId() {
        return this.queryId;
    }

}
