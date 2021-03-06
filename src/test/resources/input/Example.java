import com.github.buzztaiki.jenova.Jenova;
import com.github.buzztaiki.jenova.Lambda;
import com.google.common.base.Function;
import com.google.common.collect.Lists;
import java.util.List;

@Jenova(
    @Lambda(name="fn", type=Function.class)
)
public class Example {
    public List<String> transform(List<Integer> l) {
        l = Lists.transform(l, new fn<Integer, Integer>() {{
            return _1;
        }});
        return Lists.transform(l, new fn<Integer, String>() {{
            return Integer.toString(_1 * 2);
        }});
    }
}
