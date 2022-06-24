package games.twinhead.simplegames.misc;

public class Util {


    public static String formatString(String string){
        String[] parts = string.split("_");
        String finalString = "";
        for(String part: parts){
            part = part.toLowerCase();
            finalString = finalString.concat(part.substring(0, 1).toUpperCase().concat(part.substring(1).toLowerCase())).concat(" ");
        }

        return finalString;
    }
}
