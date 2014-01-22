package org.ets.research.nlp.stanford_0mq;

import java.util.ArrayList;
import java.util.List;

import edu.stanford.nlp.ling.TaggedWord;
import edu.stanford.nlp.ling.TaggedWordFactory;

public class CoreNLP0mqUtil 
{
    public static List<TaggedWord> getListOfTaggedWordsFromTaggedSentence(String taggedSentence, String divider)
    {
        String[] taggedTokens = taggedSentence.split(" ");
        TaggedWordFactory tf = new TaggedWordFactory(divider.charAt(0));
        List<TaggedWord> taggedWordList = new ArrayList<TaggedWord>();
        for (String taggedToken : taggedTokens)
        {
                taggedWordList.add((TaggedWord)tf.newLabelFromString(taggedToken));
        }
        return taggedWordList;
    }
}
