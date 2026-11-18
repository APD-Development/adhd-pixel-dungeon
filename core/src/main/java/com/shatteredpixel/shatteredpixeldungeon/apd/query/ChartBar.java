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


package com.shatteredpixel.shatteredpixeldungeon.apd.query;

/** Zero-based signed bar geometry. Fractions share one scale; unknown values have no bar. */
public final class ChartBar {
    public final double start, end, zero;
    private ChartBar(double start, double end, double zero){
        this.start=start; this.end=end; this.zero=zero;
    }
    public static ChartBar of(double min, double max, double value){
        if (!Double.isFinite(min) || !Double.isFinite(max) || !Double.isFinite(value)
                || min>max || value<min || value>max) return null;
        double low=Math.min(0,min), high=Math.max(0,max);
        double scale=Math.max(Math.abs(low),Math.abs(high));
        if(scale==0) return new ChartBar(0,0,0);
        // Normalize first so an extreme negative-to-positive span cannot overflow.
        low/=scale; high/=scale;
        double zero=-low/(high-low), tip=(value/scale-low)/(high-low);
        return new ChartBar(Math.max(0,Math.min(zero,tip)), Math.min(1,Math.max(zero,tip)),zero);
    }
}
