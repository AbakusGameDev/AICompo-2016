package com.aicompo.example;

import java.util.ArrayList;

public class RayCast {
	public class Point {
		public final int x, y;
		public Point(int x, int y) {
			this.x = x;
			this.y = y;
		}
	}

	public ArrayList<Point> points;
	private boolean hit;
	private PlotTest plotTest;

	public interface PlotTest {
		boolean test(int x, int y);
	}

	public RayCast(int x0, int y0, int x1, int y1, PlotTest plotTest) {
		this.hit = false;
		this.plotTest = plotTest;

		// Clear previous points
		points = new ArrayList<Point>();

		// Get deltas
		int dx = Math.abs(x1 - x0);
		int dy = Math.abs(y1 - y0);

		// Get line dir
		int sx = x0 < x1 ? 1 : -1;
		int sy = y0 < y1 ? 1 : -1;

		// Get ???
		int a = dx - dy;

		// Perform line plotting
		while(true) {
			// Plot the current pos
			if(!plot(x0, y0)) {
				// Plot test failed, break
				this.hit = true;
				break;
			}

			// Check if we have reached the end
			if(x0 == x1 && y0 == y1) {// || (m_range > 0.0f && (Vector2(x0, y0) - Vector2(p0)).magnitude() > m_range))
				break;
			}	

			// Apply y traversal
			int a2 = a*2;
			if(a2 > -dy) {
				a -= dy;
				x0 += sx;
			}

			// Apply x traversal
			if(a2 < dx) {
				a += dx;
				y0 += sy;
			}
		}
	}

	private boolean plot(int x, int y) {
		// Test plot
		if(plotTest.test(x, y)) {
			return false;
		}

		// Plot the point
		points.add(new Point(x, y));
		return true;
	}

	public boolean hasHit() {
		return hit;
	}
}
