package org.ets.research.nlp.corenlp.stanford;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import edu.stanford.nlp.ling.HasWord;
import edu.stanford.nlp.parser.lexparser.LexicalizedParser;
import edu.stanford.nlp.pipeline.DefaultPaths;
import edu.stanford.nlp.process.DocumentPreprocessor;
import edu.stanford.nlp.trees.PennTreebankLanguagePack;
import edu.stanford.nlp.trees.Tree;
import edu.stanford.nlp.trees.TreePrint;
import edu.stanford.nlp.trees.TreebankLanguagePack;

public class StanfordParserWrapper
{
    private LexicalizedParser parser;
    private TreePrint treePrinter;
    private TreebankLanguagePack tlp;

    public StanfordParserWrapper(String modelFile)
    {
        loadModel(modelFile);
        tlp = new PennTreebankLanguagePack();
    }

    private void loadModel(String modelFile)
    {
        if (modelFile.equals("") || modelFile == null) {
            parser = LexicalizedParser.loadModel(DefaultPaths.DEFAULT_PARSER_MODEL, new String[]{});
        }
        else {
            parser = LexicalizedParser.loadModel(modelFile, new String[]{});
        }
    }

    public List<List<Object>> parseText(String text, List<String> outputFormat) throws Exception
    {
        List<List<Object>> results = new ArrayList<List<Object>>();

        try
        {
            treePrinter = ParserUtil.setOptions(outputFormat, tlp);

            // assume no tokenization was done; use Stanford's default org.ets.research.nlp.stanford_thrift.tokenizer
            DocumentPreprocessor preprocess = new DocumentPreprocessor(new StringReader(text));
            Iterator<List<HasWord>> foundSentences = preprocess.iterator();
            while (foundSentences.hasNext())
            {
                Tree parseTree = parser.apply(foundSentences.next());
                List<Object> parserResults = new ArrayList<Object>();
                parserResults.add(ParserUtil.TreeObjectToString(parseTree, treePrinter));
                parserResults.add(parseTree.score());
                results.add(parserResults);
            }
        }
        catch (Exception e)
        {
            // FIXME
            throw new Exception(e.getMessage());
        }

        return results;
    }
}
