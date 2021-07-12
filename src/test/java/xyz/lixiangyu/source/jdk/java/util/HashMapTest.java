package xyz.lixiangyu.source.jdk.java.util;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * @author lixiangyu
 */
public class HashMapTest {
    private Map<Integer, Integer> map;

    @Before
    public void init() {
        map = new HashMap<>();
    }

    @After
    public void destroy() {
        map = null;
    }

    @Test
    public void keySetTest() {
        Set<Integer> set = map.keySet();

        map.put(1, 1);
        map.put(2, 1);

        System.out.println(map.getMapInfo(true));

        System.out.println(map);
    }
}
