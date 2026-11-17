package fi.cashmoney;
import android.content.Context;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.view.*;
import android.widget.TextView;
/** Packaged offline pixel font; no synthetic bold or smoothing of the pixel outlines. */
public final class PixelFont {
 private static Typeface face;
 public static Typeface get(Context c){if(face==null)face=c.getResources().getFont(R.font.vt323);return face;}
 public static void apply(TextView view){view.setTypeface(get(view.getContext()));view.setPaintFlags(view.getPaintFlags()&~(Paint.ANTI_ALIAS_FLAG|Paint.SUBPIXEL_TEXT_FLAG));}
 public static void applyTree(View view){if(view instanceof TextView)apply((TextView)view);if(view instanceof ViewGroup){ViewGroup group=(ViewGroup)view;for(int i=0;i<group.getChildCount();i++)applyTree(group.getChildAt(i));}}
}
