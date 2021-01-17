package com.example.grehelp.Models;

public class DataModel {

    String word;
    String desc;
    String syn;


    public DataModel(String word, String desc, String syn) {
        this.word = word;
        this.desc = desc;
        this.syn = syn;
    }

    public String getSyn() {
        return syn;
    }

    public void setSyn(String syn) {
        this.syn = syn;
    }


    public String getWord() {
        return word;
    }

    public void setWord(String word) {
        this.word = word;
    }

    public String getDesc() {
        return desc;
    }

    public void setDesc(String desc) {
        this.desc = desc;
    }
}
