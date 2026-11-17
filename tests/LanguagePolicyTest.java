import java.util.*;import fi.cashmoney.LanguagePolicy;
public class LanguagePolicyTest {
 static void check(String tag,String expected){Locale l=LanguagePolicy.resolve(Locale.forLanguageTag(tag));if(!l.getLanguage().equals(expected))throw new AssertionError(tag+" -> "+l);}
 public static void main(String[]args){for(String t:new String[]{"fi","fi-FI"})check(t,"fi");for(String t:new String[]{"de","de-DE","de-CH","de-AT"})check(t,"de");for(String t:new String[]{"en","en-US","en-GB","sv-FI","fr-CH","it","nb-NO","ja","ar","zh-CN","und"})check(t,"en");if(!LanguagePolicy.resolve(null).equals(Locale.ENGLISH))throw new AssertionError();System.out.println("PASS: Finnish, German variants, English and unsupported-language fallback");}
}
