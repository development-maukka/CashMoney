package fi.cashmoney;
import org.json.*;
public class ReleaseVersionTest {
 static void check(boolean ok,String why){if(!ok)throw new AssertionError(why);}
 static JSONObject release(String version)throws Exception{return new JSONObject().put("tag_name",version).put("html_url",ReleaseVersion.REPO+"/releases/tag/"+version).put("draft",false).put("prerelease",false).put("assets",new JSONArray().put(new JSONObject().put("name","CashMoney-"+version+".apk").put("browser_download_url",ReleaseVersion.REPO+"/releases/download/"+version+"/CashMoney-"+version+".apk").put("size",123456).put("state","uploaded")));}
 public static void main(String[] args)throws Exception{
 JSONObject current=release("v0.9.0");current.getJSONArray("assets").getJSONObject(0).put("name","CashMoney-0.9.0.apk").put("browser_download_url",ReleaseVersion.REPO+"/releases/download/v0.9.0/CashMoney-0.9.0.apk");check(ReleaseVersion.asset(current)!=null,"CashMoney publication name accepted");check(ReleaseVersion.compare("v0.9.0","0.9.0")==0,"no self-update prompt");check(ReleaseVersion.compare("v0.9.1","0.9.0")>0,"next release offered");
 check(ReleaseVersion.compare("1.0.0","0.9.9")>0,"first stable upgrades preparation build");check(ReleaseVersion.compare("0.10.0","0.9.9")>0,"numeric comparison");check(ReleaseVersion.compare("v1.0.0","1")==0,"v prefix and padded equality");check(ReleaseVersion.compare("0.8.6","0.9.9")<0,"downgrade");check(ReleaseVersion.compare("99999999999999999.1","1.2")>0,"no overflow");
 for(String bad:new String[]{"","1.2-beta","1.0+5","/1.2","NaN","v"})check(!ReleaseVersion.valid(bad),"reject "+bad);
 JSONObject good=release("v1.0.0");check(ReleaseVersion.asset(good)!=null,"official universal APK");
 for(String flag:new String[]{"draft","prerelease"}){JSONObject r=new JSONObject(good.toString()).put(flag,true);check(ReleaseVersion.asset(r)==null,"reject "+flag);}
 JSONObject missing=new JSONObject(good.toString()).put("assets",new JSONArray());check(ReleaseVersion.asset(missing)==null,"missing APK");
 for(String field:new String[]{"size","state","name","browser_download_url"}){JSONObject r=new JSONObject(good.toString());r.getJSONArray("assets").getJSONObject(0).put(field,field.equals("size")?0:"bad");check(ReleaseVersion.asset(r)==null,"invalid "+field);}
 JSONObject wrongTag=new JSONObject(good.toString());wrongTag.getJSONArray("assets").getJSONObject(0).put("name","CashMoney-9.0.0.apk");check(ReleaseVersion.asset(wrongTag)==null,"tag vs asset mismatch");
 JSONObject foreign=new JSONObject(good.toString()).put("html_url","https://github.com/evil/CashMoney/releases/tag/v1.0.0");check(ReleaseVersion.asset(foreign)==null,"foreign release");
 String base=ReleaseVersion.REPO+"/releases/download/v1.0.0/x.apk";check(ReleaseVersion.trusted(base),"official origin");
 for(String bad:new String[]{base+"?foo",base+"#x",base.replace("https:","http:"),base.replace("github.com","github.com.evil"),base.replace("github.com/","github.com@evil/"),base.replace("/x.apk","/%2e%2e/x.apk"),base.replace("/x.apk","/../x.apk"),base.replace("development-maukka","other"),base.replace("github.com/","github.com:8443/")})check(!ReleaseVersion.trusted(bad),"reject unsafe URL "+bad);
 boolean invalid=false;try{ReleaseVersion.compare("1-beta","1");}catch(IllegalArgumentException e){invalid=true;}check(invalid,"invalid comparison rejected");System.out.println("PASS release policy: numeric versions, no downgrade, stable only, official origin, matching APK name/version, missing/invalid files and malicious URLs");
 }
}
