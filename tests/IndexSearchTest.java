import fi.cashmoney.*;import java.util.*;
public class IndexSearchTest {
 static void has(String query,String... expected){List<Yahoo.Asset> results=IndexCatalog.match(query);Set<String> symbols=new HashSet<>();for(Yahoo.Asset a:results){if(!a.type.equals("INDEX"))throw new AssertionError();symbols.add(a.symbol);}for(String s:expected)if(!symbols.contains(s))throw new AssertionError(query+" missing "+s);}
 public static void main(String[] args){
 has("S&P500","^GSPC");has("S&P 500","^GSPC");has("sp500","^GSPC");has("^GSPC","^GSPC");has("Nasdaq","^IXIC","^NDX");has("Nasdaq-100","^NDX");has("Helsinki","^OMXHPI");has("HEX","^OMXHPI");has("OMXHPI","^OMXHPI");has("Helsingin yleisindeksi","^OMXHPI");has("indeksit","^GSPC","^IXIC","^NDX","^OMXHPI");has("Indizes","^OMXHPI");
 if(!IndexCatalog.match("Apple").isEmpty()||!IndexCatalog.match("").isEmpty())throw new AssertionError();
 for(String t:new String[]{"INDEX","EQUITY","ETF","MUTUALFUND"})if(!Yahoo.supportedType(t))throw new AssertionError(t);
 if(Yahoo.supportedType("FUTURE")||Yahoo.supportedType("OPTION"))throw new AssertionError();
 try{Yahoo.Asset a=IndexCatalog.match("HEX").get(0);Yahoo.Asset b=Yahoo.Asset.from(a.json());if(!b.type.equals("INDEX")||!b.symbol.equals("^OMXHPI"))throw new AssertionError();}catch(Exception e){throw new AssertionError(e);}
 System.out.println("PASS: index names/aliases, localized queries, existing asset types, excluded derivatives and favorite persistence");
 }
}
