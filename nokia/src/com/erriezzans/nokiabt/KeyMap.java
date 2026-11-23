package com.erriezzans.nokiabt;

import javax.microedition.rms.RecordStore;
import javax.microedition.rms.RecordStoreException;

/** Persistent, user-editable keypad mapping for the twelve gamepad controls. */
final class KeyMap {
    static final int BUTTON_COUNT = 12;
    static final String[] LABELS = {
        "Cross", "Circle", "Square", "Triangle", "L1", "R1",
        "L2", "R2", "Select", "Start", "L3", "R3"
    };
    static final char[] CHOICES = {'0','1','2','3','4','5','6','7','8','9','*','#'};
    private static final String STORE_NAME = "NokiaBTKeyMap";
    private static final char[] DEFAULTS = {'4','3','1','2','7','9','*','#','5','0','6','8'};
    private final int[] keys = new int[BUTTON_COUNT];

    KeyMap() {
        for (int i=0;i<BUTTON_COUNT;i++) keys[i]=DEFAULTS[i];
        load();
    }

    synchronized int getKey(int button) {
        return button>=0 && button<BUTTON_COUNT ? keys[button] : -1;
    }

    synchronized String getKeyLabel(int button) {
        int k=getKey(button);
        return k<0 ? "?" : String.valueOf((char)k);
    }

    synchronized int findButton(char key) {
        for(int i=0;i<BUTTON_COUNT;i++) if(keys[i]==key) return i;
        return -1;
    }

    synchronized int[] getMappings() {
        int[] copy=new int[BUTTON_COUNT];
        for(int i=0;i<BUTTON_COUNT;i++) copy[i]=keys[i];
        return copy;
    }

    synchronized boolean setMappings(int[] values) {
        if(values==null || values.length!=BUTTON_COUNT) return false;
        for(int i=0;i<BUTTON_COUNT;i++) {
            if(!validKey((char)values[i])) return false;
            for(int j=0;j<i;j++) if(values[i]==values[j]) return false;
        }
        for(int i=0;i<BUTTON_COUNT;i++) keys[i]=values[i];
        return save();
    }

    static int choiceIndex(char key) {
        for(int i=0;i<CHOICES.length;i++) if(CHOICES[i]==key) return i;
        return 0;
    }

    private static boolean validKey(char c) {
        for(int i=0;i<CHOICES.length;i++) if(CHOICES[i]==c) return true;
        return false;
    }

    private void load() {
        RecordStore store=null;
        try {
            store=RecordStore.openRecordStore(STORE_NAME,true);
            if(store.getNumRecords()>0) {
                byte[] data=store.getRecord(1);
                if(data!=null && data.length==BUTTON_COUNT) {
                    boolean valid=true;
                    for(int i=0;i<BUTTON_COUNT;i++) {
                        char c=(char)(data[i]&0xff);
                        if(!validKey(c)) valid=false;
                        for(int j=0;j<i;j++) if(data[i]==data[j]) valid=false;
                    }
                    if(valid) for(int i=0;i<BUTTON_COUNT;i++) keys[i]=data[i]&0xff;
                }
            }
        } catch(Exception ignored) {
            // Defaults remain active if this phone has no usable RMS store.
        } finally {
            if(store!=null) try{store.closeRecordStore();}catch(Exception ignored){}
        }
    }

    private boolean save() {
        RecordStore store=null;
        try {
            byte[] data=new byte[BUTTON_COUNT];
            for(int i=0;i<BUTTON_COUNT;i++) data[i]=(byte)keys[i];
            store=RecordStore.openRecordStore(STORE_NAME,true);
            if(store.getNumRecords()==0) store.addRecord(data,0,data.length);
            else store.setRecord(1,data,0,data.length);
            return true;
        } catch(RecordStoreException e) {
            return false;
        } finally {
            if(store!=null) try{store.closeRecordStore();}catch(Exception ignored){}
        }
    }
}
