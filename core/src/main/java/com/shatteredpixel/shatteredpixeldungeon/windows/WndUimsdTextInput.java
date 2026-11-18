package com.shatteredpixel.shatteredpixeldungeon.windows;

import com.shatteredpixel.shatteredpixeldungeon.ShatteredPixelDungeon;

//A UIMSD input keeps its typed value and callback when the scene is rebuilt.
public class WndUimsdTextInput extends WndTextInput {
    public interface Selection { void select(boolean positive, String value); }

    private final String title, body, positiveText, negativeText;
    private final int limit;
    private final boolean multiline;
    private final Selection selection;

    public WndUimsdTextInput(String title, String body, String initial, int limit,
                             boolean multiline, String positive, String negative, Selection selection){
        super(title, body, initial, limit, multiline, positive, negative);
        this.title = title;
        this.body = body;
        this.limit = limit;
        this.multiline = multiline;
        positiveText = positive;
        negativeText = negative;
        this.selection = selection;
    }

    @Override protected int inputWidth(boolean multiline, boolean hasBody){
        return WndUimsdList.windowWidth();
    }

    @Override protected boolean buttonsUseFullWidth(){ return true; }

    @Override public void onSelect(boolean positive, String value){
        if (selection != null) selection.select(positive, value);
    }

    @Override public Runnable restoreOnSceneReset(){
        final String savedTitle = title, savedBody = body, savedText = textBox.getText();
        final String positive = positiveText, negative = negativeText;
        final int savedLimit = limit;
        final boolean savedMultiline = multiline;
        final Selection savedSelection = selection;
        return () -> ShatteredPixelDungeon.scene().addToFront(new WndUimsdTextInput(savedTitle,
                savedBody, savedText, savedLimit, savedMultiline, positive, negative, savedSelection));
    }
}
