package fi.cashmoney;
import android.app.*;
import android.content.*;
import android.content.pm.*;
import android.database.Cursor;
import android.net.Uri;
import android.os.*;
import android.provider.Settings;
import org.json.*;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;
import java.util.concurrent.*;

/** Platform-only updater. No credentials, external SDKs or silent installation. */
final class AppUpdates {
 static final String API="https://api.github.com/repos/development-maukka/CashMoney/releases/latest";
 private static boolean checked;
 private final Activity a;
 private final SharedPreferences prefs;
 private final DownloadManager dm;
 private final Handler handler=new Handler(Looper.getMainLooper());
 private final ExecutorService worker=Executors.newSingleThreadExecutor();
 private ProgressDialog progress;
 private boolean resumed,checking,verifying,dialogOpen;private JSONObject deferred;private boolean deferredManual;
 private final Runnable poll=()->poll();
 AppUpdates(Activity a){this.a=a;prefs=a.getSharedPreferences("app_updates",0);dm=(DownloadManager)a.getSystemService(Context.DOWNLOAD_SERVICE);prefs.edit().putBoolean("verifiedThisSession",false).apply();}
 void resume(){resumed=true;if(deferred!=null){JSONObject r=deferred;deferred=null;showRelease(r,deferredManual);}if(prefs.getLong("id",0)>0)poll();else if(prefs.getBoolean("ready",false)){if(!prefs.getBoolean("verifiedThisSession",false))verify();else ready();}}
 void pause(){resumed=false;handler.removeCallbacks(poll);if(progress!=null){progress.dismiss();progress=null;}}
 void destroy(){pause();checked=false;worker.shutdown();}
 private boolean visible(){return resumed&&!a.isFinishing()&&!a.isDestroyed();}
 void check(boolean manual){
  if(checking||(!manual&&checked))return;checked=true;checking=true;
  if(manual)android.widget.Toast.makeText(a,R.string.update_checking,0).show();
  worker.execute(()->{
   JSONObject release=null;String error=null;
   try{
    HttpURLConnection c=(HttpURLConnection)new URL(API).openConnection();
    c.setConnectTimeout(15000);c.setReadTimeout(20000);c.setInstanceFollowRedirects(false);
    c.setRequestProperty("Accept","application/vnd.github+json");c.setRequestProperty("User-Agent","CashMoney/0.9.0");
    try{int status=c.getResponseCode();if(status==404){release=null;}else if(status!=200)throw new IOException("HTTP "+status);
     else try(InputStream in=c.getInputStream()){ByteArrayOutputStream out=new ByteArrayOutputStream();byte[] b=new byte[8192];int n;while((n=in.read(b))!=-1){out.write(b,0,n);if(out.size()>2*1024*1024)throw new IOException("Response too large");}release=new JSONObject(new String(out.toByteArray(),StandardCharsets.UTF_8));}
    }finally{c.disconnect();}
   }catch(Exception e){error=e.getMessage();}
   final JSONObject r=release;final String e=error;
   handler.post(()->{checking=false;if(!visible()){if(r!=null){deferred=r;deferredManual=manual;}return;}if(e!=null){if(manual)failure(a.getString(R.string.update_network));return;}
    if(r==null){if(manual)message(R.string.update_unpublished);return;}
    showRelease(r,manual);
   });
  });
 }
 private void showRelease(JSONObject r,boolean manual){
  if(!visible()||dialogOpen)return;
  try{String current=a.getPackageManager().getPackageInfo(a.getPackageName(),0).versionName;
   if(r.optBoolean("draft",true)||r.optBoolean("prerelease",true)||!ReleaseVersion.valid(r.optString("tag_name"))) {if(manual)message(R.string.update_unpublished);return;}
   if(ReleaseVersion.compare(r.getString("tag_name"),current)<=0){if(manual)message(R.string.update_latest);return;}
   JSONObject asset=ReleaseVersion.asset(r);if(asset==null){if(manual)message(R.string.update_no_apk);return;}
   dialogOpen=true;AlertDialog dialog=new AlertDialog.Builder(a).setTitle(a.getString(R.string.update_available)+" "+r.getString("tag_name")).setMessage(r.optString("body",a.getString(R.string.update_notes))).setPositiveButton(R.string.update_download,(d,w)->download(r.optString("tag_name"),asset)).setNegativeButton(R.string.update_later,null).create();dialog.setOnDismissListener(d->dialogOpen=false);show(dialog);
  }catch(Exception e){if(manual)failure(a.getString(R.string.update_network));}
 }
 private File apk(){File dir=a.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS);if(dir==null)throw new IllegalStateException();return new File(dir,"cashmoney-update.apk");}
 private void download(String version,JSONObject asset){
  if(prefs.getLong("id",0)>0){poll();return;}
  try{
   String url=asset.getString("browser_download_url");if(!ReleaseVersion.trusted(url))throw new IOException("Invalid origin");
   apk().delete();
   DownloadManager.Request req=new DownloadManager.Request(Uri.parse(url));
   req.setTitle("CashMoney "+version).setDescription(a.getString(R.string.update_downloading));req.setMimeType("application/vnd.android.package-archive");
   req.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
   req.setDestinationInExternalFilesDir(a,Environment.DIRECTORY_DOWNLOADS,"cashmoney-update.apk");
   long id=dm.enqueue(req);
   prefs.edit().putLong("id",id).putLong("size",asset.optLong("size")).putString("version",version).putString("digest",asset.optString("digest")).putBoolean("ready",false).apply();poll();
  }catch(Exception e){failure(a.getString(R.string.update_failed));}
 }
 private void poll(){
  handler.removeCallbacks(poll);if(!visible()||verifying)return;
  long id=prefs.getLong("id",0);if(id==0)return;
  try(Cursor c=dm.query(new DownloadManager.Query().setFilterById(id))){
   if(c==null||!c.moveToFirst())throw new IOException("Download missing");
   int status=c.getInt(c.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS));
   if(status==DownloadManager.STATUS_FAILED)throw new IOException("DownloadManager "+c.getInt(c.getColumnIndexOrThrow(DownloadManager.COLUMN_REASON)));
   if(status==DownloadManager.STATUS_SUCCESSFUL){if(progress!=null){progress.dismiss();progress=null;}verify();return;}
   if(progress==null){progress=new ProgressDialog(a);progress.setTitle(R.string.update_downloading);progress.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);progress.setCancelable(false);progress.setButton(DialogInterface.BUTTON_NEGATIVE,a.getString(R.string.update_cancel),(d,w)->{dm.remove(id);prefs.edit().remove("id").apply();handler.removeCallbacks(poll);progress=null;});progress.show();}
   long total=c.getLong(c.getColumnIndexOrThrow(DownloadManager.COLUMN_TOTAL_SIZE_BYTES)),done=c.getLong(c.getColumnIndexOrThrow(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR));
   progress.setIndeterminate(total<=0);if(total>0)progress.setProgress((int)(100*done/total));
   progress.setMessage(a.getString(status==DownloadManager.STATUS_PAUSED?R.string.update_waiting:R.string.update_downloading));
   handler.postDelayed(poll,700);
  }catch(Exception e){prefs.edit().remove("id").apply();if(progress!=null){progress.dismiss();progress=null;}failure(a.getString(R.string.update_failed));}
 }
 private void verify(){
  verifying=true;prefs.edit().putBoolean("verifiedThisSession",false).apply();
  worker.execute(()->{String error=null;try{validate();}catch(Exception e){error=e.getMessage();}
   final String failure=error;handler.post(()->{verifying=false;prefs.edit().remove("id").putBoolean("ready",failure==null).putBoolean("verifiedThisSession",failure==null).apply();if(!visible())return;if(failure!=null)failure(a.getString(R.string.update_invalid));else ready();});});
 }
 private void validate()throws Exception{
  PackageManager pm=a.getPackageManager();
  PackageInfo installed=pm.getPackageInfo(a.getPackageName(),PackageManager.GET_SIGNATURES);
  PackageInfo incoming=pm.getPackageArchiveInfo(apk().getAbsolutePath(),PackageManager.GET_SIGNATURES);
  if(apk().length()!=prefs.getLong("size",-1))throw new IOException("APK size mismatch");
  if(incoming==null||!a.getPackageName().equals(incoming.packageName)||incoming.getLongVersionCode()<=installed.getLongVersionCode()||!ReleaseVersion.valid(incoming.versionName)||ReleaseVersion.compare(incoming.versionName,installed.versionName)<=0||ReleaseVersion.compare(incoming.versionName,prefs.getString("version",""))!=0)throw new IOException("Package/version mismatch");
  if(incoming.applicationInfo==null||incoming.applicationInfo.minSdkVersion>Build.VERSION.SDK_INT||!compatibleAbi(apk(),Build.SUPPORTED_ABIS))throw new IOException("Android compatibility mismatch");
  Set<String> old=signatures(installed),next=signatures(incoming);if(old.isEmpty()||!old.equals(next))throw new IOException("Signature mismatch");
  String digest=prefs.getString("digest","");
  if(!digest.isEmpty()&&!digest.matches("sha256:[a-fA-F0-9]{64}"))throw new IOException("Invalid checksum metadata");
  if(digest.startsWith("sha256:")){MessageDigest md=MessageDigest.getInstance("SHA-256");try(InputStream in=new FileInputStream(apk())){byte[] b=new byte[65536];int n;while((n=in.read(b))!=-1)md.update(b,0,n);}StringBuilder hex=new StringBuilder();for(byte v:md.digest())hex.append(String.format(java.util.Locale.ROOT,"%02x",v&255));if(!digest.substring(7).equalsIgnoreCase(hex.toString()))throw new IOException("Checksum mismatch");}
 }
 static boolean compatibleAbi(File file,String[] supported)throws IOException{
  boolean libraries=false;try(java.util.zip.ZipFile zip=new java.util.zip.ZipFile(file)){java.util.Enumeration<? extends java.util.zip.ZipEntry> entries=zip.entries();while(entries.hasMoreElements()){String name=entries.nextElement().getName();if(name.startsWith("lib/")&&name.endsWith(".so")){libraries=true;String[] path=name.split("/");if(path.length==3)for(String abi:supported)if(path[1].equals(abi))return true;}}}return !libraries;
 }
 private Set<String> signatures(PackageInfo p){Set<String>s=new HashSet<>();if(p.signatures!=null)for(Signature sig:p.signatures)s.add(sig.toCharsString());return s;}
 private void ready(){if(!visible()||dialogOpen)return;dialogOpen=true;AlertDialog readyDialog=new AlertDialog.Builder(a).setTitle(R.string.update_ready).setMessage(R.string.update_install_hint).setPositiveButton(R.string.update_install,(d,w)->install()).setNegativeButton(R.string.update_later,(d,w)->prefs.edit().putBoolean("ready",false).apply()).create();readyDialog.setOnDismissListener(d->dialogOpen=false);show(readyDialog);}
 private void install(){
  if(!prefs.getBoolean("verifiedThisSession",false)){verify();return;}
  if(!a.getPackageManager().canRequestPackageInstalls()){
   new AlertDialog.Builder(a).setMessage(R.string.update_permission).setPositiveButton(R.string.update_settings,(d,w)->{try{a.startActivity(new Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,Uri.parse("package:"+a.getPackageName())));}catch(Exception e){failure(a.getString(R.string.update_failed));}}).setNegativeButton(R.string.update_later,null).show();return;
  }
  // Android's installer performs final cryptographic verification and asks for installation consent.
  try{Uri uri=new Uri.Builder().scheme("content").authority(a.getPackageName()+".files").appendPath("updates").appendPath("cashmoney-update.apk").build();
   Intent intent=new Intent(Intent.ACTION_VIEW).setDataAndType(uri,"application/vnd.android.package-archive").addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);intent.setClipData(ClipData.newRawUri("CashMoney update",uri));a.startActivity(intent);prefs.edit().putBoolean("ready",false).apply();
  }catch(Exception e){failure(a.getString(R.string.update_failed));}
 }
 private void show(AlertDialog dialog){dialog.show();PixelFont.applyTree(dialog.getWindow().getDecorView());}
 private void message(int id){show(new AlertDialog.Builder(a).setMessage(id).setPositiveButton(R.string.close,null).create());}
 private void failure(String text){if(visible())show(new AlertDialog.Builder(a).setTitle(R.string.update_error_title).setMessage(text).setPositiveButton(R.string.close,null).create());}
}
