import java.util.Comparator;
import java.util.Map;

public class ValueComparator implements Comparator {
    Map base;

    public ValueComparator(Map base) {
        this.base = base;
    }

	@Override
    public int compare(Object a, Object b) {
        if ((long)base.get(a) <= (long)base.get(b)) {
            return -1;
        } else {
            return 1;
        } // returning 0 would merge keys
    }

}