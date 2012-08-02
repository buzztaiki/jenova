import com.github.buzztaiki.jenova.Jenova;

@Jenova
public class Example {
    public static void main(String[] args) throws Exception {
        new fn<Integer, String>(){{
            return Integer.toString(_);
        }};
    }
}
