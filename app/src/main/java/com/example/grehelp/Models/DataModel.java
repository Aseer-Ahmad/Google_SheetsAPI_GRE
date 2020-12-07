package com.example.grehelp.Models;

public class DataModel {

    String word;
    String desc;

    public DataModel(String word, String desc) {
        this.word = word;
        this.desc = desc;
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
