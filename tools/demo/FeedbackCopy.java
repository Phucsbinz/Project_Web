import com.fasterxml.jackson.databind.JsonNode;
/** Product-specific, explicitly simulated feedback; never used for real customer content. */
class FeedbackCopy {
 static String render(JsonNode drink,boolean comment,int sample,String size,int sugar,int ice) {
  var notes=drink.path("tasting_notes");if(notes.size()!=3)throw new IllegalArgumentException("Missing product tasting notes");
  int n=Math.floorMod(sample,3);String taste=notes.get(n).asText();
  String choice="Size "+size+", "+sugar+"% đường và "+ice+"% đá.";
  String content=comment?(new String[]{"Mình chọn ","Lần này mình thử ","Mình vừa thử "}[n]+drink.path("name").asText()+". "+taste+" "+choice):taste+" "+choice;
  int length=content.codePointCount(0,content.length());if(length<50||length>1000)throw new IllegalArgumentException("Invalid demo feedback length");return content;
 }
}
