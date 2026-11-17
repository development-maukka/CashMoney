package fi.cashmoney;
/** Keep the moving foreground within the supported action direction and row width. */
public final class SwipeMotion {
 public static float offset(float dx,float width,int direction){float distance=Math.max(0,Math.min(Math.max(0,width),dx*direction));return direction*distance;}
}
