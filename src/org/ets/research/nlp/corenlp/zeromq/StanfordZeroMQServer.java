package org.ets.research.nlp.corenlp.zeromq;

import java.util.Arrays;
import java.util.List;

import org.ets.research.nlp.corenlp.stanford.StanfordDCorefWrapper;
import org.ets.research.nlp.corenlp.stanford.StanfordNERWrapper;
import org.ets.research.nlp.corenlp.stanford.StanfordParserWrapper;
import org.zeromq.ZMQ;
import org.zeromq.ZMQ.Context;
import org.zeromq.ZMQ.Socket;

public class StanfordZeroMQServer
{
    // TODO: config file
    private final int THREADS = 10;

    private StanfordDCorefWrapper dcoref;
    private StanfordParserWrapper englishPCFGParser;
    private StanfordNERWrapper ner;

    public StanfordZeroMQServer(String myBrokerAddress)
    {
        try
        {
            String[] nerModelPaths = {"edu/stanford/nlp/models/ner/english.conll.4class.distsim.crf.ser.gz",
                    "edu/stanford/nlp/models/ner/english.muc.7class.distsim.crf.ser.gz",
            "edu/stanford/nlp/models/ner/english.all.3class.distsim.crf.ser.gz"};
            List<String> nerModels = Arrays.asList(nerModelPaths);

            // TODO: load all the models in the server and pass them in through the constructor
            System.err.println("Initializing English PCFG Parser...");
            englishPCFGParser = new StanfordParserWrapper("edu/stanford/nlp/models/lexparser/englishPCFG.ser.gz");

            System.err.println("Initializing Named Entity Recognizer...");
            ner = new StanfordNERWrapper(nerModels);

            System.err.println("Initializing Coreference Resolver...");
            dcoref = new StanfordDCorefWrapper();

            Context context = ZMQ.context(1);

            Socket clients = context.socket(ZMQ.ROUTER);
            clients.bind(myBrokerAddress);

            Socket workers = context.socket(ZMQ.DEALER);
            workers.bind("inproc://workers");

            for (int thread_nbr = 0; thread_nbr < this.THREADS; thread_nbr++)
            {
                System.err.println("Starting worker thread " + thread_nbr);
                Thread worker = new StanfordZeroMQWorkerThread(context, englishPCFGParser, ner, dcoref);
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

    public static void main(String[] args)
    {
        // org.ets.research.nlp.corenlp.zeromq.StanfordZeroMQServer
        // <broker address>
        // for example:
        // tcp://127.0.0.1:5555
        @SuppressWarnings("unused")
        StanfordZeroMQServer server = new StanfordZeroMQServer(args[0]);
    }

}
