package fi.cashmoney;
import android.graphics.drawable.ColorDrawable;
import android.os.Handler;
import android.view.*;
import android.widget.*;
import java.util.List;
/** Native long-press drag shadow, insertion marker, and scrolling at list edges. */
public final class FavoriteDragController implements View.OnDragListener {
 public interface Callback {void started();void ended(boolean changed);}
 private final LinearLayout list;private final ScrollView scroll;private final List<Yahoo.Asset> items;private final Callback callback;
 private final Handler handler=new Handler(android.os.Looper.getMainLooper());private final ColorDrawable marker=new ColorDrawable(0xff00ffff);
 private final float density;private View source;private Yahoo.Asset asset;private float viewportY;private boolean active,changed;private int gap;
 public FavoriteDragController(LinearLayout list,List<Yahoo.Asset> items,Callback callback){this.list=list;this.items=items;this.callback=callback;scroll=(ScrollView)list.getParent();density=list.getResources().getDisplayMetrics().density;list.setOnDragListener(this);}
 public void bind(View row,Yahoo.Asset asset){row.setOnLongClickListener(v->{
  if(active)return true;this.asset=asset;source=v;changed=false;active=true;
  boolean ok=v.startDragAndDrop(null,new View.DragShadowBuilder(v),asset,0);
  if(ok){v.setAlpha(0.35f);callback.started();}else{active=false;source=null;this.asset=null;}
  return ok;
 });}
 private final Runnable autoScroll=new Runnable(){public void run(){
  if(!active)return;float edge=48*density;int speed=viewportY<edge?-(int)(8*density):viewportY>scroll.getHeight()-edge?(int)(8*density):0;
  if(speed!=0){scroll.scrollBy(0,speed);mark(viewportY+scroll.getScrollY());}handler.postDelayed(this,16);
 }};
 private void mark(float contentY){
  gap=items.size();int markerY=list.getHeight()-Math.max(1,(int)(2*density));
  for(int i=0;i<items.size();i++){View row=list.getChildAt(i*2);if(row!=null&&contentY<row.getTop()+row.getHeight()/2f){gap=i;markerY=row.getTop();break;}}
  marker.setBounds(0,Math.max(0,markerY),list.getWidth(),Math.max(0,markerY)+Math.max(2,(int)(2*density)));list.invalidate();
 }
 @Override public boolean onDrag(View view,DragEvent event){
  if(!active||event.getLocalState()!=asset)return false;
  switch(event.getAction()){
   case DragEvent.ACTION_DRAG_STARTED:list.getOverlay().add(marker);return true;
   case DragEvent.ACTION_DRAG_ENTERED:handler.removeCallbacks(autoScroll);handler.post(autoScroll);return true;
   case DragEvent.ACTION_DRAG_LOCATION:viewportY=event.getY()-scroll.getScrollY();mark(event.getY());return true;
   case DragEvent.ACTION_DRAG_EXITED:handler.removeCallbacks(autoScroll);marker.setBounds(0,0,0,0);list.invalidate();return true;
   case DragEvent.ACTION_DROP:mark(event.getY());changed=FavoriteOrder.move(items,asset,gap);return true;
   case DragEvent.ACTION_DRAG_ENDED:boolean moved=changed;dispose();callback.ended(moved);return true;
   default:return true;
  }
 }
 public void dispose(){active=false;handler.removeCallbacksAndMessages(null);list.getOverlay().remove(marker);if(source!=null)source.setAlpha(1);source=null;asset=null;}
}
