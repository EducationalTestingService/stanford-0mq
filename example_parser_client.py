'''
The purpose of this script is to demonstrate the various ways of interacting with
the Stanford Parser via this 0mq server.  Simply create a Stanford0mqClient
object, and there is one method: parse().

parse() takes three arguments:
(1) The text you want parse tree(s) for, in one of the formats demonstrated below.
(2) The Stanford Parser -outputFormat, in a comma-separated list, just like you would
send to the Stanford Parser via command-line.  Specify "" if you're fine with this
server's default (oneline).
(3) The Stanford Parser -outputFormatOptions, in a comma-separated list.  Specify
"" if you don't want to specify any.

See http://nlp.stanford.edu/nlp/javadoc/javanlp/edu/stanford/nlp/parser/lexparser/LexicalizedParser.html#main%28java.lang.String[]%29
for the full list of options to outputFormat and outputFormatOptions.
'''

import sys
sys.path.append("./src")

from stanford_0mq_client_api import Stanford0mqClient


# From the Wikipedia entry "red panda"
test_text = """The red panda is slightly larger than a domestic cat. 
It has reddish-brown fur and a long, shaggy tail and a waddling gait due to its shorter front legs.
It feeds mainly on bamboo, but is 
omnivorous and also eats eggs, birds, insects, and small mammals. It 
is a solitary animal, mainly active from dusk to dawn, and is largely 
sedentary during the day."""

test_tokens = "It feeds mainly on bamboo , but is omnivorous and also eats eggs , birds , insects , and small mammals .".split(" ")

# Your POS-tagged sentence can use either '/' or '_' as the delimiter between token and tag.
test_tagged_sentence = "The/DT red/JJ panda/NN is/VBZ slightly/RB larger/JJR than/IN a/DT domestic/JJ cat/NN ./."


client = Stanford0mqClient("tcp://127.0.0.1:5555") # add verbose=True at the end here for some potentially useful chatter

parse_trees = client.parse(test_text, "penn,wordsAndTags", "xml")
for (i, tree) in enumerate(parse_trees):
    print str(i) + "\n" + tree

print

print client.parse(test_tokens, "", "")

print

print client.parse(test_tagged_sentence, "typedDependencies", "")
