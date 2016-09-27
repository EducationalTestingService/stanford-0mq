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
    public static String requestStanfordCoreNLPOutput(List<List<String>> tokenizedText, String brokerAddress)
    {
        ZMQ.Context context = ZMQ.context(1);

        System.err.println("Connecting to CoreNLP ØMQ Server...");

        ZMQ.Socket requester = context.socket(ZMQ.REQ);
        requester.connect(brokerAddress);

        Map<String, Object> requestObject = new HashMap<String, Object>();
        requestObject.put("tokens", tokenizedText);
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

        try
        {
            String jsonResult = JavaClient.requestStanfordCoreNLPOutput(tokenizedText,
                    "tcp://127.0.0.1:5555");
            Map<String, Object> result = (Map<String, Object>) jsonParser.parse(jsonResult);
            System.out.println(result.get("tags"));
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }
}
