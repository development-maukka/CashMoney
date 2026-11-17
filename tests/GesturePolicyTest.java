import fi.cashmoney.GesturePolicy;
public class GesturePolicyTest {
 static void check(boolean ok){if(!ok)throw new AssertionError();}
 public static void main(String[] args){
 check(GesturePolicy.accepts(90,10,72,1));
 check(!GesturePolicy.accepts(-90,10,72,1));
 check(GesturePolicy.accepts(-90,10,72,-1));
 check(!GesturePolicy.accepts(90,10,72,-1));
 check(!GesturePolicy.accepts(40,0,72,1));
 check(!GesturePolicy.accepts(90,90,72,1));
 check(!GesturePolicy.accepts(10,120,72,-1));
 check(GesturePolicy.pull(0,100,80,true));
 check(!GesturePolicy.pull(0,100,80,false));
 check(!GesturePolicy.pull(0,-100,80,true));
 check(!GesturePolicy.pull(100,90,80,true));
 check(!GesturePolicy.pull(0,50,80,true));
 System.out.println("PASS: swipe directions, minimum distance, vertical/diagonal rejection, pull only at top, pull threshold");
 }
}
