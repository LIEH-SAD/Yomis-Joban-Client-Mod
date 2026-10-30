package com.jsblock.amberui.gui.widget;

/**
 * Anything that can be placed, sized and shown/hidden by the list layout, be it a single widget
 * or a group of widgets ({@link WidgetSet}).
 */
public interface WidgetHost {
	int getX();

	int getY();

	int getWidth();

	int getHeight();

	void setX(int x);

	void setY(int y);

	void setWidth(int width);

	void setVisible(boolean visible);

	boolean getActive();

	void setAllX(int x);

	void setAllY(int y);
}
