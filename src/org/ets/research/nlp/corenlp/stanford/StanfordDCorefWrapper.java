package org.ets.research.nlp.corenlp.stanford;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import edu.stanford.nlp.dcoref.CorefChain;
import edu.stanford.nlp.dcoref.CorefCoreAnnotations;
import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.util.CoreMap;
import edu.stanford.nlp.util.Pair;

public class StanfordDCorefWrapper
{
    private StanfordCoreNLP dcoref;

    public StanfordDCorefWrapper()
    {
        Properties props = new Properties();
        props.put("annotators", "dcoref");
        this.dcoref = new StanfordCoreNLP(props, false);
    }

    public List<String> getCoreferencesFromAnnotation(Annotation annotation)
    {
        dcoref.annotate(annotation);
        return MUCStyleOutput(annotation);
    }

    private List<String> MUCStyleOutput(Annotation annotation)
    {
        Map<Integer, CorefChain> corefChains = annotation.get(CorefCoreAnnotations.CorefChainAnnotation.class);
        Map<Integer, Map<Integer, Pair<CorefChain.CorefMention, CorefChain.CorefMention>>> mentionMap =
                new HashMap<Integer, Map<Integer, Pair<CorefChain.CorefMention, CorefChain.CorefMention>>>();

        List<String> mucOutput = new ArrayList<String>();

        for (CorefChain chain : corefChains.values())
        {
            CorefChain.CorefMention ref = chain.getRepresentativeMention();

            for (CorefChain.CorefMention mention : chain.getMentionsInTextualOrder())
            {
                if (mention != ref)
                {
                    // first add the mention itself
                    Pair<CorefChain.CorefMention,CorefChain.CorefMention> mentions =
                            new Pair<CorefChain.CorefMention, CorefChain.CorefMention>(mention, ref);
                    if (mentionMap.containsKey(mention.sentNum))
                    {
                        Map<Integer, Pair<CorefChain.CorefMention, CorefChain.CorefMention>> value =
                                mentionMap.get(mention.sentNum);
                        value.put(mention.startIndex, mentions);
                        mentionMap.put(mention.sentNum, value);
                    }
                    else
                    {
                        Map<Integer, Pair<CorefChain.CorefMention, CorefChain.CorefMention>> startIndexToMentionMap =
                                new HashMap<Integer, Pair<CorefChain.CorefMention, CorefChain.CorefMention>>();
                        startIndexToMentionMap.put(mention.startIndex, mentions);
                        mentionMap.put(mention.sentNum, startIndexToMentionMap);
                    }

                    // now make sure the representative is there (TODO make this code less redundant)
                    Pair<CorefChain.CorefMention,CorefChain.CorefMention> refMention =
                            new Pair<CorefChain.CorefMention, CorefChain.CorefMention>(ref, ref);
                    if (mentionMap.containsKey(ref.sentNum))
                    {
                        Map<Integer, Pair<CorefChain.CorefMention, CorefChain.CorefMention>> value =
                                mentionMap.get(ref.sentNum);
                        value.put(ref.startIndex, refMention);
                        mentionMap.put(ref.sentNum, value);
                    }
                    else
                    {
                        Map<Integer, Pair<CorefChain.CorefMention, CorefChain.CorefMention>> startIndexToMentionMap =
                                new HashMap<Integer, Pair<CorefChain.CorefMention, CorefChain.CorefMention>>();
                        startIndexToMentionMap.put(ref.startIndex, refMention);
                        mentionMap.put(ref.sentNum, startIndexToMentionMap);
                    }
                }
            }
        }


        List<CoreMap> sentences = annotation.get(CoreAnnotations.SentencesAnnotation.class);
        for (Integer sentenceNum : mentionMap.keySet())
        {
            CoreMap currentSentence = sentences.get(sentenceNum-1);
            Map<Integer, Pair<CorefChain.CorefMention, CorefChain.CorefMention>> currentSetOfMentions =
                    mentionMap.get(sentenceNum);
            CorefChain.CorefMention lastMention = null;
            String outputString = "";
            for (CoreLabel token : currentSentence.get(CoreAnnotations.TokensAnnotation.class))
            {
                if (currentSetOfMentions.containsKey(token.index()))
                {
                    lastMention = currentSetOfMentions.get(token.index()).first();
                    CorefChain.CorefMention ref = currentSetOfMentions.get(token.index()).second();
                    outputString += "<COREF ID=\"" + lastMention.mentionID + "\"";
                    if (lastMention.mentionID != ref.mentionID)
                    {
                        outputString += " REF=\"" + ref.mentionID + "\"";
                    }
                    outputString += ">";
                }
                if (lastMention != null && token.index() == lastMention.endIndex)
                {
                    outputString += "</COREF> ";
                }
                outputString += token.word() + " ";
            }
            mucOutput.add(CoreNLPWrapperUtil.closeHTMLTags(outputString.replaceAll(" </", "</")));
        }

        return mucOutput;
    }
}
