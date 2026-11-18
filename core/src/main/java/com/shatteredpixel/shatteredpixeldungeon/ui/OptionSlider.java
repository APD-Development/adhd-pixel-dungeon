/*
 * Pixel Dungeon
 * Copyright (C) 2012-2015 Oleg Dolya
 *
 * Shattered Pixel Dungeon
 * Copyright (C) 2014-2026 Evan Debenham
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>
 */

package com.shatteredpixel.shatteredpixeldungeon.ui;

import com.shatteredpixel.shatteredpixeldungeon.Chrome;
import com.shatteredpixel.shatteredpixeldungeon.scenes.PixelScene;
import com.watabou.input.PointerEvent;
import com.watabou.noosa.ColorBlock;
import com.watabou.noosa.NinePatch;
import com.watabou.noosa.PointerArea;
import com.watabou.noosa.ui.Component;
import com.watabou.utils.GameMath;
import com.watabou.utils.PointF;

public abstract class OptionSlider extends Component {

	private PointerArea pointerArea;
    private ScrollPane verticalPane;
    /** Opt in only for custom-rules sliders; existing SPD sliders keep their original behavior. */
    public void scrollVerticallyWith(ScrollPane pane) {verticalPane=pane;}

	private RenderedTextBlock title;
	private RenderedTextBlock minTxt;
	private RenderedTextBlock maxTxt;

	//values are expressed internally as ints, but they can easily be interpreted as something else externally.
	private int minVal;
	private int maxVal;
	private int selectedVal;

	private NinePatch sliderNode;
	private NinePatch BG;
	private ColorBlock sliderBG;
	private ColorBlock[] sliderTicks;
	private float tickDist;


	public OptionSlider(String title, String minTxt, String maxTxt, int minVal, int maxVal){
		super();

		//shouldn't function if this happens.
		if (minVal > maxVal){
			minVal = maxVal;
			active = false;
		}

		this.title.text(title);
		this.minTxt.text(minTxt);
		this.maxTxt.text(maxTxt);

		this.minVal = minVal;
		this.maxVal = maxVal;

		sliderTicks = new ColorBlock[(maxVal - minVal) + 1];
		for (int i = 0; i < sliderTicks.length; i++){
			add(sliderTicks[i] = new ColorBlock(1, 9, 0xFF222222));
		}
		add(sliderNode);
	}

	protected abstract void onChange();

	public int getSelectedValue(){
		return selectedVal;
	}

	public void setSelectedValue(int val) {
		this.selectedVal = val;
		sliderNode.x = (int)(x + tickDist*(selectedVal-minVal)) + 0.5f;
		sliderNode.y = sliderBG.y-4;
		PixelScene.align(sliderNode);
	}

	public void enable( boolean value ) {
		active = value;
		title.alpha( value ? 1.0f : 0.3f );
		minTxt.alpha( value ? 1.0f : 0.3f );
		maxTxt.alpha( value ? 1.0f : 0.3f );
		sliderNode.alpha( value ? 1.0f : 0.3f );
	}

    private boolean liveChanges;
    /** Opt in only for draft controls; existing sliders retain release-only notifications. */
    public void liveChanges(boolean enabled){liveChanges=enabled;}
    private void notifyDragValue(){
        int next=Math.max(minVal,Math.min(minVal+Math.round((sliderNode.x-x)/tickDist),maxVal));
        if(next!=selectedVal){selectedVal=next;onChange();}
    }

