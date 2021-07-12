package xyz.lixiangyu.source.jdk.java.lang;

import org.junit.Test;

/**
 * @author lixiangyu
 */
public class ObjectTest {
    @Test
    public void getClassTest() {
        Number number = new Integer(1);
        Class<? extends Number> clazz = number.getClass();
        Class<?> clazz1 = number.getClass();
        // 这种写法是错误的
        // Class<Integer> clazz2 = number.getClass();
        System.out.println(clazz);
    }
}
