package fi.cashmoney;
import java.time.*;
import java.util.*;
public final class Changes {
 public static double percent(double current,double base){return Double.isFinite(current)&&Double.isFinite(base)&&base>0?(current/base-1)*100:Double.NaN;}
 public static double before(List<LocalDate> dates,List<Double> values,LocalDate target){for(int i=dates.size()-1;i>=0;i--)if(!dates.get(i).isAfter(target))return values.get(i);return Double.NaN;}
 public static double[] calculate(List<LocalDate> dates,List<Double> values,double current,double previous,LocalDate date){return new double[]{percent(current,previous),percent(current,before(dates,values,date.minusWeeks(1))),percent(current,before(dates,values,date.minusMonths(1))),percent(current,before(dates,values,LocalDate.of(date.getYear(),1,1).minusDays(1))),percent(current,before(dates,values,date.minusYears(3))),percent(current,before(dates,values,date.minusYears(5)))};}
}
