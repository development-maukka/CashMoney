package fi.cashmoney;
import android.content.Context;
import android.graphics.Rect;
import android.view.*;
import android.widget.LinearLayout;
/** A refresh must start on one of the two coloured app bars. */
public final class HeaderRefreshLayout extends LinearLayout {
 public interface Callback {boolean enabled();void progress(boolean ready);void released(boolean refresh);}
 private final Callback callback;private final int slop;private final float threshold;
 private View blue,cyan;private float startX,startY;private boolean tracking,pulling;
 public HeaderRefreshLayout(Context context,Callback callback){super(context);this.callback=callback;slop=ViewConfiguration.get(context).getScaledTouchSlop();threshold=80*context.getResources().getDisplayMetrics().density;}
 public void setRefreshBars(View blue,View cyan){this.blue=blue;this.cyan=cyan;}
 private boolean inside(View view,float x,float y){if(view==null)return false;Rect rect=new Rect();view.getHitRect(rect);return rect.contains((int)x,(int)y);}
 private void begin(MotionEvent e){startX=e.getX();startY=e.getY();pulling=false;tracking=callback.enabled()&&(inside(blue,startX,startY)||inside(cyan,startX,startY));}
 private boolean update(MotionEvent e){
  if(!tracking)return false;
  if(!callback.enabled()||e.getPointerCount()!=1){cancel();return false;}
  float dx=e.getX()-startX,dy=e.getY()-startY;
  if(!pulling&&(Math.abs(dx)>slop||Math.abs(dy)>slop)){
   if(!GesturePolicy.pull(dx,dy,slop,true)){tracking=false;return false;}
   pulling=true;
  }
  return pulling;
 }
 private void cancel(){boolean notify=pulling;tracking=false;pulling=false;if(notify)callback.released(false);}
 @Override public boolean onInterceptTouchEvent(MotionEvent e){
  switch(e.getActionMasked()){
   case MotionEvent.ACTION_DOWN:begin(e);break;
   case MotionEvent.ACTION_MOVE:if(update(e))return true;break;
   case MotionEvent.ACTION_POINTER_DOWN:case MotionEvent.ACTION_CANCEL:cancel();break;
   case MotionEvent.ACTION_UP:tracking=false;break;
  }
  return super.onInterceptTouchEvent(e);
 }
 @Override public boolean onTouchEvent(MotionEvent e){
  switch(e.getActionMasked()){
   case MotionEvent.ACTION_DOWN:return tracking;
   case MotionEvent.ACTION_MOVE:if(update(e)){callback.progress(GesturePolicy.pull(e.getX()-startX,e.getY()-startY,threshold,true));return true;}return tracking;
   case MotionEvent.ACTION_UP:
    boolean handled=tracking;boolean go=pulling&&callback.enabled()&&GesturePolicy.pull(e.getX()-startX,e.getY()-startY,threshold,true);
    boolean notify=pulling;tracking=false;pulling=false;if(notify)callback.released(go);return handled;
   case MotionEvent.ACTION_POINTER_DOWN:case MotionEvent.ACTION_CANCEL:cancel();return true;
  }
  return super.onTouchEvent(e);
 }
}
