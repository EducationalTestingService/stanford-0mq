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

client = Stanford0mqClient("tcp://127.0.0.1:5555")

outputFormat = "penn,wordsAndTags"
parse_trees = client.parse(test_text, outputFormat, "xml")
for (i, tree) in enumerate(parse_trees):
    print str(i) + "\n" + tree

print

print client.parse(test_tokens, "oneline", "")
