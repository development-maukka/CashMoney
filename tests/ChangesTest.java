import fi.cashmoney.*;import java.time.*;import java.util.*;import org.json.*;
public class ChangesTest {
 static void eq(double a,double b){if(Math.abs(a-b)>0.000001||!Double.isFinite(a))throw new AssertionError(a+" != "+b);}
 public static void main(String[] args)throws Exception{
 List<LocalDate>d=Arrays.asList(LocalDate.parse("2026-09-30"),LocalDate.parse("2026-10-23"),LocalDate.parse("2026-10-29"),LocalDate.parse("2026-10-30"));List<Double>v=Arrays.asList(80d,90d,100d,110d);
 double[] c=Changes.calculate(d,v,110,100,LocalDate.parse("2026-10-30"));eq(c[0],10);eq(c[1],22.2222222222);eq(c[2],37.5);
 eq(Changes.before(d,v,LocalDate.parse("2026-10-25")),90);
 if(!Double.isNaN(Changes.before(d,v,LocalDate.parse("2026-09-01"))))throw new AssertionError("Missing history");
 if(!Double.isNaN(Changes.percent(100,0)))throw new AssertionError("Zero denominator");
 eq(Changes.percent(90,100),-10);
 String fixture="{\"chart\":{\"result\":[{\"meta\":{\"regularMarketPrice\":110,\"regularMarketTime\":1793368800,\"currency\":\"EUR\",\"exchangeTimezoneName\":\"Europe/Helsinki\"},\"timestamp\":[1790848800,1792753200,1793271600,1793368800],\"indicators\":{\"quote\":[{\"close\":[80,90,100,110]}]}}],\"error\":null}}";
 Yahoo.Price price=Yahoo.parse(new JSONObject(fixture));eq(price.current,110);eq(price.changes[0],10);if(!price.currency.equals("EUR"))throw new AssertionError();
 JSONObject bad=new JSONObject("{\"chart\":{\"result\":null,\"error\":{\"code\":\"Not Found\"}}}");boolean failed=false;try{Yahoo.parse(bad);}catch(Exception e){failed=true;}if(!failed)throw new AssertionError("Must reject empty chart");
 List<LocalDate> yd=Arrays.asList(LocalDate.parse("2025-12-30"),LocalDate.parse("2026-01-02"),LocalDate.parse("2026-10-30"));
 double[] yc=Changes.calculate(yd,Arrays.asList(100d,105d,125d),125,120,LocalDate.parse("2026-10-30"));eq(yc[3],25);
 if(!Double.isNaN(c[3]))throw new AssertionError("YTD must be missing without prior year close");
 double[] jan=Changes.calculate(yd,Arrays.asList(100d,105d,125d),105,100,LocalDate.parse("2026-01-02"));eq(jan[3],5);
 List<LocalDate> longDates=Arrays.asList(LocalDate.parse("2021-10-29"),LocalDate.parse("2023-10-30"),LocalDate.parse("2026-10-29"),LocalDate.parse("2026-10-30"));
 double[] longChanges=Changes.calculate(longDates,Arrays.asList(50d,100d,140d,150d),150,140,LocalDate.parse("2026-10-30"));eq(longChanges[4],50);eq(longChanges[5],200);
 double[] shortHistory=Changes.calculate(longDates.subList(1,4),Arrays.asList(100d,140d,150d),150,140,LocalDate.parse("2026-10-30"));eq(shortHistory[4],50);if(!Double.isNaN(shortHistory[5]))throw new AssertionError("New security must not show a fabricated five-year change");
 if(!Double.isNaN(c[4])||!Double.isNaN(c[5]))throw new AssertionError("Old short cache should show missing long-term comparisons");
 List<LocalDate> leapDates=Arrays.asList(LocalDate.parse("2019-02-28"),LocalDate.parse("2021-02-26"),LocalDate.parse("2024-02-29"));
 double[] leap=Changes.calculate(leapDates,Arrays.asList(50d,80d,100d),100,90,LocalDate.parse("2024-02-29"));eq(leap[4],25);eq(leap[5],100);
 double[] decline=Changes.calculate(longDates,Arrays.asList(200d,100d,90d,80d),80,90,LocalDate.parse("2026-10-30"));eq(decline[4],-20);eq(decline[5],-60);
 System.out.println("PASS: three/five-year cumulative returns, weekend close, leap day, missing history, old cache and negative returns");
 System.out.println("PASS: daily, weekly, monthly, weekend, missing history, negative change, zero base, chart parsing and error response");
 }
}