	@Override
	protected void createChildren() {
		super.createChildren();

		add( BG = Chrome.get(Chrome.Type.RED_BUTTON));
		BG.alpha(0.5f);

		add(title = PixelScene.renderTextBlock(9));
		add(this.minTxt = PixelScene.renderTextBlock(6));
		add(this.maxTxt = PixelScene.renderTextBlock(6));

		add(sliderBG = new ColorBlock(1, 1, 0xFF222222));
		sliderNode = Chrome.get(Chrome.Type.RED_BUTTON);
		sliderNode.size(4, 7);

		pointerArea = new PointerArea(0, 0, 0, 0){
			boolean pressed = false;
            int pressedValue;
            SliderGesture gesture;
            float lastY;
            private void position(PointerEvent event) {
                PointF p=camera().screenToCamera((int)event.current.x,(int)event.current.y);
                sliderNode.x=GameMath.gate(sliderBG.x-2,p.x-sliderNode.width()/2,sliderBG.x+sliderBG.width()-2);
            }
            private void vertical(PointerEvent event) {
                float delta=(lastY-event.current.y)/verticalPane.content().camera().zoom;
                verticalPane.scrollTo(verticalPane.content().camera().scroll.x,verticalPane.content().camera().scroll.y+delta);
                lastY=event.current.y;
            }

			@Override
			protected void onPointerDown( PointerEvent event ) {
				if(event!=curEvent || pressed)return;
                pressed = true;pressedValue=selectedVal;
                if(verticalPane!=null){
                    gesture=new SliderGesture(event.current.x,event.current.y,camera().zoom);
                    lastY=event.current.y;sliderNode.brightness(1.5f);return;
                }
                PointF p = camera().screenToCamera((int) event.current.x, (int) event.current.y);
				sliderNode.x = GameMath.gate(sliderBG.x-2, p.x - sliderNode.width()/2, sliderBG.x+sliderBG.width()-2);
				sliderNode.brightness(1.5f);
			}

			@Override
			protected void onPointerUp( PointerEvent event ) {
				if (pressed && event==curEvent) {
                    if(event.type==PointerEvent.Type.CANCEL){
                        boolean restore=liveChanges&&selectedVal!=pressedValue;
                        setSelectedValue(pressedValue);sliderNode.resetColor();pressed=false;
                        if(restore)onChange();
                        return;
                    }
                    if(verticalPane!=null){
                        SliderGesture.Direction direction=gesture.finish(event.current.x,event.current.y);
                        if(direction==SliderGesture.Direction.VERTICAL){
                            vertical(event);setSelectedValue(selectedVal);sliderNode.resetColor();pressed=false;return;
                        }
                    }
                    PointF p = camera().screenToCamera((int) event.current.x, (int) event.current.y);
					sliderNode.x = GameMath.gate(sliderBG.x - 2, p.x - sliderNode.width()/2, sliderBG.x + sliderBG.width() - 2);
					sliderNode.resetColor();
					
					//sets the selected value
					selectedVal = minVal + Math.round((sliderNode.x - x) / tickDist);
					sliderNode.x = x + tickDist * (selectedVal - minVal) + 0.5f;
					PixelScene.align(sliderNode);
					onChange();
					pressed = false;
				}
			}

			@Override
			protected void onDrag( PointerEvent event ) {
				if (pressed) {
                    if(verticalPane!=null){
                        SliderGesture.Direction direction=gesture.move(event.current.x,event.current.y);
                        if(direction==SliderGesture.Direction.VERTICAL)vertical(event);
                        else if(direction==SliderGesture.Direction.HORIZONTAL){position(event);if(liveChanges)notifyDragValue();}
                        return;
                    }
                    PointF p = camera().screenToCamera((int) event.current.x, (int) event.current.y);
					sliderNode.x = GameMath.gate(sliderBG.x - 2, p.x - sliderNode.width()/2, sliderBG.x + sliderBG.width() - 2);
				}
			}
		};
		add(pointerArea);

	}

	@Override
	protected void layout() {

		if (title.width() > 0.6f*width){
			String titleText = title.text;
			remove(title);
			title = PixelScene.renderTextBlock(6);
			add(title);
			title.text(titleText);
		}

		title.setPos(
				x + (width-title.width())/2,
				y+2
		);
		PixelScene.align(title);
		sliderBG.y = y + height() - 7;
		sliderBG.x = x+2;
		sliderBG.size(width-5, 1);
		tickDist = sliderBG.width()/(maxVal - minVal);
		for (int i = 0; i < sliderTicks.length; i++){
			sliderTicks[i].y = sliderBG.y-4;
			sliderTicks[i].x = x + 2 + (tickDist*i);
			PixelScene.align(sliderTicks[i]);
		}

		minTxt.setPos(
				x+1,
				sliderBG.y-5-minTxt.height()
		);
		maxTxt.setPos(
				x+width()-maxTxt.width()-1,
				sliderBG.y-5-minTxt.height()
		);

		sliderNode.x = x + tickDist*(selectedVal-minVal) + 0.5f;
		sliderNode.y = sliderBG.y-3;
		PixelScene.align(sliderNode);

		pointerArea.x = x;
		pointerArea.y = y;
		pointerArea.width = width();
		pointerArea.height = height();

		BG.size(width(), height());
		BG.x = x;
		BG.y = y;

	}
}

