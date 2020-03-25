package com.alphawallet.token.entity;

import com.alphawallet.token.tools.TokenDefinition;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by JB on 20/03/2020.
 */
public class Module
{
    public final ContractInfo originToken;
    public final ContractInfo contractInfo;
    public List<SequenceElement> sequence = new ArrayList<>();

    public Module(ContractInfo origin, ContractInfo info)
    {
        originToken = origin;
        contractInfo = info;
    }

    public void addSequenceElement(String name, String type, String indexed)
    {
        SequenceElement se = new SequenceElement();
        se.indexed = indexed != null && indexed.equalsIgnoreCase("true");
        se.type = type;
        se.name = name;
        sequence.add(se);
    }

    public List<SequenceElement> getSequenceArgs()
    {
        return sequence;
    }

    public List<String> getArgNames(boolean indexed)
    {
        List<String> argNameIndexedList = new ArrayList<>();
        for (SequenceElement se : sequence)
        {
            if (se.indexed == indexed)
            {
                argNameIndexedList.add(se.name);
            }
        }

        return argNameIndexedList;
    }

    int getTopicIndex(String filterTopic)
    {
        int topicIndex = -1;
        int currentIndex = 0;
        for (SequenceElement se : sequence)
        {
            if (se.indexed)
            {
                if (se.name.equals(filterTopic))
                {
                    topicIndex = currentIndex;
                    break;
                }
                else
                {
                    currentIndex++;
                }
            }
        }

        return topicIndex;
    }


    public class SequenceElement
    {
        public String name;
        public String type;
        public boolean indexed;
    }
}
