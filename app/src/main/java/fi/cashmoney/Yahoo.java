package fi.cashmoney;
import org.json.*;
import java.net.*;
import java.io.*;
import java.time.*;
import java.util.*;
public final class Yahoo {
 public static class Asset {
  public String symbol,name,exchange,type,currency="";
  public Asset(String s,String n,String e,String t){symbol=s;name=n;exchange=e;type=t;}
  public JSONObject json() throws JSONException{return new JSONObject().put("symbol",symbol).put("name",name).put("exchange",exchange).put("type",type);}
  public static Asset from(JSONObject j){return new Asset(j.optString("symbol"),j.optString("name"),j.optString("exchange"),j.optString("type"));}
 }
 public static JSONObject request(String path) throws Exception {
  Exception last=null;
  for(String host:new String[]{"query1.finance.yahoo.com","query2.finance.yahoo.com"}){
   HttpURLConnection c=null;
   try {c=(HttpURLConnection)new URL("https://"+host+path).openConnection();c.setConnectTimeout(12000);c.setReadTimeout(15000);c.setRequestProperty("User-Agent","Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 Chrome/131.0.0.0 Mobile Safari/537.36");c.setRequestProperty("Accept","application/json");int code=c.getResponseCode();if(code!=200)throw new IOException(code==429?"@rate_limit":"@http:"+code);try(InputStream in=c.getInputStream();ByteArrayOutputStream out=new ByteArrayOutputStream()){byte[] b=new byte[8192];int n;while((n=in.read(b))!=-1)out.write(b,0,n);return new JSONObject(out.toString("UTF-8"));}}
   catch(Exception e){last=e;}finally{if(c!=null)c.disconnect();}
  }throw last;
 }
 public static boolean supportedType(String type){return Arrays.asList("EQUITY","ETF","MUTUALFUND","INDEX").contains(type);}
 public static List<Asset> search(String q)throws Exception{
  List<Asset> out=new ArrayList<>(IndexCatalog.match(q));JSONArray a;
  try{a=request("/v1/finance/search?q="+URLEncoder.encode(q,"UTF-8")+"&quotesCount=30&newsCount=0&listsCount=0&enableFuzzyQuery=false").optJSONArray("quotes");}
  catch(Exception e){if(out.isEmpty())throw e;return out;}
  if(a==null)return out;Set<String> seen=new HashSet<>();for(Asset asset:out)seen.add(asset.symbol);
  for(int i=0;i<a.length();i++){JSONObject j=a.getJSONObject(i);String t=j.optString("quoteType");if(!supportedType(t))continue;String symbol=j.getString("symbol");if(!seen.add(symbol))continue;
   out.add(new Asset(symbol,j.optString("longname",j.optString("shortname",symbol)),j.optString("exchDisp",j.optString("exchange")),t));}return out;
 }
 public static JSONObject quote(String symbol)throws Exception{JSONObject data=request("/v8/finance/chart/"+URLEncoder.encode(symbol,"UTF-8")+"?range=10y&interval=1d&events=div%2Csplits");data.put("_cashmoneyDividendsRequested",true);return data;}
 public static class Price {
  public double dividendYield=Double.NaN;public double current;public double[] changes;public String currency,date;public long timestamp;
 }
 public static Price parse(JSONObject root)throws Exception{
  JSONObject chart=root.getJSONObject("chart");JSONArray results=chart.optJSONArray("result");if(results==null||results.length()==0)throw new IOException("@no_quote");
  JSONObject result=results.getJSONObject(0),meta=result.getJSONObject("meta");JSONArray ts=result.getJSONArray("timestamp"),cl=result.getJSONObject("indicators").getJSONArray("quote").getJSONObject(0).getJSONArray("close");
  ZoneId zone;try{zone=ZoneId.of(meta.optString("exchangeTimezoneName","UTC"));}catch(Exception e){zone=ZoneOffset.UTC;}
  List<LocalDate> dates=new ArrayList<>();List<Double> vals=new ArrayList<>();
  for(int i=0;i<Math.min(ts.length(),cl.length());i++)if(!cl.isNull(i)&&Double.isFinite(cl.optDouble(i))&&cl.optDouble(i)>0){dates.add(Instant.ofEpochSecond(ts.getLong(i)).atZone(zone).toLocalDate());vals.add(cl.getDouble(i));}
  if(vals.isEmpty())throw new IOException("@no_history");
  Price p=new Price();p.current=meta.optDouble("regularMarketPrice",vals.get(vals.size()-1));p.timestamp=meta.optLong("regularMarketTime",ts.getLong(ts.length()-1));LocalDate day=Instant.ofEpochSecond(p.timestamp).atZone(zone).toLocalDate();
  double previous=Changes.before(dates,vals,day.minusDays(1));if(!Double.isFinite(previous))previous=meta.optDouble("previousClose",meta.optDouble("chartPreviousClose",Double.NaN));
  p.dividendYield=DividendYield.calculate(result,root.optBoolean("_cashmoneyDividendsRequested",false),p.current,zone,p.timestamp,dates.get(0));p.changes=Changes.calculate(dates,vals,p.current,previous,day);p.currency=meta.optString("currency","");p.date=Instant.ofEpochSecond(p.timestamp).atZone(zone).format(java.time.format.DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm"));return p;
 }
}
