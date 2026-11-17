package fi.cashmoney;
import android.animation.*;
import android.content.Context;
import android.graphics.*;
import android.util.TypedValue;
import android.view.View;
import android.view.animation.LinearInterpolator;
/** Rotate around the actual dollar glyph centre, rather than a font baseline or text box. */
public final class RefreshIndicator extends View {
 private final Paint paint=new Paint();private final Rect glyph=new Rect();private ObjectAnimator spin;
 public RefreshIndicator(Context c){
  super(c);setAlpha(0.8f);paint.setTypeface(PixelFont.get(c));paint.setColor(0xffffff00);
  paint.setTextSize(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP,42,getResources().getDisplayMetrics()));
  paint.getTextBounds("$",0,1,glyph);setContentDescription(c.getString(R.string.refreshing));setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_YES);
 }
 @Override protected void onSizeChanged(int w,int h,int oldw,int oldh){super.onSizeChanged(w,h,oldw,oldh);setPivotX(w/2f);setPivotY(h/2f);}
 @Override protected void onDraw(Canvas canvas){super.onDraw(canvas);paint.setColor(0xff000000);canvas.drawCircle(getWidth()/2f,getHeight()/2f,Math.min(getWidth(),getHeight())/2f,paint);paint.setColor(0xffffff00);float x=(getWidth()-glyph.width())/2f-glyph.left,y=(getHeight()-glyph.height())/2f-glyph.top;canvas.drawText("$",x,y,paint);}
 @Override protected void onAttachedToWindow(){super.onAttachedToWindow();startSpin();}
 private void startSpin(){if(spin!=null)return;spin=ObjectAnimator.ofFloat(this,"rotation",0,360);spin.setDuration(850);spin.setInterpolator(new LinearInterpolator());spin.setRepeatCount(ValueAnimator.INFINITE);spin.start();}
 private void stopSpin(){if(spin!=null){spin.cancel();spin=null;}}
 @Override protected void onWindowVisibilityChanged(int visibility){super.onWindowVisibilityChanged(visibility);if(visibility==VISIBLE&&isAttachedToWindow())startSpin();else stopSpin();}
 @Override protected void onDetachedFromWindow(){stopSpin();super.onDetachedFromWindow();}
}
