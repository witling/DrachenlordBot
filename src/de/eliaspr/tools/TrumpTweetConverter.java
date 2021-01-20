package de.eliaspr.tools;

import de.eliaspr.json.JSONArray;
import de.eliaspr.json.JSONObject;
import de.eliaspr.json.JSONParser;
import de.eliaspr.json.JSONValueType;

import java.io.*;

public class TrumpTweetConverter {

    public static void main(String[] args) throws IOException {
        File src = new File("Z:/tweets_01-08-2021.csv");
        File dest = new File("C:/Space/DrachenlordBot/export/assets/drache/trump.txt");
        FileWriter destWriter = new FileWriter(dest);

        BufferedReader br = new BufferedReader(new FileReader(src));
        String line;
        while((line = br.readLine()) != null) {
            if(line.isEmpty())
                continue;
            if(Character.isDigit(line.charAt(0))) {
                line = line.substring(line.indexOf(',') + 1);
                if(line.charAt(0) == '"') {
                    line = line.substring(1);
                    int i = line.indexOf('"');
                    if(i > 0) {
                        line = line.substring(0, i);
                        destWriter.write(line);
                        destWriter.write('\n');
                    }
                }
            }
        }

        destWriter.close();
    }

}
