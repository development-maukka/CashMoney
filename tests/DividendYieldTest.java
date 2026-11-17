import fi.cashmoney.*;import java.time.*;import org.json.*;import java.nio.file.*;
public class DividendYieldTest {
 static final ZoneId Z=ZoneOffset.UTC;static final LocalDate DAY=LocalDate.of(2026,11,9),FIRST=LocalDate.of(2016,1,1);static final long NOW=DAY.atTime(20,0).toEpochSecond(ZoneOffset.UTC);
 static long time(LocalDate day){return day.atTime(12,0).toEpochSecond(ZoneOffset.UTC);}
 static JSONObject base(String type,String currency)throws Exception{return new JSONObject().put("meta",new JSONObject().put("instrumentType",type).put("currency",currency)).put("events",new JSONObject().put("dividends",new JSONObject()));}
 static void div(JSONObject r,String id,LocalDate date,double value)throws Exception{r.getJSONObject("events").getJSONObject("dividends").put(id,new JSONObject().put("date",time(date)).put("amount",value));}
 static double pct(JSONObject r,double price){return DividendYield.calculate(r,true,price,Z,NOW,FIRST);}
 static void eq(double a,double b){if(!Double.isFinite(a)||Math.abs(a-b)>1e-7)throw new AssertionError(a+" != "+b);}
 static void missing(double a){if(!Double.isNaN(a))throw new AssertionError("Expected unavailable, got "+a);}
 public static void main(String[] args)throws Exception{
  JSONObject r=base("EQUITY","USD");for(int i=0;i<4;i++)div(r,"q"+i,DAY.minusMonths(i*3+1),1);eq(pct(r,100),4);
  div(r,"cutoff",DAY.minusYears(1),99);div(r,"future",DAY.plusDays(1),99);div(r,"older",DAY.minusYears(2),99);eq(pct(r,100),4);
  div(r,"today",DAY,1);eq(pct(r,100),5);
  missing(DividendYield.calculate(r,false,100,Z,NOW,FIRST));missing(DividendYield.calculate(r,true,100,Z,NOW,DAY.minusMonths(6)));missing(pct(r,0));missing(pct(r,Double.NaN));
  JSONObject no=base("EQUITY","USD");missing(pct(no,100));no.remove("events");missing(pct(no,100));
  JSONObject old=base("EQUITY","EUR");div(old,"old",DAY.minusYears(2),1);eq(pct(old,100),0);
  JSONObject index=base("INDEX","USD");div(index,"cash",DAY,1);missing(pct(index,100));
  JSONObject fund=base("ETF","EUR");div(fund,"cash",DAY.minusDays(5),2);eq(pct(fund,80),2.5);
  JSONObject pence=base("EQUITY","GBp");div(pence,"cash",DAY,4);eq(pct(pence,100),4);
  pence.getJSONObject("events").getJSONObject("dividends").getJSONObject("cash").put("currency","GBP").put("amount",0.04);eq(pct(pence,100),4);
  pence.getJSONObject("events").getJSONObject("dividends").getJSONObject("cash").put("currency","USD");missing(pct(pence,100));
  JSONObject bad=base("EQUITY","USD");div(bad,"bad",DAY,-1);missing(pct(bad,100));bad.getJSONObject("events").getJSONObject("dividends").getJSONObject("bad").remove("date");missing(pct(bad,100));
  r.getJSONObject("events").put("splits",new JSONObject().put("split",new JSONObject().put("numerator",4).put("denominator",1)));eq(pct(r,100),5);
  LocalDate leapDay=LocalDate.of(2024,2,29);long leapNow=leapDay.atTime(20,0).toEpochSecond(ZoneOffset.UTC);JSONObject leap=base("EQUITY","USD");div(leap,"cutoff",LocalDate.of(2023,2,28),99);div(leap,"within",LocalDate.of(2023,3,1),2);eq(DividendYield.calculate(leap,true,100,Z,leapNow,FIRST),2);
  System.out.println("PASS: trailing sum, cutoff, future exclusion, leap year, missing vs zero, short/old cache, indices, funds, currency units, invalid data and no double split adjustment");
  for(String symbol:new String[]{"AAPL","HARVIA.HE","VOD.L","GSPC"}){
   Path path=Paths.get("tooling/live/"+symbol+".json");if(!Files.exists(path))continue;
   JSONObject root=new JSONObject(new String(Files.readAllBytes(path),"UTF-8")).put("_cashmoneyDividendsRequested",true);Yahoo.Price p=Yahoo.parse(root);
   if(symbol.equals("GSPC"))missing(p.dividendYield);else if(!Double.isFinite(p.dividendYield)||p.dividendYield<=0||p.dividendYield>50)throw new AssertionError(symbol+" "+p.dividendYield);
   System.out.println("Live chart parse "+symbol+": "+p.dividendYield);
   root.remove("_cashmoneyDividendsRequested");missing(Yahoo.parse(root).dividendYield);
  }
 }
}
