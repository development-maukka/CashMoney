package fi.cashmoney;
import java.math.BigInteger;import java.net.URI;import org.json.*;
final class ReleaseVersion {
 static final String REPO="https://github.com/development-maukka/CashMoney";
 static boolean valid(String v){return v!=null&&v.length()<=100&&v.matches("[vV]?[0-9]+(\\.[0-9]+){0,3}");}
 static int compare(String a,String b){if(!valid(a)||!valid(b))throw new IllegalArgumentException();String[] x=a.replaceFirst("^[vV]","").split("\\."),y=b.replaceFirst("^[vV]","").split("\\.");for(int i=0;i<Math.max(x.length,y.length);i++){int c=new BigInteger(i<x.length?x[i]:"0").compareTo(new BigInteger(i<y.length?y[i]:"0"));if(c!=0)return c;}return 0;}
 static boolean trusted(String url){try{URI u=new URI(url);String p=u.getRawPath();return u.getScheme().equals("https")&&"github.com".equals(u.getHost())&&u.getPort()==-1&&u.getUserInfo()==null&&u.getQuery()==null&&u.getFragment()==null&&p.startsWith("/development-maukka/CashMoney/releases/download/")&&!p.contains("%")&&!p.contains("/../")&&!p.contains("/./")&&p.endsWith(".apk")&&p.substring("/development-maukka/CashMoney/releases/download/".length()).split("/",-1).length==2;}catch(Exception e){return false;}}
 static JSONObject asset(JSONObject r)throws Exception{
  String tag=r.optString("tag_name");if(r.optBoolean("draft",true)||r.optBoolean("prerelease",true)||!valid(tag)||!r.optString("html_url").equals(REPO+"/releases/tag/"+tag))return null;
  JSONArray list=r.optJSONArray("assets");if(list==null)return null;
  for(int i=0;i<list.length();i++){JSONObject a=list.getJSONObject(i);String name=a.optString("name");if(!name.matches("CashMoney-[vV]?[0-9.]+\\.apk"))continue;String version=name.substring("CashMoney-".length(),name.length()-4);if(!valid(version)||compare(version,tag)!=0)continue;String url=a.optString("browser_download_url");if(trusted(url)&&url.equals(REPO+"/releases/download/"+tag+"/"+name)&&a.optLong("size",0)>0&&a.optLong("size")<200000000&&a.optString("state").equals("uploaded"))return a;
  }return null;
 }
}
