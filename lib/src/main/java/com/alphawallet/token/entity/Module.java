package com.alphawallet.token.entity;

import com.alphawallet.token.tools.TokenDefinition;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by JB on 20/03/2020.
 */
public class Module
{
    public final ContractInfo originToken;
    public final ContractInfo contractInfo;
    public Map<String, String> sequence = new HashMap<>();

    public Module(ContractInfo origin, ContractInfo info)
    {
        originToken = origin;
        contractInfo = info;
    }
}
