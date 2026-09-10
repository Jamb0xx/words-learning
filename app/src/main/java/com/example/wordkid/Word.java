package com.example.wordkid;

public class Word {
    public long id;
    public String en;
    public String ru;
    public boolean custom;

    public Word(long id, String en, String ru, boolean custom) {
        this.id = id;
        this.en = en;
        this.ru = ru;
        this.custom = custom;
    }
}
