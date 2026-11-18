package com.shatteredpixel.shatteredpixeldungeon.ui;

/** Direction lock for a slider in a vertical pane; no value or persistence logic. */
public final class SliderGesture {
    public enum Direction { PENDING, HORIZONTAL, VERTICAL }
    private final float startX,startY,dragSlop,tapSlop;
    private Direction direction=Direction.PENDING;
    public SliderGesture(float x,float y,float zoom) {
        startX=x;startY=y;dragSlop=4*zoom;tapSlop=zoom;
    }
    public Direction move(float x,float y) {return classify(x,y,dragSlop);}
    public Direction finish(float x,float y) {return classify(x,y,tapSlop);}
    private Direction classify(float x,float y,float slop) {
        if(direction==Direction.PENDING){
            float dx=Math.abs(x-startX),dy=Math.abs(y-startY);
            if(Math.max(dx,dy)>slop)direction=dx>dy?Direction.HORIZONTAL:Direction.VERTICAL;
        }
        return direction;
    }
}
