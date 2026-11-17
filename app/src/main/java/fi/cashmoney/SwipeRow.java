package fi.cashmoney;
import android.animation.*;
import android.content.Context;
import android.graphics.*;
import android.view.*;
import android.view.animation.DecelerateInterpolator;
import android.widget.LinearLayout;
/** Fixed hit area with a foreground that follows the finger, revealing the action below. */
public final class SwipeRow extends LinearLayout {
 private float startX,startY,offset;private boolean swiping,settling,ignoreGesture;
 private final int slop,direction;private final float threshold,density;private final Runnable action;
 private final Paint paint=new Paint();private final String label;private ValueAnimator animator;
 public SwipeRow(Context c,int direction,Runnable action){
  super(c);this.direction=direction;this.action=action;density=c.getResources().getDisplayMetrics().density;
  slop=ViewConfiguration.get(c).getScaledTouchSlop();threshold=72*density;
  label=c.getString(direction>0?R.string.swipe_action_add:R.string.swipe_action_remove);
  setClickable(true);setClipChildren(true);
 }
 private void begin(MotionEvent e){startX=e.getX();startY=e.getY();swiping=false;ignoreGesture=false;}
 @Override public boolean onInterceptTouchEvent(MotionEvent e){
  if(settling)return true;
  if(e.getActionMasked()==MotionEvent.ACTION_DOWN)begin(e);
  if(e.getPointerCount()!=1)return false;
  if(e.getActionMasked()==MotionEvent.ACTION_MOVE&&GesturePolicy.horizontal(e.getX()-startX,e.getY()-startY,slop)){
   cancelLongPress();swiping=true;getParent().requestDisallowInterceptTouchEvent(true);return true;
  }return super.onInterceptTouchEvent(e);
 }
 @Override public boolean onTouchEvent(MotionEvent e){
  if(settling)return true;
  if(e.getActionMasked()==MotionEvent.ACTION_DOWN){begin(e);return super.onTouchEvent(e);}
  if(e.getActionMasked()==MotionEvent.ACTION_POINTER_DOWN){ignoreGesture=true;swiping=false;animateTo(0,null);return true;}
  if(ignoreGesture)return true;
  float dx=e.getX()-startX,dy=e.getY()-startY;
  if(e.getActionMasked()==MotionEvent.ACTION_MOVE&&(swiping||GesturePolicy.horizontal(dx,dy,slop))){
   cancelLongPress();swiping=true;getParent().requestDisallowInterceptTouchEvent(true);setPressed(false);
   offset=SwipeMotion.offset(dx,getWidth(),direction);invalidate();return true;
  }
  if(e.getActionMasked()==MotionEvent.ACTION_UP&&swiping){
   swiping=false;setPressed(false);getParent().requestDisallowInterceptTouchEvent(false);
   if(GesturePolicy.accepts(dx,dy,threshold,direction))animateTo(direction*getWidth(),()->{action.run();offset=0;invalidate();});
   else animateTo(0,null);return true;
  }
  if(e.getActionMasked()==MotionEvent.ACTION_CANCEL){swiping=false;setPressed(false);animateTo(0,null);return true;}
  return super.onTouchEvent(e);
 }
 private void animateTo(float target,Runnable done){
  if(animator!=null)animator.cancel();settling=true;
  animator=ValueAnimator.ofFloat(offset,target);animator.setDuration(180);animator.setInterpolator(new DecelerateInterpolator());
  animator.addUpdateListener(a->{offset=(Float)a.getAnimatedValue();invalidate();});
  animator.addListener(new AnimatorListenerAdapter(){boolean cancelled;@Override public void onAnimationCancel(Animator a){cancelled=true;}@Override public void onAnimationEnd(Animator a){settling=false;if(!cancelled&&done!=null)done.run();}});animator.start();
 }
 @Override protected void dispatchDraw(Canvas canvas){
  int w=getWidth(),h=getHeight();
  if(offset!=0){
   paint.setColor(direction>0?0xff00ff00:0xffff0000);canvas.drawRect(0,0,w,h,paint);
   int saved=canvas.save();if(direction>0)canvas.clipRect(0,0,offset,h);else canvas.clipRect(w+offset,0,w,h);
   paint.setColor(0xff000000);paint.setTypeface(PixelFont.get(getContext()));paint.setTextSize(14*getResources().getDisplayMetrics().scaledDensity);
   float labelWidth=paint.measureText(label),x=direction>0?12*density:w-12*density-labelWidth;
   canvas.drawText(label,x,h/2f-(paint.ascent()+paint.descent())/2f,paint);canvas.restoreToCount(saved);
  }
  int saved=canvas.save();canvas.translate(offset,0);paint.setColor(0xff000000);canvas.drawRect(0,0,w,h,paint);super.dispatchDraw(canvas);canvas.restoreToCount(saved);
 }
 @Override protected void onDetachedFromWindow(){if(animator!=null)animator.cancel();offset=0;settling=false;super.onDetachedFromWindow();}
}
