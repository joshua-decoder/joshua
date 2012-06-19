package edu.jhu.thrax.util;

import java.util.HashMap;

public class Vocabulary {

    private static HashMap<String,Integer> word2id = new HashMap<String,Integer>();
    private static HashMap<Integer,String> id2word = new HashMap<Integer,String>();

    private static int size = 0;

    private static boolean fixed = false;

    public static final String OOV = "OOV";

    public synchronized static int getId(String word)
    {
        if (word2id.containsKey(word)) {
            return word2id.get(word);
        }
        else if (fixed) {
            return -1;
        }
        else {
            word2id.put(word, size);
            id2word.put(size, word);
        }
        int ret = size;
        size++;
        return ret;
    }

    public synchronized static String getWord(int id)
    {
        if (id2word.containsKey(id)) {
            return id2word.get(id);
        }
        else {
            return OOV;
        }
    }

    public static void fix()
    {
        fixed = true;
        return;
    }

    public static int [] getIds(String [] words)
    {
        int [] ret = new int[words.length];
        for (int i = 0; i < words.length; i++) {
            ret[i] = getId(words[i]);
        }
        return ret;
    }

    public static String [] getWords(int [] ids)
    {
        String [] ret = new String[ids.length];
        for (int i = 0; i < ids.length; i++)
            ret[i] = getWord(ids[i]);
        return ret;
    }

}
