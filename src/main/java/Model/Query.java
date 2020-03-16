package Model;

public class Query {

    String queryId;
    String queryText;

    public Query(String queryId, String queryText) {
        this.queryId = queryId;
        this.queryText = queryText;
    }

    public String getQueryId() {
        return queryId;
    }

    public String getQueryText() {
        return queryText;
    }
}
