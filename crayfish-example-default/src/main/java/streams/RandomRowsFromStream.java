package streams;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

/**
 * Created by ashindarev on 30.06.17.
 */
public class RandomRowsFromStream {

    private static final Random random = new Random();

    public static void main(String[] args) {
        List<String> list = new ArrayList<>();
        for (int i=0; i<100; i++) {
            list.add("Str:"+i);
        }
        System.out.println(list);
        System.out.println(random.ints(2, 0, list.size())
                                 .distinct()
                                 .mapToObj(i -> list.get(i))
                                 .collect(Collectors.toList()));

    }

}
