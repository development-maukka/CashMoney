package fi.cashmoney;
public final class GesturePolicy {
 public static boolean horizontal(float dx,float dy,float threshold){return Math.abs(dx)>=threshold&&Math.abs(dx)>Math.abs(dy)*1.5f;}
 public static boolean pull(float dx,float dy,float threshold,boolean atTop){return atTop&&dy>=threshold&&dy>Math.abs(dx)*1.5f;}
 public static boolean accepts(float dx,float dy,float threshold,int direction){return horizontal(dx,dy,threshold)&&dx*direction>0;}
}
