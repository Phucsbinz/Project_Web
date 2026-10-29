package com.utetra.identity;
import java.text.Normalizer;
import java.util.*;

/** Naming for explicitly provisioned test identities, never for real delivery/SMTP addresses. */
public final class TestAccountEmails {
 private TestAccountEmails(){}
 public static boolean isTest(String email){String value=email==null?"":email.toLowerCase(Locale.ROOT);return value.endsWith(".test")||value.endsWith(".example");}
 public static String email(String name,String role){
  String normalized=Normalizer.normalize(Objects.toString(name,"").replace('Đ','D').replace('đ','d'),Normalizer.Form.NFD).replaceAll("\\p{M}+","").toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9\\s]","").trim();
  String[] words=normalized.isBlank()?new String[]{"taikhoan"}:normalized.split("\\s+");StringBuilder stem=new StringBuilder(words[words.length-1]);for(int i=0;i<words.length-1;i++)stem.append(words[i].charAt(0));if(stem.length()>40)stem.setLength(40);
  String label=switch(role){case "ROLE_ADMIN"->"admin";case "ROLE_MANAGER"->"manager";case "ROLE_VENDOR"->"vendor";case "ROLE_SHIPPER"->"shipper";default->"guest";};
  return stem+"."+label+"@example.test";
 }
}
