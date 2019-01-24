package org.ets.research.nlp.corenlp.zeromq;

import org.zeromq.ZMQ;
import org.zeromq.ZMQ.Context;
import org.zeromq.ZMQ.Socket;

import edu.stanford.nlp.parser.shiftreduce.ShiftReduceParser;
import edu.stanford.nlp.tagger.maxent.MaxentTagger;

public class Server
{

    private ShiftReduceParser srModel;
    private MaxentTagger tagger;

    public Server(String myBrokerAddress, int threads)
    {

    	if (threads < 1 || threads > 10)
    	{

    		// if the number of threads is less than 1, bound it at 1;
    		// otherwise, we know it's greater than ten, so bound it at 10
    		int boundedThreads = ((threads < 1) ? 1 : 10);

    		// print an error message, and bound the threads at the value of boundedThreads
    		String errorMsg = "The number of threads must be between 1 and 10. You passed "
    	                    + Integer.toString(threads)
    	                    + ", so the number of threads will be bounded at "
    	                    + Integer.toString(boundedThreads)
    	                    + " instead.";

    		System.err.println(errorMsg);
    		threads = boundedThreads;
    	}
 
    	try
        {
            System.err.println("Initializing MaxentTagger...");
            tagger = new MaxentTagger("edu/stanford/nlp/models/pos-tagger/english-left3words/english-left3words-distsim.tagger");

            System.err.println("Initializing Shift-Reduce Parser...");
            srModel = ShiftReduceParser.loadModel("edu/stanford/nlp/models/srparser/englishSR.ser.gz");

            Context context = ZMQ.context(1);

            Socket clients = context.socket(ZMQ.ROUTER);
            clients.bind(myBrokerAddress);

            Socket workers = context.socket(ZMQ.DEALER);
            workers.bind("inproc://workers");

            for (int thread_nbr = 0; thread_nbr < threads; thread_nbr++)
            {
                System.err.println("Starting worker thread " + thread_nbr);
                Thread worker = new Worker(context, srModel, tagger);
                worker.start();
            }

            // Connect work threads to client threads via a queue
            ZMQ.proxy (clients, workers, null);
        }
        catch (Exception e)
        {
            // TODO: Auto-generated catch block
            e.printStackTrace();
        }
    }

    public Server(String myBrokerAddress)
    {
        this(myBrokerAddress, 10);
    }

    public static void main(String[] args)
    {
        // org.ets.research.nlp.corenlp.zeromq.Server <broker address> [thread count]
        // for example:
        // tcp://127.0.0.1:5555
        if (args.length == 2)
        {
                
            // get the number of threads as an integer
            int threads = Integer.parseInt(args[1].trim());

            @SuppressWarnings("unused")
            Server server = new Server(args[0], threads);
        }
        else
        {
            @SuppressWarnings("unused")
            Server server = new Server(args[0]);
        }
    }
}
