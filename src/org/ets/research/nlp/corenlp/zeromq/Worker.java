package org.ets.research.nlp.corenlp.zeromq;

import java.io.PrintWriter;
import java.io.StringReader;
import java.io.StringWriter;
import java.text.DateFormat;
import java.text.MessageFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.zeromq.ZMQ;
import org.zeromq.ZMQ.Context;

import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.ling.HasWord;
import edu.stanford.nlp.ling.SentenceUtils;
import edu.stanford.nlp.ling.TaggedWord;
import edu.stanford.nlp.ling.TaggedWordFactory;
import edu.stanford.nlp.parser.shiftreduce.ShiftReduceParser;
import edu.stanford.nlp.process.DocumentPreprocessor;
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
     * {"text: : "This is a sentence.  Here is another one.  Hello, world!"}
     * or
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

        if (request.containsKey("text"))
        {
            String rawTextInput = (String) request.get("text");
            output.put("tokens", tokenizeText(rawTextInput));
        }
        else if (request.containsKey("tokens"))
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

        return JSONObject.toJSONString(output);
    }

    private List<List<String>> tokenizeText(String rawTextInput)
    {
        List<List<String>> tokenizedSentences = new ArrayList<List<String>>();

        DocumentPreprocessor preprocess = new DocumentPreprocessor(new StringReader(rawTextInput));
        Iterator<List<HasWord>> foundSentences = preprocess.iterator();
        while (foundSentences.hasNext())
        {
            List<HasWord> tokenizedSentence = foundSentences.next();
            List<String> tokenizedSentenceAsListOfStrings = new ArrayList<String>();
            for (HasWord w : tokenizedSentence)
            {
                tokenizedSentenceAsListOfStrings.add(w.word());
            }
            tokenizedSentences.add(tokenizedSentenceAsListOfStrings);
        }

        return tokenizedSentences;
    }

    private List<List<String>> tagTokenizedSentences(List<List<String>> tokenizedSentences)
    {
        List<List<String>> taggedTokenizedSentences = new ArrayList<List<String>>();

        for (List<String> tokenizedSentence : tokenizedSentences)
        {
            String[] tokenArray = new String[tokenizedSentence.size()];
            tokenizedSentence.toArray(tokenArray);
            List<CoreLabel> stanfordFormat = SentenceUtils.toCoreLabelList(tokenArray);
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

        DateFormat dateFormat = new SimpleDateFormat("MM/dd/yyyy HH:mm:ss");

        while (true)
        {
            String request = socket.recvStr(0);
            String info = MessageFormat.format("[{0}] {1} received request of {2} characters.",
                                               dateFormat.format(Calendar.getInstance().getTime()),
                                               Thread.currentThread().getName(),
                                               request.length());
            System.err.println(info);

            try
            {
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
