package fi.cashmoney;
import java.text.Normalizer;
import java.util.*;
/** Common index aliases make familiar local names searchable without knowing a Yahoo symbol. */
public final class IndexCatalog {
 private static final String[][] ENTRIES={
  {"^GSPC","S&P 500","SNP","sp500|s&p 500|s&p500|gspc|^gspc|standard and poors"},
  {"^IXIC","Nasdaq Composite","NASDAQ","nasdaq|nasdaq composite|ixic|^ixic"},
  {"^NDX","Nasdaq-100","NASDAQ","nasdaq|nasdaq100|nasdaq-100|ndx|^ndx"},
  {"^OMXHPI","OMX Helsinki PI","Helsinki","helsinki|hex|omxhpi|^omxhpi|omx helsinki|helsingin yleisindeksi"}
 };
 private static String normalize(String s){return Normalizer.normalize(s.toLowerCase(Locale.ROOT),Normalizer.Form.NFD).replaceAll("\\p{M}+","").replaceAll("[^a-z0-9]","");}
 public static List<Yahoo.Asset> match(String query){String q=normalize(query);List<Yahoo.Asset> result=new ArrayList<>();if(q.isEmpty())return result;
  boolean all=Arrays.asList("index","indices","indexes","indeksi","indeksit","indizes").contains(q);
  for(String[] e:ENTRIES){boolean matches=all||normalize(e[1]).contains(q);for(String alias:e[3].split("\\|"))if(normalize(alias).contains(q))matches=true;if(matches)result.add(new Yahoo.Asset(e[0],e[1],e[2],"INDEX"));}return result;
 }
}
