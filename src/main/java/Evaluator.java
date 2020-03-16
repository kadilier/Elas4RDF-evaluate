import Model.Qrel;
import Model.Query;
import Model.Result;
import Request.RestRequest;
import org.json.simple.parser.ParseException;
import sun.reflect.generics.tree.Tree;

import java.io.*;
import java.util.*;

/**
 * Performs evaluation on Elas4RDF system
 * based on the 'DBpedia-Entity' test collection
 */
public class Evaluator {

    /* key : 'queryId' */
    public Map<String, Query> queriesMap;
    public Map<String, Qrel> qrelsMap;
    public Map<String, Result> resultsMap;
    public Map<String, Float> ndcgResMap;

    public String queryCategory;
    public String datasetId;
    public int responseSize;

    public static void main(String[] args) throws IOException, ParseException {

        Evaluator evaluator = new Evaluator();

        evaluator.queriesMap = new HashMap<>();
        evaluator.qrelsMap = new HashMap<>();
        evaluator.resultsMap = new HashMap<>();
        evaluator.ndcgResMap = new HashMap<>();

        evaluator.queryCategory = "SemSearch_ES";
        evaluator.datasetId = "dbpedia";
        evaluator.responseSize = 500;

        /* initialize a Rest request & load queries and qrels */
        RestRequest restRequest = new RestRequest("139.91.183.46", "8080/elas4rdf_rest/");
        evaluator.readQueries("queries-v2_stopped.txt");
        evaluator.readQrels("qrels-v2.txt");

        /* perform requests & store results */
        for (String queryId : evaluator.qrelsMap.keySet()) {
            String queryText = evaluator.queriesMap.get(queryId).getQueryText();
            Result result = new Result(queryId);

            String jsonResponse = restRequest.getJsonQueryResults(queryText, evaluator.datasetId, "entities", evaluator.responseSize);
            result.setJsonResponse(jsonResponse);
            result.parseEntities();
            evaluator.resultsMap.put(queryId, result);


            float idcg = evaluator.calculateIdcg(evaluator.qrelsMap.get(queryId), 100);
            float dcg = evaluator.calculateDcg(queryId, 100);
            float ndcg = dcg / idcg;

            evaluator.ndcgResMap.put(queryId, ndcg);

            System.out.println("#### RESULTS ####");
            System.out.println("ndcg: " + ndcg);


        }


    }

    public void readQueries(String fileName) throws IOException {

        File queriesFile = new File(fileName);
        BufferedReader br = new BufferedReader(new FileReader(queriesFile));

        String line;
        while ((line = br.readLine()) != null) {
            String queryId = line.split("\t")[0];
            String queryText = line.split("\t")[1];

            if (queryCategory.equals("ListSearch")) {
                if (queryId.contains("SemSearch_ES") || queryId.contains("INEX_LD") || queryId.contains("QALD2")) {
                    continue;
                }
            } else {
                if (!queryId.contains(queryCategory)) {
                    continue;
                }
            }

            Query query = new Query(queryId, queryText);
            this.queriesMap.put(queryId, query);
        }

    }

    public void readQrels(String fileName) throws IOException {
        File qrelsFile = new File(fileName);
        BufferedReader br = new BufferedReader(new FileReader(qrelsFile));

        String line;
        while ((line = br.readLine()) != null) {
            String[] contents = line.split("\t");
            String queryId = contents[0];
            String resource = contents[2].replace("<", "").replace(">", "");
            Integer relScore = Integer.parseInt(contents[3]);

            if (queryCategory.equals("ListSearch")) {
                if (queryId.contains("SemSearch_ES") || queryId.contains("INEX_LD") || queryId.contains("QALD2")) {
                    continue;
                }
            } else {
                if (!queryId.contains(queryCategory)) {
                    continue;
                }
            }

            Qrel qrel;
            if (qrelsMap.containsKey(queryId)) {
                qrel = qrelsMap.get(queryId);
            } else {
                qrel = new Qrel(queryId);
            }

            qrel.addResource(resource, relScore);
            qrelsMap.put(queryId, qrel);

        }

    }

    public float calculateIdcg(Qrel qrel, int resNum) {

        float idcg = 0;
        int i = 1;

        /* sort resources (descending order) */
        qrel.sortResources();

        for (String resource : qrel.getResources().keySet()) {

            Integer relevance = qrel.getResources().get(resource);
            if (relevance == 0 || i > resNum) {
                break;
            }

            idcg += (Math.pow(2, relevance) - 1) / (Math.log(i + 1) / Math.log(2));
            i++;
        }

        return idcg;

    }

    public float calculateDcg(String queryId, int resNum) {

        float dcg = 0;
        int i = 1;
        int jdg_n = 1;
        int rel_n = 1;

        Result result = resultsMap.get(queryId);
        Qrel qrel = qrelsMap.get(queryId);

        for (String resource : result.getResourcesRet()) {

            if (i > resNum) {
                break;
            }

            if (qrel.containsResource(resource)) {
                int jdg_i = qrel.getRelevance(resource);
                if (jdg_i != 0) {
                    rel_n++;
                }
                dcg += (Math.pow(2, jdg_i) - 1) / (Math.log(jdg_n + 1) / Math.log(2));
                jdg_n++;
            }
            i++;
        }

        return dcg;

    }


}
