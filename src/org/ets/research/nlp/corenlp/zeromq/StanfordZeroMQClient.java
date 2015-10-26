package org.ets.research.nlp.corenlp.zeromq;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.json.simple.JSONValue;
import org.zeromq.ZMQ;

/**
 * Java client
 * Based on http://zguide.zeromq.org/java:hwclient
 * @author dnapolitano
 *
 */
public class StanfordZeroMQClient
{
    public static String requestStanfordCoreNLPOutput(String untokenizedText, List<String> outputs, String brokerAddress)
    {
        ZMQ.Context context = ZMQ.context(1);

        System.err.println("Connecting to CoreNLP ØMQ Server...");

        ZMQ.Socket requester = context.socket(ZMQ.REQ);
        requester.connect(brokerAddress);

        Map<String, Object> requestObject = new HashMap<String, Object>();
        requestObject.put("text", untokenizedText);
        requestObject.put("request", outputs);
        String requestString = JSONValue.toJSONString(requestObject);

        requester.send(requestString.getBytes(), 0);

        byte[] reply = requester.recv(0);
        String replyStr = new String(reply);
        requester.close();
        context.term();

        return replyStr;
    }

    public static void main(String[] args)
    {
        String[] requestedOutputs = {"pcfg_parser", "ner", "coref"};
        String result = StanfordZeroMQClient.requestStanfordCoreNLPOutput("Our president's name is Barack Obama.",
                Arrays.asList(requestedOutputs), "tcp://127.0.0.1:5555");
        System.out.println(result);
    }
}
