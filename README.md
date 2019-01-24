Assuming you have the shift-reduce parser's model in a directory called `$NLPTools/stanford-parser` and that the built JAR is in a subdirectory called `target/`, the server can be started with:

```
java -cp $NLPTools/stanford-parser/stanford-srparser-2014-10-23-models.jar:target/stanford-sr-parser-zeromq-server-0.0.3-SNAPSHOT.jar org.ets.research.nlp.corenlp.zeromq.Server tcp://$(hostname):5555
```

If you could like to specify the number of threads (default: 10), you can do so as follows:

```
java -cp $NLPTools/stanford-parser/stanford-srparser-2014-10-23-models.jar:target/stanford-sr-parser-zeromq-server-0.0.3-SNAPSHOT.jar org.ets.research.nlp.corenlp.zeromq.Server tcp://$(hostname):5555 10
```

All other JARs needed are contained within the stanford-sr-parser-zeromq-server JAR.

Build with Maven with:
`mvn clean package`