import fi.cashmoney.FavoriteOrder;import java.util.*;
public class FavoriteOrderTest {
 static List<String> list(){return new ArrayList<>(Arrays.asList("AAPL","VWCE.DE","HARVIA.HE","NESN.SW"));}
 static void eq(List<String> actual,String... expected){if(!actual.equals(Arrays.asList(expected)))throw new AssertionError(actual.toString());}
 public static void main(String[] args){
 List<String> a=list();if(!FavoriteOrder.move(a,"AAPL",4))throw new AssertionError();eq(a,"VWCE.DE","HARVIA.HE","NESN.SW","AAPL");
 if(!FavoriteOrder.move(a,"AAPL",0))throw new AssertionError();eq(a,"AAPL","VWCE.DE","HARVIA.HE","NESN.SW");
 FavoriteOrder.move(a,"VWCE.DE",3);eq(a,"AAPL","HARVIA.HE","VWCE.DE","NESN.SW");
 FavoriteOrder.move(a,"VWCE.DE",1);eq(a,"AAPL","VWCE.DE","HARVIA.HE","NESN.SW");
 if(FavoriteOrder.move(a,"VWCE.DE",1)||FavoriteOrder.move(a,"VWCE.DE",2)||FavoriteOrder.move(a,"MISSING",0))throw new AssertionError();eq(a,"AAPL","VWCE.DE","HARVIA.HE","NESN.SW");
 FavoriteOrder.move(a,"NESN.SW",-10);eq(a,"NESN.SW","AAPL","VWCE.DE","HARVIA.HE");
 FavoriteOrder.move(a,"NESN.SW",99);eq(a,"AAPL","VWCE.DE","HARVIA.HE","NESN.SW");
 List<String> single=new ArrayList<>(Arrays.asList("AAPL"));if(FavoriteOrder.move(single,"AAPL",1))throw new AssertionError();eq(single,"AAPL");
 System.out.println("PASS: move to start/end, adjacent up/down, no-op gaps, absent security and boundaries; no entries lost or duplicated");
 }
}
