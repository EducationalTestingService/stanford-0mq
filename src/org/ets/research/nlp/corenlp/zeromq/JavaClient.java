package org.ets.research.nlp.corenlp.zeromq;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.json.simple.JSONValue;
import org.json.simple.parser.JSONParser;
import org.zeromq.ZMQ;

public class JavaClient
{
    public static String requestStanfordCoreNLPOutput(List<List<String>> input, String type, String brokerAddress)
    {
        ZMQ.Context context = ZMQ.context(1);

        System.err.println("Connecting to CoreNLP ØMQ Server...");

        ZMQ.Socket requester = context.socket(ZMQ.REQ);
        requester.connect(brokerAddress);

        Map<String, Object> requestObject = new HashMap<String, Object>();
        requestObject.put(type, input);
        String requestString = JSONValue.toJSONString(requestObject);

        requester.send(requestString.getBytes(), 0);

        byte[] reply = requester.recv(0);
        String replyStr = new String(reply);
        requester.close();
        context.term();

        return replyStr;
    }

    @SuppressWarnings("unchecked")
    public static void main(String[] args)
    {
        JSONParser jsonParser = new JSONParser();
        List<List<String>> tokenizedText = new ArrayList<List<String>>();
        List<String> test = new ArrayList<String>();
        test.add("Hello");
        test.add(",");
        test.add("world");
        test.add("!");
        tokenizedText.add(test);

        String brokerAddress = "tcp://127.0.0.1:5555";

        try
        {
            String result = JavaClient.requestStanfordCoreNLPOutput(tokenizedText, "tokens", brokerAddress);
            Map<String, Object> tagsResult = (Map<String, Object>) jsonParser.parse(result);
            List<List<String>> tags = (List<List<String>>) tagsResult.get("tags");
            //System.out.println(tags);


            result = JavaClient.requestStanfordCoreNLPOutput(tags, "tags", brokerAddress);
            Map<String, Object> treesResult = (Map<String, Object>) jsonParser.parse(result);
            List<String> trees = (List<String>) treesResult.get("trees");
            System.out.println(trees);
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }
}
