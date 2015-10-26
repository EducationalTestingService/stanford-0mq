package org.ets.research.nlp.corenlp.stanford;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Stack;

import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.NERCombinerAnnotator;
import edu.stanford.nlp.util.CoreMap;

public class StanfordNERWrapper
{
    /*
     * TODO: When I add in the bit for the Stanford Tagger, add a method here that recognizes
     * named entities from POS-tagged text, similar to the one that recognizes them from parse
     * trees.
     */

    private NERCombinerAnnotator ner;

    public StanfordNERWrapper(List<String> nerModels) throws IOException, ClassNotFoundException
    {
        String[] models = new String[nerModels.size()];
        nerModels.toArray(models);
        ner = new NERCombinerAnnotator(false, models);
    }

    public Annotation annotateForNamedEntities(Annotation annotation)
    {
        Annotation withNE = annotation.copy();
        ner.annotate(withNE);
        return withNE;
    }

    public List<Map<String, Object>> getNamedEntitiesFromTrees(List<String> parseTrees)
    {
        Annotation annotation = CoreNLPWrapperUtil.getAnnotationFromParseTrees(parseTrees);
        ner.annotate(annotation);
        List<CoreMap> sentenceMap = annotation.get(CoreAnnotations.SentencesAnnotation.class);
        return toNamedEntityMaps(sentenceMap);
    }

    private List<Map<String, Object>> toNamedEntityMaps(List<CoreMap> results)
    {
        List<Map<String, Object>> entities = new ArrayList<Map<String, Object>>();

        Stack<CoreLabel> namedEntityStack = new Stack<CoreLabel>();
        int sentenceNum = 1;
        for (CoreMap sentence : results)
        {
            List<CoreLabel> tokens = sentence.get(CoreAnnotations.TokensAnnotation.class);
            for (Iterator<CoreLabel> wordIter = tokens.iterator(); wordIter.hasNext();)
            {
                CoreLabel wi = wordIter.next();
                if (namedEntityStack.empty() || wi.ner().equals(namedEntityStack.peek().ner()))
                {
                    namedEntityStack.push(wi);
                }
                else
                {
                    String tag = "";
                    String entity = "";
                    int startIndex = namedEntityStack.peek().beginPosition();
                    int endIndex = namedEntityStack.peek().endPosition();
                    while (!namedEntityStack.empty())
                    {
                        CoreLabel popped = namedEntityStack.pop();
                        tag = popped.ner();
                        entity = popped.word() + " " + entity;
                        if (popped.beginPosition() < startIndex)
                        {
                            startIndex = popped.beginPosition();
                        }
                    }
                    if (!tag.equals("O"))
                    {
                        Map<String, Object> theseEntities = new HashMap<String, Object>();
                        theseEntities.put("start_offset", startIndex);
                        theseEntities.put("end_offset", endIndex);
                        theseEntities.put("entity_text", entity.trim());
                        theseEntities.put("entity_tag", tag);
                        theseEntities.put("sentence_num", sentenceNum);
                        entities.add(theseEntities);
                    }
                    namedEntityStack.push(wi);
                }
            }
            sentenceNum++;
        }

        return entities;
    }
}
