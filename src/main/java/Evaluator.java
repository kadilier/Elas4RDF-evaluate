import Model.Qrel;
import Model.Query;
import Model.Result;
import org.json.simple.parser.ParseException;

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
    public Map<String, Float> queryTimesMap;

    public String queryCategory;
    public String datasetId;
    public int responseSize;
    public boolean aggregationPenalty;
    public float aggregationFactor;
    public int aggregationVersion;

    public static void main(String[] args) throws IOException, ParseException {

        Evaluator evaluator = new Evaluator();

        evaluator.queriesMap = new HashMap<>();
        evaluator.qrelsMap = new HashMap<>();
        evaluator.resultsMap = new HashMap<>();
        evaluator.ndcgResMap = new HashMap<>();
        evaluator.queryTimesMap = new HashMap<>();

        evaluator.datasetId = "dbpedia";
        evaluator.queryCategory = args[1];
        evaluator.responseSize = Integer.parseInt(args[2]);
        evaluator.aggregationPenalty = Boolean.parseBoolean(args[3]);
        evaluator.aggregationFactor = Float.parseFloat(args[4]);
        evaluator.aggregationVersion = Integer.parseInt(args[5]);

        /* initialize a Rest request & load queries and qrels */
        RestRequest restRequest = new RestRequest("139.91.183.46", "8080/elas4rdf_rest/");
        evaluator.readQueries(args[0]);
        evaluator.readQrels("/home/giorgos/Documents/Elas4RDF-evaluate/src/main/resources/qrels-v2.txt");

        /* perform requests & store results */
        for (String queryId : evaluator.queriesMap.keySet()) {
            String queryText = evaluator.queriesMap.get(queryId).getQueryText();
            Result result = new Result(queryId);

            long q_start = System.currentTimeMillis();

            String jsonResponse = restRequest.getJsonQueryResults(queryText, "dbpedia", evaluator);
            result.setJsonResponse(jsonResponse);
            result.parseEntities();
            evaluator.resultsMap.put(queryId, result);

            long q_end = System.currentTimeMillis() - q_start;

            float idcg = evaluator.calculateIdcg(evaluator.qrelsMap.get(queryId), 100);
            float dcg = evaluator.calculateDcg(queryId, 100);
            float ndcg = dcg / idcg;

            if (idcg == 0) {
                System.out.println("IDCG NULL : " + queryId);
                continue;
            }

            System.out.println("Q:" + queryId + " " + ndcg);

            evaluator.ndcgResMap.put(queryId, ndcg);
            evaluator.queryTimesMap.put(queryId, q_end / 1000F);

        }

        double average_ndcg = evaluator.ndcgResMap.values().stream().mapToDouble(Float::floatValue).average().orElse(0);
        double average_time = evaluator.queryTimesMap.values().stream().mapToDouble(Float::floatValue).average().orElse(0);

        System.out.println();
        System.out.println("ndcg: " + average_ndcg);
        System.out.println("time: " + average_time);

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


        if (qrel.getResources() == null) {
            return 0;
        }

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
        float score = 0, prev_score = 0;

        Result result = resultsMap.get(queryId);
        Qrel qrel = qrelsMap.get(queryId);

        Map<String, Float> resultMap = result.getResourcesRet();
        for (String resource : resultMap.keySet()) {

            score = resultMap.get(resource);

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

            if (score != prev_score) {
                prev_score = score;
                i++;
            }

        }

        return dcg;

    }


}
