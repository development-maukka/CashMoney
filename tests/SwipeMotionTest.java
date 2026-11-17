import fi.cashmoney.SwipeMotion;
public class SwipeMotionTest {
 static void eq(float actual,float expected){if(Math.abs(actual-expected)>0.001)throw new AssertionError(actual+" != "+expected);}
 public static void main(String[]args){
  eq(SwipeMotion.offset(45,360,1),45);eq(SwipeMotion.offset(-45,360,-1),-45);
  eq(SwipeMotion.offset(-45,360,1),0);eq(SwipeMotion.offset(45,360,-1),0);
  eq(SwipeMotion.offset(500,360,1),360);eq(SwipeMotion.offset(-500,360,-1),-360);
  eq(SwipeMotion.offset(30,0,1),0);eq(SwipeMotion.offset(0,360,-1),0);
  eq(SwipeMotion.offset(12,360,1),12);eq(SwipeMotion.offset(4,360,1),4);
  System.out.println("PASS: foreground tracks finger, returns towards origin, clamps to row width and ignores unsupported direction");
 }
}
