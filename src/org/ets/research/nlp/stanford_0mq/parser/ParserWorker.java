package org.ets.research.nlp.stanford_0mq.parser;

import java.io.PrintWriter;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.zeromq.ZMsg;
import org.zeromq.zguide.chapter4.majordomo.mdwrkapi;

import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.ling.HasWord;
import edu.stanford.nlp.ling.Sentence;
import edu.stanford.nlp.parser.lexparser.LexicalizedParser;
import edu.stanford.nlp.pipeline.DefaultPaths;
import edu.stanford.nlp.process.DocumentPreprocessor;
import edu.stanford.nlp.trees.PennTreebankLanguagePack;
import edu.stanford.nlp.trees.Tree;
import edu.stanford.nlp.trees.TreePrint;
import edu.stanford.nlp.trees.TreebankLanguagePack;

public class ParserWorker extends mdwrkapi
{
    private LexicalizedParser parser;
    private TreePrint treePrinter;
    private TreebankLanguagePack tlp;
	
	public ParserWorker(String modelFile, String broker, boolean verbose) 
	{
		super(broker, "ParserWorker", verbose);
		
        if (modelFile.equals("") || modelFile == null) 
        {
        	parser = LexicalizedParser.loadModel(DefaultPaths.DEFAULT_PARSER_MODEL, new String[]{});
        }
        else 
        {
        	parser = LexicalizedParser.loadModel(modelFile, new String[]{});
        }
		
        tlp = new PennTreebankLanguagePack();
        treePrinter = new TreePrint("oneline", "", tlp);
		
		ZMsg reply = null;
		while (true)
		{
			ZMsg request = super.receive(reply);
			String outputFormat = request.popString();
			String outputFormatOptions = request.popString();
			String receivedText = request.popString();
			
			reply = new ZMsg();
			if (request.size() == 0)
			{
				List<String> parseTrees = parseText(receivedText, outputFormat, outputFormatOptions);
				reply.clear();
				for (String tree : parseTrees)
				{
					reply.add(tree);
				}
			}
			else //tokens
			{
				List<String> tokens = new ArrayList<String>();
				tokens.add(receivedText);
				while ((receivedText = request.popString()) != null)
				{
					tokens.add(receivedText);
				}
				reply.clear();
				reply.add(parseSentence(tokens, outputFormat, outputFormatOptions));
			}
		}
	}

	private String parseSentence(List<String> sentenceTokens, String outputFormat,
			String outputFormatOptions) 
	{
        setOptions(outputFormat, outputFormatOptions);
        
        // a single sentence worth of tokens
        String[] tokenArray = new String[sentenceTokens.size()];
        sentenceTokens.toArray(tokenArray);
        List<CoreLabel> crazyStanfordFormat = Sentence.toCoreLabelList(tokenArray);
        Tree parseTree = parser.apply(crazyStanfordFormat);
        return TreeObjectToString(parseTree);//, parseTree.score());
	}

	private List<String> parseText(String text, String outputFormat, String outputFormatOptions) 
	{
        List<String> results = new ArrayList<String>();
        
        setOptions(outputFormat, outputFormatOptions);
        
        // assume no tokenization was done; use Stanford's default tokenizer
        DocumentPreprocessor preprocess = new DocumentPreprocessor(new StringReader(text));
        Iterator<List<HasWord>> foundSentences = preprocess.iterator();
        while (foundSentences.hasNext())
        {
        	Tree parseTree = parser.apply(foundSentences.next());
        	results.add(TreeObjectToString(parseTree));//, parseTree.score()));
        }
        
        return results;
	}
	
	private String TreeObjectToString(Tree tree)
	{
		StringWriter sw = new StringWriter();
		PrintWriter pw = new PrintWriter(sw);
		treePrinter.printTree(tree, pw);
		return sw.getBuffer().toString().trim();
	}
	
	private void setOptions(String outputFormat, String outputFormatOptions)
	{
        if (outputFormat.length() > 0)
        {
        	if (outputFormatOptions.length() > 0)
        	{
        		treePrinter = new TreePrint(outputFormat, outputFormatOptions, tlp);
        	}
        	else
        	{
        		treePrinter = new TreePrint(outputFormat, "", tlp);
        	}
        }
	}
}
