package fi.cashmoney;
import org.json.*;
import java.time.*;
import java.util.*;
/** Gross trailing cash dividends divided by the current price, using Yahoo ex-dividend dates. */
public final class DividendYield {
 private static String major(String currency){if(currency.equals("GBp")||currency.equals("GBX"))return "GBP";if(currency.equals("ZAc"))return "ZAR";if(currency.equals("ILA"))return "ILS";return currency;}
 private static double unit(String currency){return currency.equals("GBp")||currency.equals("GBX")||currency.equals("ZAc")||currency.equals("ILA")?0.01:1;}
 public static double calculate(JSONObject result,boolean requested,double price,ZoneId zone,long quoteTime,LocalDate firstDate){
  if(!requested||!Double.isFinite(price)||price<=0||quoteTime<=0)return Double.NaN;
  try{
   JSONObject meta=result.getJSONObject("meta");String type=meta.optString("instrumentType"),currency=meta.optString("currency");
   if(!Arrays.asList("EQUITY","ETF","MUTUALFUND").contains(type)||currency.isEmpty())return Double.NaN;
   LocalDate day=Instant.ofEpochSecond(quoteTime).atZone(zone).toLocalDate(),cutoff=day.minusYears(1);
   if(firstDate==null||firstDate.isAfter(cutoff))return Double.NaN;
   JSONObject events=result.optJSONObject("events"),dividends=events==null?null:events.optJSONObject("dividends");
   // An omitted/empty event list does not establish a confirmed zero dividend yield.
   if(dividends==null||dividends.length()==0)return Double.NaN;
   double sum=0;Iterator<String> keys=dividends.keys();
   while(keys.hasNext()){
    JSONObject dividend=dividends.optJSONObject(keys.next());if(dividend==null)return Double.NaN;
    long time=dividend.optLong("date",-1);if(time<0)return Double.NaN;
    LocalDate date=Instant.ofEpochSecond(time).atZone(zone).toLocalDate();
    if(!date.isAfter(cutoff)||date.isAfter(day)||time>quoteTime)continue;
    double amount=dividend.optDouble("amount",Double.NaN);if(!Double.isFinite(amount)||amount<0)return Double.NaN;
    // Yahoo normally supplies chart dividends in the quote unit. Honour an explicit currency when present.
    String cashCurrency=dividend.optString("currency",currency);if(!major(cashCurrency).equals(major(currency)))return Double.NaN;
    sum+=amount*unit(cashCurrency)/unit(currency);
   }
   double yield=sum/price*100;return Double.isFinite(yield)?yield:Double.NaN;
  }catch(Exception e){return Double.NaN;}
 }
}
