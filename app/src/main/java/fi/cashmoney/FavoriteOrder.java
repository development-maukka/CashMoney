package fi.cashmoney;
import java.util.List;
public final class FavoriteOrder {
 /** insertion is a gap in the original list, from zero through size inclusive. */
 public static <T> boolean move(List<T> items,T item,int insertion){
  int from=items.indexOf(item);if(from<0)return false;
  int gap=Math.max(0,Math.min(items.size(),insertion));int to=gap>from?gap-1:gap;
  if(from==to)return false;items.remove(from);items.add(to,item);return true;
 }
}
