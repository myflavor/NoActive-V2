package cn.myflv.noactive.core.utils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class BinderUtil {
    private final static String LOG_DIR = "/dev/binderfs/binder_logs";
    private final static File TRANSACTIONS = new File(LOG_DIR, "transactions");
    private final static Pattern PATTERN = Pattern.compile(".*?to\\s([\\d]*).*");

    public static List<Integer> getPidList() {
        List<Integer> list = new ArrayList<>();
        try {
            BufferedReader bufferedReader = new BufferedReader(new FileReader(TRANSACTIONS));
            String line;
            while ((line = bufferedReader.readLine()) != null) {
                String finalLine = line.trim();
                if (!finalLine.contains("pending transaction")) {
                    continue;
                }
                Matcher matcher = PATTERN.matcher(finalLine);
                if (!matcher.matches()) {
                    continue;
                }
                String pid = matcher.group(1);
                if (pid == null) {
                    continue;
                }
                list.add(Integer.parseInt(pid));
            }
        } catch (Throwable throwable) {
            System.out.println(throwable.getMessage());
        }
        return list;
    }

}
