package com.example.frontend.utils;

import android.graphics.Color;

import java.util.Random;

public class ColorUtils {

    public static int getColorForUserId(long userId) {
        int hash = Long.valueOf(userId).hashCode();
        Random random = new Random(hash);
        int red = 100 + random.nextInt(156);   // Pour éviter des couleurs trop sombres
        int green = 100 + random.nextInt(156);
        int blue = 100 + random.nextInt(156);
        return Color.rgb(red, green, blue);
    }
}