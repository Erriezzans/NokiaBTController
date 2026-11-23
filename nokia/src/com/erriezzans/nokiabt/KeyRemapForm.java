package com.erriezzans.nokiabt;

import javax.microedition.lcdui.Choice;
import javax.microedition.lcdui.ChoiceGroup;
import javax.microedition.lcdui.Command;
import javax.microedition.lcdui.CommandListener;
import javax.microedition.lcdui.Displayable;
import javax.microedition.lcdui.Form;

/** Scrolling MIDP form for remapping all gamepad controls to physical keypad keys. */
final class KeyRemapForm extends Form implements CommandListener {
    private final NokiaBTController app;
    private final ChoiceGroup[] choices = new ChoiceGroup[KeyMap.BUTTON_COUNT];
    private final Command saveCommand = new Command("Save",Command.OK,1);
    private final Command cancelCommand = new Command("Cancel",Command.CANCEL,2);
    private static final String[] KEY_NAMES = {"0","1","2","3","4","5","6","7","8","9","*","#"};

    KeyRemapForm(NokiaBTController owner,int[] current) {
        super("Gamepad key remap");
        app=owner;
        append("Select a different Nokia keypad key for each controller function. Each key can only be assigned once.");
        for(int i=0;i<KeyMap.BUTTON_COUNT;i++) {
            choices[i]=new ChoiceGroup(KeyMap.LABELS[i],Choice.POPUP,KEY_NAMES,null);
            int key=current[i];
            choices[i].setSelectedIndex(KeyMap.choiceIndex((char)key),true);
            append(choices[i]);
        }
        addCommand(saveCommand);
        addCommand(cancelCommand);
        setCommandListener(this);
    }

    public void commandAction(Command command,Displayable displayable) {
        if(command==cancelCommand) {
            app.showMainScreen();
            return;
        }
        if(command!=saveCommand) return;
        int[] values=new int[KeyMap.BUTTON_COUNT];
        for(int i=0;i<KeyMap.BUTTON_COUNT;i++) {
            values[i]=KeyMap.CHOICES[choices[i].getSelectedIndex()];
            for(int j=0;j<i;j++) {
                if(values[i]==values[j]) {
                    app.setStatus("Key conflict: "+KeyMap.LABELS[i]+" and "+KeyMap.LABELS[j]+" use the same key.");
                    return;
                }
            }
        }
        app.saveKeyMappings(values);
    }
}
