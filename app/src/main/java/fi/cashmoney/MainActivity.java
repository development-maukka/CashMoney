package fi.cashmoney;
import android.app.*;import android.os.*;import android.view.*;import android.widget.*;import android.text.*;import org.json.*;import java.util.*;import java.util.concurrent.*;
public class MainActivity extends Activity {
 static final int BLACK=0xff000000,WHITE=0xffffffff,BLUE=0xff0000ff,CYAN=0xff00ffff,GREEN=0xff00ff00,RED=0xffff5555,YELLOW=0xffffff00;
 final List<Yahoo.Asset> favorites=new ArrayList<>();final ExecutorService worker=Executors.newSingleThreadExecutor();final Handler handler=new Handler(Looper.getMainLooper());
 android.content.SharedPreferences prefs;LinearLayout root,body,footer;FrameLayout contentFrame;RefreshIndicator refreshIndicator;TextView status;EditText input;boolean searching=false,refreshing=false;int searchId=0;Runnable pending;
 FavoriteDragController favoriteDrag;boolean draggingFavorite=false,pendingPortfolioRefresh=false;
 Locale uiLocale;AppUpdates updates;
 @Override protected void attachBaseContext(android.content.Context base){
  android.content.res.Configuration config=new android.content.res.Configuration(base.getResources().getConfiguration());
  Locale primary=config.getLocales().isEmpty()?Locale.ENGLISH:config.getLocales().get(0);
  uiLocale=LanguagePolicy.resolve(primary);config.setLocales(new android.os.LocaleList(uiLocale));config.setLayoutDirection(uiLocale);super.attachBaseContext(base.createConfigurationContext(config));
 }
 int dp(int n){return Math.round(n*getResources().getDisplayMetrics().density);}
 TextView text(String s,int size,int color){TextView t=new TextView(this);t.setText(s);t.setTextSize(size);t.setTextColor(color);PixelFont.apply(t);t.setPadding(dp(10),dp(6),dp(10),dp(6));return t;}
 Button button(String label,int color){Button b=new Button(this);b.setText(label);b.setTextColor(color);b.setTextSize(15);PixelFont.apply(b);b.setBackgroundColor(BLUE);b.setMinWidth(0);b.setMinimumWidth(0);b.setAllCaps(false);return b;}
 @Override public void onCreate(Bundle b){super.onCreate(b);getWindow().setStatusBarColor(BLUE);getWindow().setNavigationBarColor(BLACK);prefs=getSharedPreferences("cashmoney",MODE_PRIVATE);try{JSONArray a=new JSONArray(prefs.getString("favorites","[]"));for(int i=0;i<a.length();i++)favorites.add(Yahoo.Asset.from(a.getJSONObject(i)));}catch(Exception ignored){}updates=new AppUpdates(this);showPortfolio();refresh();}
 @Override protected void onResume(){super.onResume();if(updates!=null){updates.resume();updates.check(false);}}
 @Override protected void onPause(){if(updates!=null)updates.pause();super.onPause();}
 void shell(String title){if(favoriteDrag!=null){favoriteDrag.dispose();favoriteDrag=null;}root=new HeaderRefreshLayout(this,new HeaderRefreshLayout.Callback(){public boolean enabled(){return !searching&&!refreshing&&!draggingFavorite&&!favorites.isEmpty();}public void progress(boolean ready){if(status!=null)status.setText(ready?getString(R.string.release_refresh):getString(R.string.pull_refresh));}public void released(boolean go){if(go)refresh();else if(status!=null)status.setText(getString(R.string.gestures_hint));}});root.setOrientation(1);root.setBackgroundColor(BLACK);root.setPadding(dp(8),0,dp(8),0);setContentView(root);root.setOnApplyWindowInsetsListener((v,insets)->{android.graphics.Insets x=insets.getInsets(WindowInsets.Type.systemBars());root.setPadding(dp(8)+x.left,x.top,dp(8)+x.right,x.bottom);return insets;});root.requestApplyInsets();
 LinearLayout top=new LinearLayout(this);top.setBackgroundColor(BLUE);TextView logo=text("CashMoney",26,WHITE);top.addView(logo,new LinearLayout.LayoutParams(0,dp(60),1));Button nav=button(searching?getString(R.string.nav_portfolio):getString(R.string.search),YELLOW);nav.setSingleLine(true);nav.setPadding(dp(3),0,dp(3),0);nav.setAutoSizeTextTypeUniformWithConfiguration(10,15,1,android.util.TypedValue.COMPLEX_UNIT_SP);nav.setContentDescription(searching?getString(R.string.back_portfolio):getString(R.string.search_assets));nav.setOnClickListener(v->{if(searching){searchId++;showPortfolio();refresh();}else showSearch();});top.addView(nav,new LinearLayout.LayoutParams(dp(95),dp(60)));root.addView(top);
 TextView bar=text(title,18,BLUE);bar.setBackgroundColor(CYAN);root.addView(bar);((HeaderRefreshLayout)root).setRefreshBars(top,bar);ScrollView scroll=new ScrollView(this);scroll.setOverScrollMode(View.OVER_SCROLL_NEVER);scroll.setFillViewport(true);body=new LinearLayout(this);body.setOrientation(1);scroll.addView(body);contentFrame=new FrameLayout(this);contentFrame.addView(scroll,new FrameLayout.LayoutParams(-1,-1));root.addView(contentFrame,new LinearLayout.LayoutParams(-1,0,1));status=text("",12,CYAN);root.addView(status);
 footer=new LinearLayout(this);footer.setBackgroundColor(BLUE);footer.setGravity(Gravity.CENTER_VERTICAL);root.addView(footer);updateFooter();}
 void updateFooter(){
  if(footer==null)return;footer.removeAllViews();long updated=prefs.getLong("updated_at",0);
  if(updated==0&&!favorites.isEmpty()){long oldest=Long.MAX_VALUE;for(Yahoo.Asset a:favorites)oldest=Math.min(oldest,prefs.getLong("fetched."+a.symbol,0));if(oldest>0&&oldest<Long.MAX_VALUE)updated=oldest;}
  String stamp=updated>0?getString(R.string.updated_at,new java.text.SimpleDateFormat("dd.MM.yyyy HH:mm",uiLocale).format(new Date(updated))):getString(R.string.not_updated);
  TextView time=text(stamp,12,CYAN);time.setSingleLine(true);time.setAutoSizeTextTypeUniformWithConfiguration(9,12,1,android.util.TypedValue.COMPLEX_UNIT_SP);footer.addView(time,new LinearLayout.LayoutParams(0,dp(48),1));time.setGravity(Gravity.CENTER_VERTICAL);
  updateRefreshIndicator();
  Button about=button("Info",WHITE);about.setOnClickListener(v->showInfo());footer.addView(about,new LinearLayout.LayoutParams(dp(70),dp(48)));
 }
 void updateRefreshIndicator(){
  if(refreshIndicator!=null){ViewParent parent=refreshIndicator.getParent();if(parent instanceof ViewGroup)((ViewGroup)parent).removeView(refreshIndicator);refreshIndicator=null;}
  if(refreshing&&contentFrame!=null){refreshIndicator=new RefreshIndicator(this);FrameLayout.LayoutParams params=new FrameLayout.LayoutParams(dp(66),dp(66),Gravity.TOP|Gravity.CENTER_HORIZONTAL);params.topMargin=dp(8);contentFrame.addView(refreshIndicator,params);}
 }
 void showPixelDialog(AlertDialog dialog){dialog.show();PixelFont.applyTree(dialog.getWindow().getDecorView());}
 void showInfo(){
  ScrollView scroll=new ScrollView(this);LinearLayout content=new LinearLayout(this);content.setOrientation(1);content.setPadding(dp(8),dp(8),dp(8),dp(8));content.setBackgroundColor(BLACK);scroll.addView(content);
  content.addView(text(getString(R.string.app_details),17,CYAN));
  content.addView(text(getString(R.string.about_app),15,WHITE));
  content.addView(text(getString(R.string.info),17,CYAN));
  content.addView(text(getString(R.string.about_info),14,WHITE));
  content.addView(text(getString(R.string.changelog),17,CYAN));
  String[] versions={"0.9.0","0.2.4","0.2.3"};int[] changes={R.string.changes_090,R.string.changes_024,R.string.changes_023};
  for(int i=0;i<versions.length;i++){content.addView(text(versions[i],15,YELLOW));content.addView(text(getString(changes[i]),14,WHITE));}
  showPixelDialog(new AlertDialog.Builder(this).setTitle("CashMoney · Info").setView(scroll).setNeutralButton(R.string.update_check,(d,w)->updates.check(true)).setPositiveButton(getString(R.string.close),null).create());
 }
 TextView cell(String value,int color,float weight,boolean left){
  TextView t=text(value,15,color);t.setSingleLine(true);t.setEllipsize(TextUtils.TruncateAt.END);t.setPadding(left?dp(2):0,0,left?dp(2):0,0);t.setGravity((left?Gravity.START:Gravity.END)|Gravity.CENTER_VERTICAL);t.setAutoSizeTextTypeWithDefaults(TextView.AUTO_SIZE_TEXT_TYPE_NONE);t.setLayoutParams(new LinearLayout.LayoutParams(0,dp(36),weight));return t;
 }
 void showPortfolio(){searching=false;shell(getString(R.string.portfolio_title));
  LinearLayout headers=new LinearLayout(this);headers.addView(cell(getString(R.string.security),CYAN,1.8f,true));headers.addView(cell(getString(R.string.price),CYAN,1.1f,false));for(String label:new String[]{getString(R.string.day),getString(R.string.week),getString(R.string.month),"YTD"})headers.addView(cell(label,CYAN,1f,false));root.addView(headers,2);
  if(favorites.isEmpty()){body.addView(text(getString(R.string.empty_portfolio),18,WHITE));status.setText(getString(R.string.select_favorites));return;}
  favoriteDrag=new FavoriteDragController(body,favorites,new FavoriteDragController.Callback(){
   public void started(){draggingFavorite=true;status.setText(getString(R.string.reorder_hint));}
   public void ended(boolean changed){draggingFavorite=false;if(changed)save();boolean redraw=changed||pendingPortfolioRefresh;pendingPortfolioRefresh=false;if(!isFinishing()&&!isDestroyed()&&!searching){if(redraw)showPortfolio();else status.setText(getString(R.string.gestures_hint));}}
  });
  int errors=0,old=0;
  for(Yahoo.Asset a:new ArrayList<>(favorites)){
   String cached=prefs.getString("quote."+a.symbol,null);Yahoo.Price parsed=null;if(cached!=null)try{parsed=Yahoo.parse(new JSONObject(cached));}catch(Exception ignored){}
   final Yahoo.Price p=parsed;final String error=localizedError(prefs.getString("error."+a.symbol,null));
   boolean stale=p!=null&&(System.currentTimeMillis()-prefs.getLong("fetched."+a.symbol,0)>900000||error!=null);if(stale)old++;if(error!=null)errors++;
   LinearLayout row=new SwipeRow(this,-1,()->{favorites.remove(a);save();showPortfolio();Toast.makeText(this,getString(R.string.removed)+a.name,Toast.LENGTH_SHORT).show();});row.setGravity(Gravity.CENTER_VERTICAL);row.setBackgroundColor(BLACK);
   row.addView(cell(a.name+(error!=null?" !":stale?" *":""),WHITE,1.8f,true));
   row.addView(cell(p!=null?String.format(uiLocale,"%.2f",p.current):"—",YELLOW,1.1f,false));
   for(int i=0;i<4;i++){double change=p==null?Double.NaN:p.changes[i];row.addView(cell(Double.isFinite(change)?String.format(uiLocale,"%+.2f%%",change):"—",!Double.isFinite(change)?WHITE:change>=0?GREEN:RED,1f,false));}
   favoriteDrag.bind(row,a);
   row.setContentDescription(a.name+", "+a.symbol+getString(R.string.tap_details)+" "+getString(R.string.reorder_accessibility));
   row.setOnClickListener(v->{String details=a.symbol+" · "+a.exchange;
    if(p!=null){details+="\n"+String.format(uiLocale,"%.2f %s",p.current,a.type.equals("INDEX")?getString(R.string.index_points):p.currency)+"\n"+getString(R.string.quote_time)+p.date;String[] periods={getString(R.string.day),getString(R.string.week),getString(R.string.month),"YTD",getString(R.string.three_years),getString(R.string.five_years)};for(int i=0;i<periods.length;i++)details+="\n"+periods[i]+": "+(Double.isFinite(p.changes[i])?String.format(uiLocale,"%+.2f%%",p.changes[i]):"—");details+="\n\n"+getString(R.string.dividend_yield)+": "+(!a.type.equals("INDEX")&&Double.isFinite(p.dividendYield)?String.format(uiLocale,"%.2f%%",p.dividendYield):"—");details+="\n"+getString(R.string.fetched)+new java.text.SimpleDateFormat("dd.MM.yyyy HH:mm",uiLocale).format(new Date(prefs.getLong("fetched."+a.symbol,0)));}
    else details+="\n"+getString(R.string.no_quote);if(error!=null)details+="\n\n"+error;
    showPixelDialog(new AlertDialog.Builder(this).setTitle(a.name).setMessage(details).setPositiveButton(getString(R.string.close),null).create());
   });body.addView(row);View line=new View(this);line.setBackgroundColor(BLUE);body.addView(line,new LinearLayout.LayoutParams(-1,dp(1)));
  }
  status.setText(refreshing?getString(R.string.loading):errors>0?errors+getString(R.string.errors):old>0?getString(R.string.cached):getString(R.string.gestures_hint));
 }
 void save(){JSONArray array=new JSONArray();for(Yahoo.Asset a:favorites)try{array.put(a.json());}catch(Exception ignored){}prefs.edit().putString("favorites",array.toString()).apply();}
 boolean contains(String s){for(Yahoo.Asset a:favorites)if(a.symbol.equals(s))return true;return false;}
 void refresh(){
  if(refreshing||favorites.isEmpty())return;refreshing=true;List<Yahoo.Asset> selected=new ArrayList<>(favorites);if(!searching)showPortfolio();else updateFooter();
  worker.execute(()->{int succeeded=0;for(Yahoo.Asset a:selected)try{JSONObject data=Yahoo.quote(a.symbol);Yahoo.parse(data);prefs.edit().putString("quote."+a.symbol,data.toString()).putLong("fetched."+a.symbol,System.currentTimeMillis()).remove("error."+a.symbol).apply();succeeded++;}catch(Exception e){prefs.edit().putString("error."+a.symbol,errorCode(e)).apply();}
   if(succeeded==selected.size())prefs.edit().putLong("updated_at",System.currentTimeMillis()).apply();
   handler.post(()->{refreshing=false;if(!isFinishing()&&!isDestroyed()){if(!searching&&!draggingFavorite)showPortfolio();else{if(draggingFavorite)pendingPortfolioRefresh=true;updateFooter();}}});
  });
 }
 String localizedError(String code){if(code==null)return null;if(code.startsWith("@http:"))return getString(R.string.http_error,code.substring(6));if(code.equals("@rate_limit"))return getString(R.string.rate_limit);if(code.equals("@no_quote"))return getString(R.string.no_quote);if(code.equals("@no_history"))return getString(R.string.no_history);if(code.equals("@offline"))return getString(R.string.offline);if(code.equals("@timeout"))return getString(R.string.timeout);return getString(R.string.fetch_failed);}
 String errorCode(Exception e){if(e instanceof java.net.UnknownHostException)return "@offline";if(e instanceof java.net.SocketTimeoutException)return "@timeout";String code=e.getMessage();return code!=null&&code.startsWith("@")?code:"@fetch_failed";}
 String message(Exception e){return localizedError(errorCode(e));}
 void showSearch(){searching=true;shell(getString(R.string.search_title));input=new EditText(this);input.setSingleLine();input.setTextColor(WHITE);input.setHintTextColor(CYAN);input.setHint(getString(R.string.search_hint));input.setTextSize(16);PixelFont.apply(input);root.addView(input,2,new LinearLayout.LayoutParams(-1,dp(60)));input.setImeOptions(android.view.inputmethod.EditorInfo.IME_ACTION_SEARCH);input.setOnEditorActionListener((v,a,event)->{startSearch();return true;});input.addTextChangedListener(new TextWatcher(){public void beforeTextChanged(CharSequence s,int a,int c,int f){}public void onTextChanged(CharSequence s,int a,int b,int c){searchId++;if(pending!=null)handler.removeCallbacks(pending);pending=()->startSearch();handler.postDelayed(pending,650);}public void afterTextChanged(Editable e){}});body.addView(text(getString(R.string.search_intro),16,WHITE));status.setText(getString(R.string.markets));}
 void startSearch(){if(!searching||input==null)return;final String query=input.getText().toString().trim();final int id=++searchId;if(query.length()<1){body.removeAllViews();body.addView(text(getString(R.string.enter_query),16,WHITE));return;}body.removeAllViews();status.setText(getString(R.string.searching)+query);worker.execute(()->{try{List<Yahoo.Asset> results=Yahoo.search(query);handler.post(()->{if(!searching||id!=searchId||isDestroyed())return;body.removeAllViews();status.setText(results.size()+getString(R.string.results));if(results.isEmpty())body.addView(text(getString(R.string.no_results),16,YELLOW));for(Yahoo.Asset a:results){LinearLayout row=new SwipeRow(MainActivity.this,1,()->{if(!contains(a.symbol)){favorites.add(a);save();Toast.makeText(this,getString(R.string.added)+a.name,Toast.LENGTH_SHORT).show();}else Toast.makeText(this,getString(R.string.already_added),Toast.LENGTH_SHORT).show();});row.setContentDescription(a.name+getString(R.string.swipe_add));LinearLayout details=new LinearLayout(MainActivity.this);details.setOrientation(1);details.addView(text(a.name,17,WHITE));details.addView(text(a.symbol+" · "+a.exchange+" · "+(a.type.equals("INDEX")?getString(R.string.index_type):a.type.equals("MUTUALFUND")?getString(R.string.fund):a.type.equals("ETF")?"ETF":getString(R.string.stock)),12,CYAN));row.addView(details,new LinearLayout.LayoutParams(0,-2,1));body.addView(row);}});}catch(Exception e){handler.post(()->{if(searching&&id==searchId&&!isDestroyed()){body.removeAllViews();body.addView(text(message(e),16,RED));Button retry=button(getString(R.string.retry),WHITE);retry.setOnClickListener(v->startSearch());body.addView(retry);status.setText(getString(R.string.search_failed));}});}});}
 @Override public void onBackPressed(){if(searching){searchId++;showPortfolio();refresh();}else super.onBackPressed();}
 @Override public void onDestroy(){if(favoriteDrag!=null)favoriteDrag.dispose();handler.removeCallbacksAndMessages(null);worker.shutdownNow();super.onDestroy();}
}
