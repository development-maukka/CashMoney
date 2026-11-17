package fi.cashmoney;
import java.util.Locale;
public final class LanguagePolicy {
 public static Locale resolve(Locale primary){String lang=primary==null?"":primary.getLanguage();return "fi".equals(lang)?Locale.forLanguageTag("fi-FI"):"de".equals(lang)?Locale.forLanguageTag("de-CH"):Locale.ENGLISH;}
}
