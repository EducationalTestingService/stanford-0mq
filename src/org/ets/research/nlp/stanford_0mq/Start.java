package org.ets.research.nlp.stanford_0mq;

import org.ets.research.nlp.stanford_0mq.parser.ParserWorker;

/**
 * Starts the Majordomo broker and the worker(s) as specified in the config file.
 * @author diane
 *
 */
public class Start 
{

	/**
	 * @param args
	 */
	public static void main(String[] args) 
	{
		ParserWorker parser = new ParserWorker("", "tcp://127.0.0.1:5555", true);
	}
}
