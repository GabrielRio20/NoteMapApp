package com.example.notemapapp;

public class Notes {
    String noteID;
    String noteTitle;
    String noteDesc;

    public String getNoteID() {
        return noteID;
    }

    public void setNoteID(String noteID) {
        this.noteID = noteID;
    }

    public String getNoteTitle() {
        return noteTitle;
    }

    public void setNoteTitle(String noteTitle) {
        this.noteTitle = noteTitle;
    }

    public String getNoteDesc() {
        return noteDesc;
    }

    public void setNoteDesc(String noteDesc) {
        this.noteDesc = noteDesc;
    }

    public Notes() {
        this.noteID = noteID;
        this.noteTitle = noteTitle;
        this.noteDesc = noteDesc;
    }
}
