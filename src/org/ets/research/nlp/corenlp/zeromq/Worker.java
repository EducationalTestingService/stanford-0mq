package org.ets.research.nlp.corenlp.zeromq;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.zeromq.ZMQ;
import org.zeromq.ZMQ.Context;

import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.ling.Sentence;
import edu.stanford.nlp.ling.TaggedWord;
import edu.stanford.nlp.ling.TaggedWordFactory;
import edu.stanford.nlp.parser.shiftreduce.ShiftReduceParser;
import edu.stanford.nlp.tagger.maxent.MaxentTagger;
import edu.stanford.nlp.trees.PennTreebankLanguagePack;
import edu.stanford.nlp.trees.Tree;
import edu.stanford.nlp.trees.TreePrint;
import edu.stanford.nlp.trees.TreebankLanguagePack;

public class Worker extends Thread
{
    private Context context;
    private JSONParser jsonParser;
    private ShiftReduceParser parser;
    private MaxentTagger tagger;

    public Worker(Context c, ShiftReduceParser p, MaxentTagger t)
    {
        this.context = c;
        this.parser = p;
        this.tagger = t;
        this.jsonParser = new JSONParser();
    }

    /**
     * Request could contain:
     * {"tokens" : [["This", "is", "a", "sentence", "."], ["Here", "is", "another", "one", "."]]}
     * or
     * {"tags" : [["Hello/UH", ",/,", "world/NN", "!/."], ...]}
     * @param incommingRequest
     * @return
     * @throws Exception
     */
    @SuppressWarnings("unchecked")
    private String parseRequestJSON(String incommingRequest) throws Exception
    {
        @SuppressWarnings("rawtypes")
        Map request = (Map)jsonParser.parse(incommingRequest);
        Map<String, Object> output = new HashMap<String, Object>();

        List<List<String>> input = null;

        if (request.containsKey("tokens"))
        {
            input = (List<List<String>>) request.get("tokens");
            output.put("tags", tagTokenizedSentences(input));
        }
        else if (request.containsKey("tags"))
        {
            // each String is like "Hello/UH"
            input = (List<List<String>>) request.get("tags");
            output.put("trees", parseTaggedSentences(input));
        }

        System.err.println(output.toString());
        return JSONObject.toJSONString(output);
    }

    private List<List<String>> tagTokenizedSentences(List<List<String>> tokenizedSentences)
    {
        List<List<String>> taggedTokenizedSentences = new ArrayList<List<String>>();

        for (List<String> tokenizedSentence : tokenizedSentences)
        {
            String[] tokenArray = new String[tokenizedSentence.size()];
            tokenizedSentence.toArray(tokenArray);
            List<CoreLabel> stanfordFormat = Sentence.toCoreLabelList(tokenArray);
            List<TaggedWord> outputFromTagger = tagger.apply(stanfordFormat);
            // each element is like "dog/NN"
            List<String> taggedSentence = new ArrayList<String>();
            for (TaggedWord tw : outputFromTagger)
            {
                taggedSentence.add(tw.word() + "/" + tw.tag());
            }
            taggedTokenizedSentences.add(taggedSentence);
        }

        return taggedTokenizedSentences;
    }

    private List<String> parseTaggedSentences(List<List<String>> taggedTokenizedSentences)
    {
        List<String> parseTrees = new ArrayList<String>();
        //String[] outputFormat = {"-outputFormat", "oneline"};
        TaggedWordFactory tf = new TaggedWordFactory('/');
        TreebankLanguagePack tlp = new PennTreebankLanguagePack();

        for (List<String> taggedSentence : taggedTokenizedSentences)
        {
            String[] taggedTokensArray = new String[taggedSentence.size()];
            taggedSentence.toArray(taggedTokensArray);
            List<TaggedWord> taggedWordList = new ArrayList<TaggedWord>();
            for (String taggedToken : taggedTokensArray)
            {
                taggedWordList.add((TaggedWord)tf.newLabelFromString(taggedToken));
            }

            TreePrint treePrinter = new TreePrint("oneline", tlp);
            Tree parseTree = parser.apply(taggedWordList);
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            treePrinter.printTree(parseTree, pw);
            parseTrees.add(sw.getBuffer().toString().trim());
        }

        return parseTrees;
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
