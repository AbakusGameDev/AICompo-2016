package com.aicompo.ai;

import com.badlogic.gdx.math.Vector2;

public class LineOfSight {
	public boolean check(float x0, float y0, float x1, float y1) {
	    x0 /= Map.TILE_SIZEF; y0 /= Map.TILE_SIZEF;
	    x1 /= Map.TILE_SIZEF; y1 /= Map.TILE_SIZEF;
		
		float dx = Math.abs(x1 - x0);
	    float dy = Math.abs(y1 - y0);

	    int x = (int) Math.floor(x0);
	    int y = (int) Math.floor(y0);

	    float dt_dx = 1.0f / dx;
	    float dt_dy = 1.0f / dy;

	    float t = 0;

	    int n = 1;
	    int x_inc, y_inc;
	    float t_next_vertical, t_next_horizontal;

	    if (dx == 0) {
	        x_inc = 0;
	        t_next_horizontal = dt_dx;
	    } else if (x1 > x0) {
	        x_inc = 1;
	        n += (int) Math.floor(x1) - x;
	        t_next_horizontal = ((float) Math.floor(x0) + 1 - x0) * dt_dx;
	    } else {
	        x_inc = -1;
	        n += x - (int) Math.floor(x1);
	        t_next_horizontal = (x0 - (float) Math.floor(x0)) * dt_dx;
	    }

	    if (dy == 0) {
	        y_inc = 0;
	        t_next_vertical = dt_dy;
	    } else if (y1 > y0) {
	        y_inc = 1;
	        n += (int) Math.floor(y1) - y;
	        t_next_vertical = ((float) Math.floor(y0) + 1 - y0) * dt_dy;
	    } else {
	        y_inc = -1;
	        n += y - (int) Math.floor(y1);
	        t_next_vertical = (y0 - (float) Math.floor(y0)) * dt_dy;
	    }

	    for (; n > 0; --n) {
	        if (Map.isTile(x, y)) {
	        	return false;
	        }

	        if (t_next_vertical < t_next_horizontal) {
	            y += y_inc;
	            t = t_next_vertical;
	            t_next_vertical += dt_dy;
	        } else {
	            x += x_inc;
	            t = t_next_horizontal;
	            t_next_horizontal += dt_dx;
	        }
	    }
	    
	    return true;
	}
	
	public boolean check(Vector2 p0, Vector2 p1) {
		return check(p0.x, p0.y, p1.x, p1.y);
	}
}
