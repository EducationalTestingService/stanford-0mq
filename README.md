stanford-0mq
============

An implementation of a server for Stanford's CoreNLP suite using 0mq and the majordomo protocol.

[![Bitdeli Badge](https://d2weczhvl823v0.cloudfront.net/EducationalTestingService/stanford-0mq/trend.png)](https://bitdeli.com/free "Bitdeli Badge")

This is very much a **work in progress**, however you're welcome to join the party and/or use what's here so far.

## Requirements ##

* The latest version of [Stanford CoreNLP](http://nlp.stanford.edu/software/corenlp.shtml).  You'll also want to download the [Stanford Parser](http://nlp.stanford.edu/software/lex-parser.shtml) if you want to use any of their models besides the default one.  Right now this will probably only work with English models.
* The latest version of [jzmq](http://zeromq.org/bindings:java) along with whatever version of ZeroMQ it supports.  You'll need to install ZeroMQ first.
* (**optional** but do this first) For the Python client, do a `pip install zeromq` (or `conda install zeromq`).  In addition to the Python bindings, this will also provide ZeroMQ, which you'll need for the Java bindings.

## How This Works ##

For the most part, this project uses the majordomo code as provided by the [zguide](https://github.com/imatix/zguide).  The broker provided here (`mdbroker`) is completely unmodified from the example code, so you're welcome to swap in your own.  The same goes for `mdwrkapi`, although each of the workers provided here is a subclass of that.  There are/will be one worker per each Stanford CoreNLP tool.

At the moment, only a Python client API is provided, which is a subclass of (again unmodified) `MajorDomoClient` as provided by the zguide's `mdcliapi2` module.  See the `example_[a-z]+_client.py` files for examples of how to use the Python client.

## How to Use This ##

1. Start a broker; I recommend `mdbroker` as provided here in the `src` directory.  (`jar` file coming soon.)
2. Run `org.ets.research.nlp.stanford_0mq.Start` to start the workers.  Right now this just starts a single `ParserWorker`, but the plan is for this to read a configuration file and start any number of workers of any type (although, while you could start 12 NER's without starting any Parsers or Taggers, NER (and pretty much everything else) needs one of those, so the Parser will be started anyway).
3. See the `example_[a-z]+_client.py` files for examples of how to interact with this with Python.  With Java, it will be identical.
