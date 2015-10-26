package org.ets.research.nlp.corenlp.zeromq;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.ets.research.nlp.corenlp.stanford.CoreNLPWrapperUtil;
import org.ets.research.nlp.corenlp.stanford.StanfordDCorefWrapper;
import org.ets.research.nlp.corenlp.stanford.StanfordNERWrapper;
import org.ets.research.nlp.corenlp.stanford.StanfordParserWrapper;
import org.json.simple.JSONArray;
import org.json.simple.JSONValue;
import org.json.simple.parser.JSONParser;
import org.zeromq.ZMQ;
import org.zeromq.ZMQ.Context;

import edu.stanford.nlp.pipeline.Annotation;

public class StanfordZeroMQWorkerThread extends Thread
{
    private Context context;
    private JSONParser jsonParser;

    private StanfordDCorefWrapper dcoref;
    private StanfordParserWrapper parser;
    private StanfordNERWrapper ner;

    private String[] outputFormat = {"-outputFormat", "oneline"};

    public StanfordZeroMQWorkerThread(Context context, StanfordParserWrapper parser,
            StanfordNERWrapper ner, StanfordDCorefWrapper dcoref) throws IOException, ClassNotFoundException
    {
        this.context = context;
        this.jsonParser = new JSONParser();
        this.parser = parser;
        this.dcoref = dcoref;
        this.ner = ner;
    }

    private String parseRequestJSON(String incommingRequest) throws Exception
    {
        @SuppressWarnings("rawtypes")
        Map request = (Map)jsonParser.parse(incommingRequest);
        JSONArray requestedOutputs = (JSONArray) request.get("request");
        Map<String, Object> outputs = new HashMap<String, Object>();

        String rawRequestText = "";

        if (request.containsKey("text"))
        {
            rawRequestText = (String) request.get("text");
        }

        for (int i = 0; i < requestedOutputs.size(); i++)
        {
            if (requestedOutputs.get(i).equals("coref") || requestedOutputs.get(i).equals("dcoref"))
            {
                if (!outputs.containsKey("pcfg_parser"))
                {
                    outputs.put("pcfg_parser", parser.parseText(rawRequestText, Arrays.asList(outputFormat)));
                }
                @SuppressWarnings("unchecked")
                List<String> parseTrees = CoreNLPWrapperUtil.ParseTreeListObjectsToStrings((List<List<Object>>) outputs.get("pcfg_parser"));

                Annotation annotation = ner.annotateForNamedEntities(CoreNLPWrapperUtil.getAnnotationFromParseTrees(parseTrees));
                outputs.put("coref", dcoref.getCoreferencesFromAnnotation(annotation));
            }
            if (requestedOutputs.get(i).equals("pcfg_parser"))
            {
                if (!outputs.containsKey("pcfg_parser"))
                {
                    outputs.put("pcfg_parser", parser.parseText(rawRequestText, Arrays.asList(outputFormat)));
                }
            }
            if (requestedOutputs.get(i).equals("ner"))
            {
                if (!outputs.containsKey("ner"))
                {
                    if (!outputs.containsKey("pcfg_parser"))
                    {
                        outputs.put("pcfg_parser", parser.parseText(rawRequestText, Arrays.asList(outputFormat)));
                    }
                    @SuppressWarnings("unchecked")
                    List<String> parseTrees = CoreNLPWrapperUtil.ParseTreeListObjectsToStrings((List<List<Object>>) outputs.get("pcfg_parser"));
                    outputs.put("ner", ner.getNamedEntitiesFromTrees(parseTrees));
                }
            }
        }

        System.err.println(outputs.toString());
        return JSONValue.toJSONString(outputs);
    }

    public void run()
    {
        ZMQ.Socket socket = context.socket(ZMQ.REP);
        socket.connect ("inproc://workers");

        while (true)
        {
            // Wait for next request from client (C string)
            String request = socket.recvStr(0);//, StandardCharsets.UTF_8);
            System.err.println(Thread.currentThread().getName() + " Received request: [" + request + "]");

            try
            {
                // Send reply back to client (C string)
                String results = parseRequestJSON(request);
                socket.send(results, 0);
                Thread.sleep(1000); // ?
            }
            catch (Throwable e)
            {
                StringWriter sw = new StringWriter();
                PrintWriter pw = new PrintWriter(sw);
                pw.write("Error with request [" + request + "]\n");
                e.printStackTrace(pw);
                socket.send(sw.toString(), 0);
            }
        }
    }
}
