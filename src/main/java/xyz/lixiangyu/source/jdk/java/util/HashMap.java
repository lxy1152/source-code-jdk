/*
 * Copyright (c) 1997, 2017, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 */

package java.util;

import java.io.IOException;
import java.io.InvalidObjectException;
import java.io.PrintStream;
import java.io.Serializable;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;

import sun.misc.SharedSecrets;

/**
 * <p>
 * 哈希表是基于 {@link Map} 接口实现的, 它对所有操作都提供了实现, 同时允许键和值为
 * {@code null}(除了非同步和键值允许 {@code null} 以外, {@link HashMap} 和
 * {@link Hashtable} 是一致的).
 * </p>
 *
 * <p>
 * 如果元素都是均匀放在桶中的, 那么 {@link #get(Object)} 和
 * {@link #put(Object, Object)} 操作的时间复杂度可以认为是 O(1). 另外由于迭代
 * 器遍历所需的时间与 {@linkplain #capacity() 桶的数量} 加
 * {@linkplain #size 键值对的数量} 成正比, 所以如果是需要考虑遍历性能的场景, 那么
 * 初始容量不应该设置太大(或者加载银子不应该设置太小).
 * </p>
 *
 * <p>
 * {@link HashMap} 有两个重要的参数影响它的性能, 一个是<b>初始容量</b>, 一个是
 * <b>加载因子</b>. 容量指的是哈希表中桶的数量, 初始容量就是指哈希表在创建时指定的
 * 容量. 加载因子是一个用来衡量哈希表满的程度的参数, 当前容量和加载因子相乘可以得到
 * 一个阈值. 如果当前的 {@link #size} 大于这个阈值, 那么 {@link HashMap} 就
 * 会自动的进行扩容(内部结构会有所改变). 在扩容后, 新的哈希表的桶的数量是原来数量的
 * 两倍.
 * </p>
 *
 * <p>
 * 加载因子默认取值为 0.75, 这个值可以很好的在时间和空间上达到平衡. 对于
 * {@link HashMap} 类中的大部分操作, 如果值更大, 空间开销会降低, 但是会增加查找
 * 所需的成本. 在设置初始容量时, 应考虑哈希表中预期的键值对数量与加载因子的大小, 尽
 * 量避免过多的进行 {@code rehash} 操作. 由于阈值是通过当前容量和加载因子相乘得到
 * 的, 那么可以得到, 容量 = 阈值 / 加载因子. 进一步可以根据预估的最大阈值(这个哈希
 * 表估计的最大键值对数量)得到: 最大容量 = 最大阈值 / 加载因子, 因此如果初始容量比
 * 最大容量还要大, 那么就永远不会发生 {@code rehash} 操作. 所以如果这个
 * {@link HashMap} 中要保存非常多的键值对, 那么定义一个比较大的初始容量, 往往比让
 * 哈希表自己来扩容要好得多.
 * </p>
 *
 * <p>
 * {@link HashMap} 的迭代器是支持 {@code fail-fast} 机制的, 也就是说在迭代器
 * 创建后, 除非调用迭代器提供的 {@link Iterator#remove()} 操作, 其他任何导致哈
 * 希表结构发生变化的操作(结构修改是指增删元素, 修改已有键的值不算是结构修改)都是不
 * 被允许的. 如果开发人员真的这么操作了, 那么哈希表将直接抛出
 * {@link ConcurrentModificationException} 异常. 因此哈希表的迭代器在面对并
 * 发修改时将快速的抛出异常, 而不是任由这种不确定行为继续进行. 但 {@code fail-fast}
 * 机制在不同步并发修改的情况下不能保证程序的正常执行, 所以不要过分依赖这种机制, 把它
 * 作为一种检测 bug 的手段就可以了.
 * </p>
 * <p>
 * 注意:
 * <ul>
 *     <li>
 *         {@link HashMap} 不保证键值对的顺序, 也不保证随着时间的推移, 已有键值
 *         对顺序不改变
 *     </li>
 *     <li>
 *         如果在哈希表中插入很多哈希值相同的元素, 那么哈希表的性能肯定会降低. 如果
 *         真的存在这种情况, 使键实现 {@link Comparable} 接口会有所帮助
 *     </li>
 *     <li>
 *         {@link HashMap} 类是线程不安全的, 如果至少有一个对哈希表的结构进行了
 *         修改, 那么必须要在外部进行同步. 在进行同步时, 可以指定一些封装好的对象,
 *         也可以使用现成的接口来获取一个线程安全的哈希表:<br>
 *         {@code Map m = Collections.synchronizedMap(new HashMap(...));}
 *     </li>
 *     <li>
 *         {@link HashMap} 是
 *         <a href="https://docs.oracle.com/javase/8/docs/technotes/guides/collections/index.html">
 *             Java 集合框架
 *         </a>
 *         中的一员
 *     </li>
 * </ul>
 *
 * <p>
 * 源码中 Implementation notes 部分的注释放到这里了:<br>
 * {@link HashMap} 通常是使用桶来进行存储的, 但是当桶变得特别大的时候, 它们会转换
 * 为{@linkplain TreeNode 红黑树节点} 来进行存储, 其结构和 {@link TreeMap}
 * 是类似的, 这种转换一般是当节点个数到达一个阈值时才会发生. 之所以使用红黑树是因为在
 * 数据量较大的情况下, 它支持快速查找. 由于一般情况下桶并不会很大, 所以对数量检查也会
 * 有所延迟.<br><br>
 * 两个树节点通常是根据它们的哈希值来进行排序, 但如果它们实现了 {@link Comparable}
 * 接口, 那么就会转而通过 {@link Comparable#compareTo(Object)} 方法来进行比较
 * 并排序, 详细实现见 {@link #comparableClassFor(Object)} 方法. 红黑树最坏的
 * 时间复杂度是 O(log n), 所以即使哈希值有很多冲突, 只要对象是可比较的, 那么这种转换
 * 就会使性能缓慢的下降. 如果这两种方法都不使用, 与不采取预防措施相比, 可能会浪费大约
 * 两倍的时间和空间.<br><br>
 * 由于树节点的大小通常是普通节点大小的两倍, 因此只有在链表长度大于一个阈值
 * ({@link #TREEIFY_THRESHOLD})时, 链表才会转换为红黑树. 当进行删除或者扩容操作
 * 导致链表长度减小后, 红黑树将重新退化为链表. 在哈希分布均匀的系统中, 树其实使用的很少.
 * 在理想情况以及随机哈希值的情况下, 链表长度是服从泊松分布的, 由于长度 8 的概率已经低
 * 到 0.00000006, 可以认为是不可能事件, 所以选取 8 作为阈值: <br>
 * 0:    0.60653066<br>
 * 1:    0.30326533<br>
 * 2:    0.07581633<br>
 * 3:    0.01263606<br>
 * 4:    0.00157952<br>
 * 5:    0.00015795<br>
 * 6:    0.00001316<br>
 * 7:    0.00000094<br>
 * 8:    0.00000006<br><br>
 * 树的根节点通常是第一个节点, 但有时(目前仅发生在 {@link Iterator#remove()}
 * 方法上)它的根可能在别的地方. 就算是在别的地方, 也依然可以通过
 * {@link TreeNode#root()}方法进行恢复.<br><br>
 * 类中所有 {@code private} 方法都接受哈希值作为参数(通常是由 {@code public}
 * 方法提供的), 这样在互相调用时就不需要重新计算哈希值了. 也有一部分方法支持传入
 * {@code tab},通常它表示当前的数组(在扩容时可能指旧的或者是新的数组).<br><br>
 * 当桶被树化、拆分或退化时, 将保持它们的相对访问顺序(即
 * {@linkplain Node#next Node.next} 的顺序), 以更好地保留局部性.<br><br>
 * 由于有类 {@link LinkedHashMap}, 所以链表模式与树模式之间的使用和转换非常复杂.
 * 所以在下方定义了一些钩子, 这些钩子方法定义为在插入、删除和访问时调用, 从而允许
 * {@link LinkedHashMap} 内部独立于这些机制.
 * </p>
 *
 * @param <K> 键的类型
 * @param <V> 值的类型
 * @author Doug Lea
 * @author Josh Bloch
 * @author Arthur van Hoff
 * @author Neal Gafter
 * @see Object#hashCode()
 * @see Collection
 * @see Map
 * @see TreeMap
 * @see Hashtable
 * @since 1.2
 */
public class HashMap<K, V> extends AbstractMap<K, V> implements Map<K, V>, Cloneable, Serializable {
    /* ---------------- 静态变量开始 -------------- */

    /**
     * 序列化 id
     */
    private static final long serialVersionUID = 362498820763181265L;

    /**
     * 默认的初始容量, 必须是 2^n. 为了强调, 下面的数值还特意写成了位运算, 而
     * 不是 16
     */
    static final int DEFAULT_INITIAL_CAPACITY = 1 << 4;

    /**
     * 最大容量值, 不要超过 2^30
     */
    static final int MAXIMUM_CAPACITY = 1 << 30;

    /**
     * 加载因子, 默认为 0.75
     */
    static final float DEFAULT_LOAD_FACTOR = 0.75f;

    /**
     * 当链表中的节点大于这个阈值后并且总键值对数量大于
     * {@link #MIN_TREEIFY_CAPACITY} 时, 会将链表转化为红黑树, 长度
     * 取 8 的原因见Implementation notes 部分的注释
     */
    static final int TREEIFY_THRESHOLD = 8;

    /**
     * 当树中的节点小于这个阈值后, 红黑树将退化为链表
     */
    static final int UNTREEIFY_THRESHOLD = 6;

    /**
     * 进行树化时的最小键值对数量, 如果数量不够, 那么就只会进行扩容而不是
     * 树化, 它的值应该至少是 {@link #TREEIFY_THRESHOLD} 的 4 倍
     */
    static final int MIN_TREEIFY_CAPACITY = 64;

    /* ---------------- 静态变量结束 -------------- */




    /* ---------------- 静态方法开始 -------------- */

    /**
     * <p>
     * 通过静态方法 {@link #hash(Object)} 可以得到某个键所对应的哈希值. 具体的
     * 计算方法为: 将键的高 16 位右移 16 位, 将右移后的高 16 位和原本的低 16 位
     * 进行异或, 最终就可以得到一个哈希值. 虽然 {@link HashMap} 使用拉链法作为
     * 哈希碰撞的解决方案, 但是仍可以在 {@link #hash(Object)} 函数上进行一定程
     * 度的优化, 来减少碰撞的可能性. 由于哈希表的容量一定是 2^n, 所以原本计算数组
     * 索引所使用的求余运算可以转换为与运算(这一部分将在后面进行叙述), 这样在表较小
     * 时, 就只有低位会参与与运算. 举个例子:<br>
     * 假设表大小为 16, 第一个键的哈希值是 1661711899, 第二个键的哈希值为
     * 1661580827. 索引计算公式为 (表大小 - 1) & 哈希值, 将它们转换为二进制:<br>
     * <pre>
     * 表大小: 16 - 1 = 15  ->  00000000 00000000 00000000 00001111
     * 哈希值: 1661711899   ->  01100011 00001011 10110110 00011011  ->  index = 1011 = 11
     * 哈希值: 1661580827   ->  01100011 00001001 10110110 00011011  ->  index = 1011 = 11
     * </pre>
     * 由于进行的是与运算, 所以 100 的从低到高 5-7 位将全部被忽略, 哈希值 100 所
     * 计算出的索引与哈希值 4 所计算出的索引位置是相同的. 如果通过高位向低位异或传播
     * 的方式来计算哈希值的话, 那么高位会参与到运算中, 减少了碰撞的可能性. 还是上面
     * 的例子:<br>
     * <pre>
     * 表大小: 16 - 1 = 15                       ->  00000000 00000000 00000000 00001111
     * 哈希值: 1661711899 ^ (1661711899 >> 16)   ->  01100011 00001011 11010101 00010000  ->  index = 0000
     * 哈希值: 1661580827 ^ (1661580827 >> 16)   ->  01100011 00001001 11010101 00010010  ->  index = 0010
     * </pre>
     * </p>
     *
     * <p>
     * 注: {@code h >>> 16} 这一步是将高 16 位移到低 16 位上, 同时因为使用的是
     * 无符号右移所以右移后的数字的高 16 位都是 0, 0 与任何数做亦或操作的结果还是自
     * 身, 因此最终的结果可以视为这个数高 16 位与低 16 位异或的结果. 举例:
     * <pre>
     *     h = hashCode()    = 11111111 11111111 11110000 11101010
     *       h >>> 16        = 00000000 00000000 11111111 11111111
     * hash = h ^ (h >>> 16) = 11111111 11111111 00001111 00010101
     * </pre>
     * </p>
     */
    static final int hash(Object key) {
        int h;
        return (key == null) ? 0 : (h = key.hashCode()) ^ (h >>> 16);
    }

    /**
     * 如果当前对象实现了 {@link Comparable} 接口, 就返回当前对象的 {@link Class}
     * 对象, 否则返回 {@code null}
     *
     * @param x 要分析的对象
     * @return null 或 x 的 {@link Class} 对象
     */
    static Class<?> comparableClassFor(Object x) {
        // 如果 x 没有实现 Comparable 接口, 则直接返回 null
        if (x instanceof Comparable) {
            Class<?> c;
            Type[] ts;
            Type[] as;
            Type t;
            ParameterizedType p;
            // 如果是字符串, 那么它一定是可比的, 直接返回
            if ((c = x.getClass()) == String.class) {
                return c;
            }
            // 返回直接实现的接口, 包含泛型参数
            if ((ts = c.getGenericInterfaces()) != null) {
                for (int i = 0; i < ts.length; ++i) {
                    // 如果满足以下条件就认为该对象是可比较的
                    // 1. 有泛型参数
                    t = ts[i];
                    boolean hasParameterizedType = t instanceof ParameterizedType;

                    // 2. 泛型参数实现了 Comparable 接口
                    p = ((ParameterizedType) t);
                    boolean implementationOfComparable = p.getRawType() == Comparable.class;

                    // 3. 重写了 compareTo 方法
                    as = p.getActualTypeArguments();
                    boolean overrideCompareTo = as != null && as.length == 1 && as[0] == c;

                    if (hasParameterizedType && implementationOfComparable && overrideCompareTo)
                        return c;
                }
            }
        }
        return null;
    }

    /**
     * 将两个对象进行比较, 如果这两个对象的 {@link Class} 对象相同, 就通过
     * {@link Comparable#compareTo(Object)} 方法进行比较, 否则返回 0
     *
     * @param kc {@link Class} 对象
     * @param k  第一个要比较的对象
     * @param x  第二个要比较的对象
     * @return 比较的结果
     */
    @SuppressWarnings({"rawtypes", "unchecked"})
    static int compareComparables(Class<?> kc, Object k, Object x) {
        if (x == null || x.getClass() != kc) {
            return 0;
        } else {
            return ((Comparable) k).compareTo(x);
        }
    }

    /**
     * 由于规定哈希表的容量必须是 2^n, 所以如果容量不满足要求, 那么需要将它转换为
     * 离这个数最近的一个 2^n. 以 {@code cap = 10} 举个例子:
     * <pre>
     * cap  =     10   = 00000000 00001010
     *  n   =  cap - 1 = 00000000 00001001
     *  n  |=  n >>> 1 = 00000000 00001001 | 00000000 00000100 = 00000000 00001101
     *  n  |=  n >>> 2 = 00000000 00001101 | 00000000 00000011 = 00000000 00001111
     *  n  |=  n >>> 4 = 00000000 00001111 | 00000000 00000000 = 00000000 00001111
     *  ... 因为和右移四位类似, 所以省略 8 和 16 的操作步骤 ...
     *      result     = 00000000 00001111 + 00000000 00000001 = 00000000 00010000 = 16
     * </pre>
     */
    static final int tableSizeFor(int cap) {
        int n = cap - 1;
        n |= n >>> 1;
        n |= n >>> 2;
        n |= n >>> 4;
        n |= n >>> 8;
        n |= n >>> 16;
        return (n < 0) ? 1 : (n >= MAXIMUM_CAPACITY) ? MAXIMUM_CAPACITY : n + 1;
    }

    /* ---------------- 静态方法结束 -------------- */




    /* ---------------- 静态内部类 Node 开始 -------------- */

    /**
     * 基础节点类
     *
     * @see Map.Entry
     */
    static class Node<K, V> implements Map.Entry<K, V> {
        /* ---------------- 成员变量开始 -------------- */

        /**
         * 哈希值
         */
        final int hash;

        /**
         * 键
         */
        final K key;

        /**
         * 值
         */
        V value;

        /**
         * 下一节点
         */
        Node<K, V> next;

        /* ---------------- 成员变量结束 -------------- */


        /* ---------------- 构造器开始 -------------- */

        /**
         * 默认构造器, 为上述四个变量指定初始值
         *
         * @param hash  哈希值
         * @param key   键
         * @param value 值
         * @param next  下一节点
         */
        Node(int hash, K key, V value, Node<K, V> next) {
            this.hash = hash;
            this.key = key;
            this.value = value;
            this.next = next;
        }

        /* ---------------- 构造器结束 -------------- */

        /* ---------------- Object 类重写方法开始 -------------- */

        /**
         * 重写 {@link Object#toString()} 方法, 输出 {@link #key}
         * 和{@link #value}
         *
         * @return 形如 key=value 样式的字符串
         */
        public final String toString() {
            return "{" + key + " = " + value + "}";
        }

        /**
         * 重写 {@link Object#hashCode()} 方法, 将键和值得哈希值进行
         * 异或得到的值作为哈希值
         *
         * @return 哈希值
         */
        public final int hashCode() {
            return Objects.hashCode(key) ^ Objects.hashCode(value);
        }

        /**
         * 重写 {@link Object#equals(Object)} 方法, 通过比较键和值判
         * 断两个节点是否相同
         *
         * @param o 另一个节点
         * @return 两节点是否相同
         */
        public final boolean equals(Object o) {
            if (o == this) {
                return true;
            }
            if (o instanceof Map.Entry) {
                Map.Entry<?, ?> e = (Map.Entry<?, ?>) o;
                if (Objects.equals(key, e.getKey()) && Objects.equals(value, e.getValue())) {
                    return true;
                }
            }
            return false;
        }

        /* ---------------- Object 类重写方法结束 -------------- */


        /* ---------------- get/set 方法开始 -------------- */

        public final K getKey() {
            return key;
        }

        public final V getValue() {
            return value;
        }

        public final V setValue(V newValue) {
            V oldValue = value;
            value = newValue;
            return oldValue;
        }

        /* ---------------- get/set 方法结束 -------------- */
    }

    /* ---------------- 静态内部类 Node 结束 -------------- */




    /* ---------------- 成员变量开始 -------------- */

    /**
     * 用于保存节点的数组, 它会在首次使用时进行扩容与初始化. 这个数组的长度应该
     * 是 2^n, 长度为 0 在某些情况下也是允许的
     */
    transient Node<K, V>[] table;

    /**
     * 当调用 {@link #entrySet()} 方法时, 这个变量会被赋值, 用于缓存
     */
    transient Set<Map.Entry<K, V>> entrySet;

    /**
     * 当前哈希表中保存的键值对数量
     */
    transient int size;

    /**
     * 这个变量用于记录哈希表结构发生变化的次数, 其中结构变化指的是某些
     * 操作修改了键值对的数量或是修改了哈希表的内部结构(比如 rehash).
     * 这个字段主要是用于迭代器的 {@code fail-fast} 机制
     *
     * @see ConcurrentModificationException
     */
    transient int modCount;

    /**
     * 容量 * 加载因子 = 阈值, 表示扩容的临界值, 当容量大于阈值时, 将
     * 进行扩容. 当数组为 null 时, 这个字段表示数组的初始容量
     *
     * @serial
     */
    int threshold;

    /**
     * 加载因子, 默认值为 0.75
     *
     * @serial
     */
    final float loadFactor;

    /* ---------------- 成员变量结束 -------------- */




    /* ---------------- 构造器开始 -------------- */

    /**
     * 根据指定的容量和加载因子创建一个新的哈希表
     *
     * @param initialCapacity 初始容量
     * @param loadFactor      加载因子
     * @throws IllegalArgumentException 如果容量为负或加载因子为负数
     */
    public HashMap(int initialCapacity, float loadFactor) {
        // 初始容量必须为正数
        if (initialCapacity < 0) {
            throw new IllegalArgumentException("Illegal initial capacity: " + initialCapacity);
        }
        // 如果初始容量大于 2^30, 那么使用 2^30 作为容量
        if (initialCapacity > MAXIMUM_CAPACITY) {
            initialCapacity = MAXIMUM_CAPACITY;
        }
        // 加载因子不是正数或者不是数字都抛出异常
        if (loadFactor <= 0 || Float.isNaN(loadFactor)) {
            throw new IllegalArgumentException("Illegal load factor: " + loadFactor);
        }
        // 赋值加载因子
        this.loadFactor = loadFactor;
        // 根据给定的容量计算一个满足要求的容量
        this.threshold = tableSizeFor(initialCapacity);
    }

    /**
     * 根据指定的容量创建一个新的哈希表, 加载因子默认取 0.75
     *
     * @param initialCapacity 初始容量
     * @throws IllegalArgumentException 如果初始容量为负
     */
    public HashMap(int initialCapacity) {
        this(initialCapacity, DEFAULT_LOAD_FACTOR);
    }

    /**
     * 根据默认值创建一个新的哈希表, 容量取 16, 加载因子取 0.75.
     */
    public HashMap() {
        this.loadFactor = DEFAULT_LOAD_FACTOR;
    }

    /**
     * 根据一个已有的映射对象创建哈希表, 容量保证可以容纳指定的映射,
     * 加载因子默认取 0.75
     *
     * @param m 一个映射
     * @throws NullPointerException 如果这个对象是 null
     */
    public HashMap(Map<? extends K, ? extends V> m) {
        this.loadFactor = DEFAULT_LOAD_FACTOR;
        putMapEntries(m, false);
    }

    /* ---------------- 构造器结束 -------------- */




    /* ---------------- public 方法开始 -------------- */

    /**
     * 返回哈希表中键值对的数量
     *
     * @return 哈希表中键值对的数量
     */
    @Override
    public int size() {
        return size;
    }

    /**
     * 如果哈希表中不存在键值对则返回 {@code true}, 否则返回 {@code false}
     *
     * @return 哈希表中是否存在键值对
     */
    @Override
    public boolean isEmpty() {
        return size == 0;
    }

    /**
     * 根据对应的键获取值, 如果没有相应的键那么会返回 null. 但是返回 null 并不一定就代表
     * 哈希表没有这个键, 也有可能这个键所对应的值本来就是 null. 使用
     * {@link #containsKey(Object)} 可以区分这两种情况. {@link #get(Object)}
     * 操作的流程图如下所示.<br><br>
     *
     * <img src="../../../../../../../resources/xyz/lixiangyu/source/jdk/java/util/hashmap/get操作流程图.jpg">
     * <br>
     *
     * @param key 键
     * @return 键对应的值
     * @see #put(Object, Object)
     * @see #getNode(int, Object)
     */
    @Override
    public V get(Object key) {
        // 根据哈希和键获取节点
        Node<K, V> e = getNode(hash(key), key);
        // 如果节点不为空则返回它的值
        return e == null ? null : e.value;
    }

    /**
     * 在哈希表中插入一个键值对, 如果键已存在, 那么它的值会被替换而不是新插入一个键值对.
     * {@link #put(Object, Object)} 操作的流程图如下所示.<br><br>
     *
     * <img src="../../../../../../../resources/xyz/lixiangyu/source/jdk/java/util/hashmap/put操作流程图.jpg">
     * <br>
     *
     * @param key   键
     * @param value 值
     * @return 这个键所对应得旧值, 如果不存在这个键, 那么会返回 null, 但是返回 null
     * 也可能代表键原本所对应的值就是 null
     * @see #get(Object)
     * @see #putVal(int, Object, Object, boolean, boolean)
     */
    public V put(K key, V value) {
        return putVal(hash(key), key, value, false, true);
    }

    /**
     * 将给定映射中键值对全部复制到这个哈希表中, 如果键已存在, 那么会替换它的值
     *
     * @param m 一个映射
     * @throws NullPointerException 如果这个映射是 null
     */
    public void putAll(Map<? extends K, ? extends V> m) {
        putMapEntries(m, true);
    }

    /**
     * 如果通过指定的键能查找到一个节点, 那么就会 {@code true}, 否则返回 {@code false}
     *
     * @param key 键
     * @return true: 如果包含键所对应的值; false: 不包含这个键所对应的值
     */
    public boolean containsKey(Object key) {
        return getNode(hash(key), key) != null;
    }

    /**
     * 根据指定的键在哈希表中删除键值对
     *
     * @param key 要删除键值对的键
     * @return 键所对应的值(可能为 null)
     */
    @Override
    public V remove(Object key) {
        Node<K, V> e = removeNode(hash(key), key, null, false, true);
        return e == null ? null : e.value;
    }

    /**
     * 根据给定的键和值移除键值对
     *
     * @param key   键
     * @param value 值
     * @return 是否成功删除
     */
    @Override
    public boolean remove(Object key, Object value) {
        return removeNode(hash(key), key, value, true, true) != null;
    }

    /**
     * 删除哈希表中所有的键值对, 调用这个方法后, 哈希表将完全清空
     */
    public void clear() {
        // 指向当前数组
        Node<K, V>[] tab = table;

        // 清空操作, 导致哈希表结构发生变化
        modCount++;

        // 如果哈希表有值
        if (tab != null && size > 0) {
            // 将 size 更新为 0
            size = 0;

            // 释放链表头节点
            for (int i = 0; i < tab.length; ++i) {
                tab[i] = null;
            }
        }
    }

    /**
     * 判断给定的值在哈希表中是否存在, 如果存在返回 {@code true}, 否则返回
     * {@code false}. 不排除同一个值可能对应多个键.
     *
     * @param value 要查找的值
     * @return 如果存在给定的值则返回 true, 否则返回 false
     */
    public boolean containsValue(Object value) {
        // 指向当前数组
        Node<K, V>[] tab = table;

        // 遍历哈希表
        if (tab != null && size > 0) {
            for (int i = 0; i < tab.length; ++i) {
                for (Node<K, V> e = tab[i]; e != null; e = e.next) {
                    V v = e.value;
                    if (v == value || (value != null && value.equals(v))) {
                        return true;
                    }
                }
            }
        }

        // 如果数组为空或者不存在对应的值则返回 false
        return false;
    }

    /**
     * 返回一个键组成的集合, 任何对哈希表键的修改, 都会被反映到它的上面. 如果在迭代
     * 过程中哈希表发生了变化(除了迭代器提供的 {@link Iterator#remove()} 方法),
     * 那么迭代器返回的结果是不确定的. 这个集合支持删除操作, 比如:
     * {@link Iterator#remove()}, {@link Set#remove(Object)},
     * {@link Set#removeAll(Collection)}, {@link Set#retainAll(Collection)},
     * {@link Set#clear()}. 但是不支持 {@link Set#add(Object)},
     * {@link Set#addAll(Collection)} 操作.
     *
     * @return 哈希表键的集合
     */
    public Set<K> keySet() {
        Set<K> ks = keySet;
        if (ks == null) {
            ks = new KeySet();
            keySet = ks;
        }
        return ks;
    }

    /**
     * 返回一个值组成的集合, 任何对哈希表值的修改, 都会被反映到它的上面. 如果在迭代
     * 过程中哈希表发生了变化(除了迭代器提供的 {@link Iterator#remove()} 方法),
     * 那么迭代器返回的结果是不确定的. 这个集合支持删除操作, 比如:
     * {@link Iterator#remove()}, {@link Collection#remove(Object)},
     * {@link Collection#removeAll(Collection)},
     * {@link Collection#retainAll(Collection)},
     * {@link Collection#clear()}. 但是不支持
     * {@link Collection#add(Object)},
     * {@link Collection#addAll(Collection)} 操作.
     *
     * @return 一个键组成的结合
     */
    public Collection<V> values() {
        Collection<V> vs = values;
        if (vs == null) {
            vs = new Values();
            values = vs;
        }
        return vs;
    }

    /**
     * 返回一个由键值对组合组成的集合, 任何对哈希表值的修改, 都会被反映到它的上面.
     * 如果在迭代过程中哈希表发生了变化(除了迭代器提供的 {@link Iterator#remove()},
     * 或者是 {@link java.util.Map.Entry#setValue(Object)} 方法),
     * 那么迭代器返回的结果是不确定的. 这个集合支持删除操作, 比如:
     * {@link Iterator#remove()}, {@link Set#remove(Object)},
     * {@link Set#removeAll(Collection)}, {@link Set#retainAll(Collection)},
     * {@link Set#clear()}. 但是不支持 {@link Set#add(Object)},
     * {@link Set#addAll(Collection)} 操作.
     *
     * @return 一个由键值对组合组成的集合
     */
    public Set<Map.Entry<K, V>> entrySet() {
        Set<Map.Entry<K, V>> es = entrySet;
        if (es == null) {
            entrySet = new EntrySet();
            es = entrySet;
        }
        return es;
    }

    /**
     * 获取某个键对应的值, 如果不存在则返回默认值
     *
     * @param key          键
     * @param defaultValue 默认值
     * @return 键对应的值或者是默认值
     */
    @Override
    public V getOrDefault(Object key, V defaultValue) {
        Node<K, V> e = getNode(hash(key), key);
        return e == null ? defaultValue : e.value;
    }

    /**
     * 在键不存在时做插入操作, 在键已存在时不进行修改
     *
     * @param key   键
     * @param value 值
     * @return 旧值
     */
    @Override
    public V putIfAbsent(K key, V value) {
        return putVal(hash(key), key, value, true, true);
    }

    /**
     * 将某个键所对应的值进行更新, 如果成功更新返回 {@code true}, 否则返回
     * {@code false}. 注意:<br>
     * 1. 这个方法会调用 {@link #afterNodeAccess(Node)} 钩子<br>
     * 2. 这个方法需要比较旧值是否相同
     *
     * @param key      键
     * @param oldValue 旧值
     * @param newValue 新值
     * @return 是否成功更新
     */
    @Override
    public boolean replace(K key, V oldValue, V newValue) {
        // 获取节点
        Node<K, V> e = getNode(hash(key), key);
        // 如果键所对应的节点不存在则直接返回, 否则会尝试替换值
        if (e != null) {
            if (Objects.equals(e.value, oldValue)) {
                e.value = newValue;
                // 调用钩子
                afterNodeAccess(e);
                return true;
            }
        }
        return false;
    }

    /**
     * 将某个键所对应的值进行更新, 如果成功更新返回 {@code true}, 否则返回
     * {@code false}. 注意: <br>
     * 1. 这个方法会调用 {@link #afterNodeAccess(Node)} 钩子
     * 2. 不需要比较旧值是否相同, 这一点与 {@link #replace(Object, Object, Object)} 不同
     * 3. 返回值不再表示是否替换成功, 而是替换前的旧值
     *
     * @param key   键
     * @param value 新值
     * @return 替换前的旧值
     */
    @Override
    public V replace(K key, V value) {
        Node<K, V> e = getNode(hash(key), key);
        if (e != null) {
            // 注意与上面的方法区分
            // 这里没有比较旧值是否相同
            V oldValue = e.value;
            e.value = value;
            afterNodeAccess(e);
            return oldValue;
        }
        return null;
    }

    @Override
    public V computeIfAbsent(K key,
                             Function<? super K, ? extends V> mappingFunction) {
        if (mappingFunction == null)
            throw new NullPointerException();
        int hash = hash(key);
        Node<K, V>[] tab;
        Node<K, V> first;
        int n, i;
        int binCount = 0;
        TreeNode<K, V> t = null;
        Node<K, V> old = null;
        if (size > threshold || (tab = table) == null ||
                (n = tab.length) == 0)
            n = (tab = resize()).length;
        if ((first = tab[i = (n - 1) & hash]) != null) {
            if (first instanceof TreeNode)
                old = (t = (TreeNode<K, V>) first).getTreeNode(hash, key);
            else {
                Node<K, V> e = first;
                K k;
                do {
                    if (e.hash == hash &&
                            ((k = e.key) == key || (key != null && key.equals(k)))) {
                        old = e;
                        break;
                    }
                    ++binCount;
                } while ((e = e.next) != null);
            }
            V oldValue;
            if (old != null && (oldValue = old.value) != null) {
                afterNodeAccess(old);
                return oldValue;
            }
        }
        V v = mappingFunction.apply(key);
        if (v == null) {
            return null;
        } else if (old != null) {
            old.value = v;
            afterNodeAccess(old);
            return v;
        } else if (t != null)
            t.putTreeVal(this, tab, hash, key, v);
        else {
            tab[i] = newNode(hash, key, v, first);
            if (binCount >= TREEIFY_THRESHOLD - 1)
                treeifyBin(tab, hash);
        }
        ++modCount;
        ++size;
        afterNodeInsertion(true);
        return v;
    }

    public V computeIfPresent(K key,
                              BiFunction<? super K, ? super V, ? extends V> remappingFunction) {
        if (remappingFunction == null)
            throw new NullPointerException();
        Node<K, V> e;
        V oldValue;
        int hash = hash(key);
        if ((e = getNode(hash, key)) != null &&
                (oldValue = e.value) != null) {
            V v = remappingFunction.apply(key, oldValue);
            if (v != null) {
                e.value = v;
                afterNodeAccess(e);
                return v;
            } else
                removeNode(hash, key, null, false, true);
        }
        return null;
    }

    @Override
    public V compute(K key,
                     BiFunction<? super K, ? super V, ? extends V> remappingFunction) {
        if (remappingFunction == null)
            throw new NullPointerException();
        int hash = hash(key);
        Node<K, V>[] tab;
        Node<K, V> first;
        int n, i;
        int binCount = 0;
        TreeNode<K, V> t = null;
        Node<K, V> old = null;
        if (size > threshold || (tab = table) == null ||
                (n = tab.length) == 0)
            n = (tab = resize()).length;
        if ((first = tab[i = (n - 1) & hash]) != null) {
            if (first instanceof TreeNode)
                old = (t = (TreeNode<K, V>) first).getTreeNode(hash, key);
            else {
                Node<K, V> e = first;
                K k;
                do {
                    if (e.hash == hash &&
                            ((k = e.key) == key || (key != null && key.equals(k)))) {
                        old = e;
                        break;
                    }
                    ++binCount;
                } while ((e = e.next) != null);
            }
        }
        V oldValue = (old == null) ? null : old.value;
        V v = remappingFunction.apply(key, oldValue);
        if (old != null) {
            if (v != null) {
                old.value = v;
                afterNodeAccess(old);
            } else
                removeNode(hash, key, null, false, true);
        } else if (v != null) {
            if (t != null)
                t.putTreeVal(this, tab, hash, key, v);
            else {
                tab[i] = newNode(hash, key, v, first);
                if (binCount >= TREEIFY_THRESHOLD - 1)
                    treeifyBin(tab, hash);
            }
            ++modCount;
            ++size;
            afterNodeInsertion(true);
        }
        return v;
    }

    @Override
    public V merge(K key, V value,
                   BiFunction<? super V, ? super V, ? extends V> remappingFunction) {
        if (value == null)
            throw new NullPointerException();
        if (remappingFunction == null)
            throw new NullPointerException();
        int hash = hash(key);
        Node<K, V>[] tab;
        Node<K, V> first;
        int n, i;
        int binCount = 0;
        TreeNode<K, V> t = null;
        Node<K, V> old = null;
        if (size > threshold || (tab = table) == null ||
                (n = tab.length) == 0)
            n = (tab = resize()).length;
        if ((first = tab[i = (n - 1) & hash]) != null) {
            if (first instanceof TreeNode)
                old = (t = (TreeNode<K, V>) first).getTreeNode(hash, key);
            else {
                Node<K, V> e = first;
                K k;
                do {
                    if (e.hash == hash &&
                            ((k = e.key) == key || (key != null && key.equals(k)))) {
                        old = e;
                        break;
                    }
                    ++binCount;
                } while ((e = e.next) != null);
            }
        }
        if (old != null) {
            V v;
            if (old.value != null)
                v = remappingFunction.apply(old.value, value);
            else
                v = value;
            if (v != null) {
                old.value = v;
                afterNodeAccess(old);
            } else
                removeNode(hash, key, null, false, true);
            return v;
        }
        if (value != null) {
            if (t != null)
                t.putTreeVal(this, tab, hash, key, value);
            else {
                tab[i] = newNode(hash, key, value, first);
                if (binCount >= TREEIFY_THRESHOLD - 1)
                    treeifyBin(tab, hash);
            }
            ++modCount;
            ++size;
            afterNodeInsertion(true);
        }
        return value;
    }

    @Override
    public void forEach(BiConsumer<? super K, ? super V> action) {
        Node<K, V>[] tab;
        if (action == null)
            throw new NullPointerException();
        if (size > 0 && (tab = table) != null) {
            int mc = modCount;
            for (int i = 0; i < tab.length; ++i) {
                for (Node<K, V> e = tab[i]; e != null; e = e.next)
                    action.accept(e.key, e.value);
            }
            if (modCount != mc)
                throw new ConcurrentModificationException();
        }
    }

    @Override
    public void replaceAll(BiFunction<? super K, ? super V, ? extends V> function) {
        Node<K, V>[] tab;
        if (function == null)
            throw new NullPointerException();
        if (size > 0 && (tab = table) != null) {
            int mc = modCount;
            for (int i = 0; i < tab.length; ++i) {
                for (Node<K, V> e = tab[i]; e != null; e = e.next) {
                    e.value = function.apply(e.key, e.value);
                }
            }
            if (modCount != mc)
                throw new ConcurrentModificationException();
        }
    }

    /* ------------------------------------------------------------ */
    // Cloning and serialization

    /**
     * Returns a shallow copy of this <tt>HashMap</tt> instance: the keys and
     * values themselves are not cloned.
     *
     * @return a shallow copy of this map
     */
    @SuppressWarnings("unchecked")
    @Override
    public Object clone() {
        HashMap<K, V> result;
        try {
            result = (HashMap<K, V>) super.clone();
        } catch (CloneNotSupportedException e) {
            // this shouldn't happen, since we are Cloneable
            throw new InternalError(e);
        }
        result.reinitialize();
        result.putMapEntries(this, false);
        return result;
    }

    @Override
    public String getMapInfo(boolean outputEachNode) {
        StringBuffer stringBuffer = new StringBuffer();
        stringBuffer.append("---------- HashMap 信息输出开始 ----------\n");

        // 1. HashMap 当前的容量
        stringBuffer.append("1. 容量: ").append(capacity()).append("\n");
        // 2. HashMap 的加载因子大小
        stringBuffer.append("2. 加载因子: ").append(loadFactor()).append("\n");
        // 3. HashMap 总键值对的数量
        stringBuffer.append("3. 键值对数量: ").append(size()).append("\n");
        // 4. HashMap 阈值大小
        stringBuffer.append("4. 阈值大小: ").append(threshold).append("\n");
        // 5. HashMap 维护的 table 数组的长度
        stringBuffer.append("5. 数组长度: ").append(table == null ? 0 : table.length).append("\n");
        // 6. table 数组各个索引位置的头节点信息
        stringBuffer.append("6. 数组各个头节点的信息: ");
        if (table == null || table.length == 0) {
            stringBuffer.append("数组为空\n");
        } else {
            stringBuffer.append("[");
            for (int i = 0; i < table.length; i++) {
                Node<K, V> node = table[i];
                if (node == null) {
                    stringBuffer.append("null");
                } else if (node instanceof TreeNode) {
                    stringBuffer.append("红黑树");
                } else {
                    stringBuffer.append("链表");
                }
                if (i != table.length - 1) {
                    stringBuffer.append(", ");
                }
            }
            stringBuffer.append("]\n");
        }

        // 7. 展示指定索引区间的链表或红黑树的详细信息
        if (outputEachNode) {
            stringBuffer.append("7. 指定索引区间的链表或红黑树信息: \n");
            int i = 0;
            while (table != null && i < table.length) {
                stringBuffer.append("(").append(i + 1).append(") ");
                stringBuffer.append("索引 ").append(i).append(" 处为: ");
                Node<K, V> node = table[i];
                if (node == null) {
                    stringBuffer.append("null");
                } else if (node instanceof TreeNode) {
                    stringBuffer.append("红黑树");
                } else {
                    stringBuffer.append("链表, 此处的哈希值为 ")
                                .append(hash(node.key))
                                .append(" , 它的结构如下所示 ↓\n");
                    while (node != null) {
                        stringBuffer.append(node);
                        if (node.next != null) {
                            stringBuffer.append(" -> ");
                        }
                        node = node.next;
                    }
                }
                stringBuffer.append("\n");
                i++;
            }
        }

        stringBuffer.append("---------- HashMap 信息输出结束 ----------\n");

        return stringBuffer.toString();
    }

    /* ---------------- default 方法开始 -------------- */

    /**
     * {@link #get(Object)} 以及和它有关的方法会调用这个方法获得一个链表/树节点
     *
     * @param hash 键的哈希值
     * @param key  键
     * @return 节点(可能为 null)
     */
    final Node<K, V> getNode(int hash, Object key) {
        // 指向当前数组
        Node<K, V>[] tab = table;
        // 当前数组长度
        int n;

        // 通过 (n - 1) & hash 计算索引得到头节点
        Node<K, V> first;
        // 遍历过程中的当前节点
        Node<K, V> e;

        // 某个节点的键
        K k;

        // 如果数组不为空, 且头结点不为空
        if (tab != null && (n = tab.length) > 0 && (first = tab[(n - 1) & hash]) != null) {
            // 获取头结点的键
            k = first.key;
            // 如果哈希相同, 并且键也相同, 那么头节点就是要找的节点
            if (first.hash == hash && (k == key || (key != null && key.equals(k)))) {
                return first;
            }

            // 如果不是头节点那么就需要进行遍历
            e = first.next;
            if (e != null) {
                // 如果链表已经转换为了红黑树, 那么在红黑树中进行查找
                if (first instanceof TreeNode) {
                    return ((TreeNode<K, V>) first).getTreeNode(hash, key);
                }
                // 遍历链表
                do {
                    // 如果哈希相同, 并且键也相同, 那么这个节点就是要找的节点
                    k = e.key;
                    if (e.hash == hash && (k == key || (key != null && key.equals(k)))) {
                        return e;
                    }
                    e = e.next;
                } while (e != null);
            }
        }

        // 如果最后找不到, 返回 null
        return null;
    }

    /**
     * {@link #put(Object, Object)} 以及相关的操作和调用这个方法
     *
     * @param hash         键的哈希值
     * @param key          键
     * @param value        值
     * @param onlyIfAbsent 如果是 true, 那么不会改变已有的值
     * @param evict        如果是 false, 表示哈希表处于创建过程
     * @return 键所对应的旧值(可能为 null)
     */
    final V putVal(int hash, K key, V value, boolean onlyIfAbsent, boolean evict) {
        // 指向当前数组
        Node<K, V>[] tab = table;
        // 当前数组长度
        int n;

        // 如果当前数组未初始化, 那么首先需要进行扩容
        // 从这里也可以看到哈希表是延迟加载的
        if (tab == null || (n = tab.length) == 0) {
            tab = resize();
            n = tab.length;
        }

        // 索引
        int i = (n - 1) & hash;
        // 指向某个节点
        Node<K, V> p = tab[i];

        // 如果头节点为空, 那么需要新创建一个节点作为头节点
        // 此时这个头节点的下一节点为 null
        if (p == null) {
            tab[i] = newNode(hash, key, value, null);
        }
        // 如果头节点不为空, 证明此时存在链表或红黑树
        else {
            // 保存结果节点
            Node<K, V> e;
            // 保存某个键
            K k = p.key;

            // 如果头节点的哈希相等, 且键也相等, 那么头节点就是结果
            if (p.hash == hash && (k == key || (key != null && key.equals(k)))) {
                e = p;
            }
            // 如果头节点不满足要求, 并且链表已经转换为了红黑树
            // 那么在红黑树中查找所需节点
            else if (p instanceof TreeNode) {
                e = ((TreeNode<K, V>) p).putTreeVal(this, tab, hash, key, value);
            }
            // 在链表中查找节点
            else {
                // binCount 是在统计链表长度
                for (int binCount = 0; ; ++binCount) {
                    // 移动节点指针
                    e = p.next;
                    // 如果已经到了链表末尾
                    if (e == null) {
                        // 新建一个节点用于保存这个键值对
                        p.next = newNode(hash, key, value, null);
                        // 如果当前链表长度大于树化阈值, 那么需要尝试尝试将链表转为红黑树
                        // 因为 binCount 是从 0 开始的, 所以此处要减一
                        if (binCount >= TREEIFY_THRESHOLD - 1) {
                            treeifyBin(tab, hash);
                        }
                        break;
                    }
                    // 如果遍历过程中某个节点满足要求就推出循环
                    if (e.hash == hash && ((k = e.key) == key || (key != null && key.equals(k)))) {
                        break;
                    }
                    // 移动头指针
                    p = e;
                }
            }

            // 如果确实存在键所对应的节点
            if (e != null) {
                // 旧值
                V oldValue = e.value;
                // 如果允许改变旧值或者旧值本身就是 null, 更新旧值
                if (!onlyIfAbsent || oldValue == null) {
                    e.value = value;
                }
                // 给 LinkedHashMap 提供的钩子
                // 表示 put 操作修改了某节点的值
                afterNodeAccess(e);
                // 把旧值返回
                return oldValue;
            }
        }

        // 当修改原节点时的操作是直接返回, 所以如果能执行到这里,
        // 那么一定是新创建了节点, 即结构发生了变化
        ++modCount;

        // 如果插入以后大于阈值, 那么进行扩容
        if (++size > threshold) {
            resize();
        }

        // 给 LinkedHashMap 提供的钩子
        // 表示 put 操作是新创建了节点
        afterNodeInsertion(evict);

        // 由于是新创建的节点, 自然不存在旧值, 返回 null
        return null;
    }

    /**
     * 这个方法会被 {@link Map#putAll(Map)} 方法和构造器调用
     *
     * @param m     一个映射
     * @param evict false 表示初次创建, 其余情况为 true
     */
    final void putMapEntries(Map<? extends K, ? extends V> m, boolean evict) {
        // 映射的大小
        int s = m.size();
        // 如果映射不为空
        if (s > 0) {
            // 如果数组为空, 那么要进行初始化
            if (table == null) {
                // 初始容量 = (需要存储的元素个数 / 加载因子) + 1
                float ft = ((float) s / loadFactor) + 1.0F;
                // 如果计算出的容量大于最大容量, 则直接用最大容量
                int t = ft < (float) MAXIMUM_CAPACITY ? (int) ft : MAXIMUM_CAPACITY;
                // 当数组为空是, 阈值保存的的初始容量
                // 如果超出了, 那么就需要根据现有容量得到一个新的阈值
                if (t > threshold) {
                    threshold = tableSizeFor(t);
                }
            }
            // 如果数组不为空, 并且映射的大小大于阈值, 则需要扩容
            else if (s > threshold) {
                resize();
            }

            // 遍历 Map, 将键值对插入到哈希表中
            for (Map.Entry<? extends K, ? extends V> e : m.entrySet()) {
                K key = e.getKey();
                V value = e.getValue();
                putVal(hash(key), key, value, false, evict);
            }
        }
    }

    /**
     * {@link #remove(Object, Object)} 方法以及它相关的方法会调用这个方法
     *
     * @param hash       键的哈希值
     * @param key        键
     * @param value      值
     * @param matchValue 如果是 true, 则需要比较值是否相同
     * @param movable    如果是 false, 那么在删除是不会移动其他节点
     * @return 要删除的那个节点(可能是 null)
     */
    final Node<K, V> removeNode(int hash, Object key, Object value, boolean matchValue, boolean movable) {
        // 当前数组的引用
        Node<K, V>[] tab = table;
        // 数组长度
        int n;
        // 根据哈希计算索引
        int index;
        // 头节点
        Node<K, V> p;

        // 如果数组不为空, 并且存在头节点
        if (tab != null && (n = tab.length) > 0 && (p = tab[index = (n - 1) & hash]) != null) {
            // 保存要删除的那个节点
            Node<K, V> node = null;
            // 头节点的下一个节点
            Node<K, V> e = p.next;
            // 键
            K k = p.key;

            // 如果哈希值和键与参数相同, 那么这个节点就是要删除的节点
            if (p.hash == hash && (k == key || (key != null && key.equals(k)))) {
                node = p;
            }
            // 如果链表除了头节点还有其他节点
            else if (e != null) {
                // 如果已经是红黑树了
                // 那么从红黑树中查找节点
                if (p instanceof TreeNode) {
                    node = ((TreeNode<K, V>) p).getTreeNode(hash, key);
                }
                // 遍历链表查找要删除的那个节点
                else {
                    do {
                        k = e.key;
                        if (e.hash == hash && (k == key || (key != null && key.equals(k)))) {
                            node = e;
                            break;
                        }
                        p = e;
                        e = e.next;
                    } while (e != null);
                }
            }

            // 保存值
            V v;
            // 可以删除节点的情况有:
            // 1. 不需要比较值
            // 2. 需要比较值, 并且值相同
            if (node != null && (!matchValue || (v = node.value) == value || (value != null && value.equals(v)))) {
                // 如果是红黑树, 那么在红黑树中删除节点
                if (node instanceof TreeNode) {
                    ((TreeNode<K, V>) node).removeTreeNode(this, tab, movable);
                }
                // 删除头节点
                else if (node == p) {
                    tab[index] = node.next;
                }
                // 其他情况就直接修改 next
                else {
                    p.next = node.next;
                }

                // 结构发生变化, 所以 modCount 要加一
                ++modCount;
                // 键值对的数量减一
                --size;

                // 为 LinkedHashMap 提供的钩子
                afterNodeRemoval(node);

                // 返回节点
                return node;
            }
        }

        // 没有删除, 返回 null
        return null;
    }

    /**
     * 如果 {@link #table} 数组为 null, 那么会对数组进行初始化, 否则会将数组
     * 扩大为原来的两倍. 另外, 由于每次扩容都是翻倍, 所以原来的元素要么是继续待在
     * 原来的索引位置, 要么是偏移 2^n 个位置. {@link #resize()} 操作的流程图
     * 如下图所示.
     *
     * <img src="../../../../../../../resources/xyz/lixiangyu/source/jdk/java/util/hashmap/resize操作流程图.jpg">
     *
     * @return 初始化或扩容后的 {@link #table} 数组
     */
    final Node<K, V>[] resize() {
        // 指向当前数组, 表示扩容前的数组
        Node<K, V>[] oldTab = table;
        // 旧数组的长度
        int oldCap = (oldTab == null) ? 0 : oldTab.length;
        // 旧阈值
        int oldThr = threshold;

        // 新的容量
        int newCap = 0;
        // 新的阈值
        int newThr = 0;

        // 如果旧表不为空, 对它进行扩容
        if (oldCap > 0) {
            // 旧容量已经达到最大, 阈值直接取 Integer 的最大值并返回旧表
            if (oldCap >= MAXIMUM_CAPACITY) {
                threshold = Integer.MAX_VALUE;
                return oldTab;
            }
            // 将新容量和新阈值扩大为两倍
            else if ((newCap = oldCap << 1) < MAXIMUM_CAPACITY && oldCap >= DEFAULT_INITIAL_CAPACITY) {
                newThr = oldThr << 1; // double threshold
            }
        }
        // 如果旧表没有初始化, 但阈值不为 0, 那么将新容量设置为旧阈值
        // 这种情况出现在使用 HashMap(int, float) 构造器时
        else if (oldThr > 0) {
            newCap = oldThr;
        }
        // 如果容量和阈值都没有指定, 那么给它们设置默认值
        else {
            newCap = DEFAULT_INITIAL_CAPACITY;
            newThr = (int) (DEFAULT_LOAD_FACTOR * DEFAULT_INITIAL_CAPACITY);
        }

        // 如果阈值没有赋值, 对它进行赋值
        // 这部分的逻辑对应上面的 else if (oldThr > 0) 条件
        if (newThr == 0) {
            float ft = (float) newCap * loadFactor;
            if (newCap < MAXIMUM_CAPACITY && ft < (float) MAXIMUM_CAPACITY) {
                newThr = (int) ft;
            } else {
                newThr = Integer.MAX_VALUE;
            }
        }

        // 设置新的阈值
        threshold = newThr;

        // 根据新的容量创建一个的数组
        @SuppressWarnings({"rawtypes", "unchecked"})
        Node<K, V>[] newTab = (Node<K, V>[]) new Node[newCap];
        // 将 table 指向新创建的数组
        table = newTab;

        // 如果旧表中还有数据, 那么需要将数据做迁移
        if (oldTab != null) {
            // 遍历旧表
            for (int j = 0; j < oldCap; ++j) {
                // 获得头节点
                Node<K, V> e = oldTab[j];

                // 如果头节点不为空, 证明存在链表或者红黑树
                if (e != null) {
                    // 释放旧表中的头节点
                    oldTab[j] = null;

                    // 如果只有一个头节点
                    if (e.next == null) {
                        // 计算这个节点在新表中的索引位置
                        // 索引计算公式: (n - 1) & hash
                        int index = (newCap - 1) & e.hash;
                        newTab[index] = e;
                    }
                    // 如果是红黑树, 通过红黑树的 split 方法重新 rehash
                    else if (e instanceof TreeNode) {
                        ((TreeNode<K, V>) e).split(this, newTab, j, oldCap);
                    }
                    // 如果是链表
                    else {
                        // 通过下面节点变量保证节点之间顺序
                        // 存储与原索引位置相同的节点
                        Node<K, V> loHead = null;
                        Node<K, V> loTail = null;
                        // 存储与原索引位置差 oldCap 的节点
                        Node<K, V> hiHead = null;
                        Node<K, V> hiTail = null;
                        Node<K, V> next;

                        do {
                            // 使 next 指向当前节点的下一节点
                            next = e.next;

                            // 因为 oldCap 一定是 2^n, 假设是 16, 那么它的二进制表示是
                            // 0001 0000, e.hash 和它做与运算的结果为 0, 只有在
                            // e.hash = xxx0 xxxx 的情况下才会出现. 新数组的大小是 32,
                            // 二进制为 0010 0000, 索引计算是需要减 1, 即 0001 1111,
                            // 索引计算出的位置是一样的. 而这两个二进制数的区别仅在一个高位
                            // 的 1, 即 oldCap
                            int t = e.hash & oldCap;

                            // 如果索引位置相同, 就放在 lo 链表中, 否则放到 hi 链表中
                            if (t == 0) {
                                // 第一次时需要设置头节点, 其他情况就直接设置尾结点就可以了
                                if (loTail == null) {
                                    loHead = e;
                                } else {
                                    loTail.next = e;
                                }
                                loTail = e;
                            } else {
                                if (hiTail == null) {
                                    hiHead = e;
                                } else {
                                    hiTail.next = e;
                                }
                                hiTail = e;
                            }

                            // 移动指向当前节点的指针
                            e = next;
                        } while (e != null);

                        // 保存链表到数组中
                        if (loTail != null) {
                            loTail.next = null;
                            newTab[j] = loHead;
                        }

                        if (hiTail != null) {
                            hiTail.next = null;
                            newTab[j + oldCap] = hiHead;
                        }
                    }
                }
            }
        }

        // 初始化或扩容成功后返回新表
        return newTab;
    }

    /**
     * 根据指定的哈希计算出一个索引, 如果哈希表足够大, 那么就把链表转化为红黑树, 否则
     * 只是进行扩容
     *
     * @param tab  数组
     * @param hash 键的哈希值
     */
    final void treeifyBin(Node<K, V>[] tab, int hash) {
        // 数组的长度
        int n = tab.length;
        // 根据哈希值计算索引
        int index = (n - 1) & hash;
        // 获取对应索引位置的头节点
        Node<K, V> e = tab[index];

        // 如果数组为空或者数组长度小于64, 此时转换红黑树意义不大
        // 直接进行扩容
        if (tab == null || n < MIN_TREEIFY_CAPACITY) {
            resize();
        }
        // 在满足转换关系的情况下, 看一下头节点是否不为空
        else if (e != null) {
            // 红黑树的头尾节点
            TreeNode<K, V> hd = null;
            TreeNode<K, V> tl = null;

            // 转换为双向链表
            do {
                // 替换链表为树节点
                TreeNode<K, V> p = replacementTreeNode(e, null);

                // 红黑树的头节点
                if (tl == null) {
                    hd = p;
                }
                // 插入一个尾节点
                else {
                    p.prev = tl;
                    tl.next = p;
                }

                // 把当前节点作为尾节点
                tl = p;

                // 遍历链表
                e = e.next;
            } while (e != null);

            // 用双向链表替换原来的链表
            tab[index] = hd;
            // 将双向链表转换为红黑树
            if (tab[index] != null) {
                hd.treeify(tab);
            }
        }
    }

    /**
     * 返回加载因子的大小
     *
     * @return 加载因子的大小
     */
    final float loadFactor() {
        return loadFactor;
    }

    /**
     * 返回哈希表当前的容量
     *
     * @return
     */
    final int capacity() {
        int capacity = 0;
        if (table != null) {
            capacity = table.length;
        } else if (threshold > 0) {
            capacity = threshold;
        } else {
            capacity = DEFAULT_INITIAL_CAPACITY;
        }
        return capacity;
    }

    /* ------------------------------------------------------------ */
    // LinkedHashMap support


    /*
     * The following package-protected methods are designed to be
     * overridden by LinkedHashMap, but not by any other subclass.
     * Nearly all other internal methods are also package-protected
     * but are declared final, so can be used by LinkedHashMap, view
     * classes, and HashSet.
     */

    // Create a regular (non-tree) node
    Node<K, V> newNode(int hash, K key, V value, Node<K, V> next) {
        return new Node<>(hash, key, value, next);
    }

    // For conversion from TreeNodes to plain nodes
    Node<K, V> replacementNode(Node<K, V> p, Node<K, V> next) {
        return new Node<>(p.hash, p.key, p.value, next);
    }

    // Create a tree bin node
    TreeNode<K, V> newTreeNode(int hash, K key, V value, Node<K, V> next) {
        return new TreeNode<>(hash, key, value, next);
    }

    // For treeifyBin
    TreeNode<K, V> replacementTreeNode(Node<K, V> p, Node<K, V> next) {
        return new TreeNode<>(p.hash, p.key, p.value, next);
    }

    /**
     * Reset to initial default state.  Called by clone and readObject.
     */
    void reinitialize() {
        table = null;
        entrySet = null;
        keySet = null;
        values = null;
        modCount = 0;
        threshold = 0;
        size = 0;
    }

    // Callbacks to allow LinkedHashMap post-actions
    void afterNodeAccess(Node<K, V> p) {
    }

    void afterNodeInsertion(boolean evict) {
    }

    void afterNodeRemoval(Node<K, V> p) {
    }

    // Called only from writeObject, to ensure compatible ordering.
    void internalWriteEntries(java.io.ObjectOutputStream s) throws IOException {
        Node<K, V>[] tab;
        if (size > 0 && (tab = table) != null) {
            for (int i = 0; i < tab.length; ++i) {
                for (Node<K, V> e = tab[i]; e != null; e = e.next) {
                    s.writeObject(e.key);
                    s.writeObject(e.value);
                }
            }
        }
    }

    /* ---------------- default 方法结束 -------------- */

    /**
     * Save the state of the <tt>HashMap</tt> instance to a stream (i.e.,
     * serialize it).
     *
     * @serialData The <i>capacity</i> of the HashMap (the length of the
     * bucket array) is emitted (int), followed by the
     * <i>size</i> (an int, the number of key-value
     * mappings), followed by the key (Object) and value (Object)
     * for each key-value mapping.  The key-value mappings are
     * emitted in no particular order.
     */
    private void writeObject(java.io.ObjectOutputStream s)
            throws IOException {
        int buckets = capacity();
        // Write out the threshold, loadfactor, and any hidden stuff
        s.defaultWriteObject();
        s.writeInt(buckets);
        s.writeInt(size);
        internalWriteEntries(s);
    }

    /**
     * Reconstitutes this map from a stream (that is, deserializes it).
     *
     * @param s the stream
     * @throws ClassNotFoundException if the class of a serialized object
     *                                could not be found
     * @throws IOException            if an I/O error occurs
     */
    private void readObject(java.io.ObjectInputStream s)
            throws IOException, ClassNotFoundException {
        // Read in the threshold (ignored), loadfactor, and any hidden stuff
        s.defaultReadObject();
        reinitialize();
        if (loadFactor <= 0 || Float.isNaN(loadFactor))
            throw new InvalidObjectException("Illegal load factor: " +
                    loadFactor);
        s.readInt();                // Read and ignore number of buckets
        int mappings = s.readInt(); // Read number of mappings (size)
        if (mappings < 0)
            throw new InvalidObjectException("Illegal mappings count: " +
                    mappings);
        else if (mappings > 0) { // (if zero, use defaults)
            // Size the table using given load factor only if within
            // range of 0.25...4.0
            float lf = Math.min(Math.max(0.25f, loadFactor), 4.0f);
            float fc = (float) mappings / lf + 1.0f;
            int cap = ((fc < DEFAULT_INITIAL_CAPACITY) ?
                    DEFAULT_INITIAL_CAPACITY :
                    (fc >= MAXIMUM_CAPACITY) ?
                            MAXIMUM_CAPACITY :
                            tableSizeFor((int) fc));
            float ft = (float) cap * lf;
            threshold = ((cap < MAXIMUM_CAPACITY && ft < MAXIMUM_CAPACITY) ?
                    (int) ft : Integer.MAX_VALUE);

            // Check Map.Entry[].class since it's the nearest public type to
            // what we're actually creating.
            SharedSecrets.getJavaOISAccess().checkArray(s, Map.Entry[].class, cap);
            @SuppressWarnings({"rawtypes", "unchecked"})
            Node<K, V>[] tab = (Node<K, V>[]) new Node[cap];
            table = tab;

            // Read the keys and values, and put the mappings in the HashMap
            for (int i = 0; i < mappings; i++) {
                @SuppressWarnings("unchecked")
                K key = (K) s.readObject();
                @SuppressWarnings("unchecked")
                V value = (V) s.readObject();
                putVal(hash(key), key, value, false, false);
            }
        }
    }

    /* ------------------------------------------------------------ */
    // iterators

    abstract class HashIterator {
        Node<K, V> next;        // next entry to return
        Node<K, V> current;     // current entry
        int expectedModCount;  // for fast-fail
        int index;             // current slot

        HashIterator() {
            expectedModCount = modCount;
            Node<K, V>[] t = table;
            current = next = null;
            index = 0;
            if (t != null && size > 0) { // advance to first entry
                do {
                } while (index < t.length && (next = t[index++]) == null);
            }
        }

        public final boolean hasNext() {
            return next != null;
        }

        final Node<K, V> nextNode() {
            Node<K, V>[] t;
            Node<K, V> e = next;
            if (modCount != expectedModCount)
                throw new ConcurrentModificationException();
            if (e == null)
                throw new NoSuchElementException();
            if ((next = (current = e).next) == null && (t = table) != null) {
                do {
                } while (index < t.length && (next = t[index++]) == null);
            }
            return e;
        }

        public final void remove() {
            Node<K, V> p = current;
            if (p == null)
                throw new IllegalStateException();
            if (modCount != expectedModCount)
                throw new ConcurrentModificationException();
            current = null;
            K key = p.key;
            removeNode(hash(key), key, null, false, false);
            expectedModCount = modCount;
        }
    }

    final class KeyIterator extends HashIterator
            implements Iterator<K> {
        public final K next() {
            return nextNode().key;
        }
    }

    final class ValueIterator extends HashIterator
            implements Iterator<V> {
        public final V next() {
            return nextNode().value;
        }
    }

    final class EntryIterator extends HashIterator
            implements Iterator<Map.Entry<K, V>> {
        public final Map.Entry<K, V> next() {
            return nextNode();
        }
    }

    /* ------------------------------------------------------------ */
    // spliterators

    static class HashMapSpliterator<K, V> {
        final HashMap<K, V> map;
        Node<K, V> current;          // current node
        int index;                  // current index, modified on advance/split
        int fence;                  // one past last index
        int est;                    // size estimate
        int expectedModCount;       // for comodification checks

        HashMapSpliterator(HashMap<K, V> m, int origin,
                           int fence, int est,
                           int expectedModCount) {
            this.map = m;
            this.index = origin;
            this.fence = fence;
            this.est = est;
            this.expectedModCount = expectedModCount;
        }

        final int getFence() { // initialize fence and size on first use
            int hi;
            if ((hi = fence) < 0) {
                HashMap<K, V> m = map;
                est = m.size;
                expectedModCount = m.modCount;
                Node<K, V>[] tab = m.table;
                hi = fence = (tab == null) ? 0 : tab.length;
            }
            return hi;
        }

        public final long estimateSize() {
            getFence(); // force init
            return (long) est;
        }
    }

    static final class KeySpliterator<K, V>
            extends HashMapSpliterator<K, V>
            implements Spliterator<K> {
        KeySpliterator(HashMap<K, V> m, int origin, int fence, int est,
                       int expectedModCount) {
            super(m, origin, fence, est, expectedModCount);
        }

        public KeySpliterator<K, V> trySplit() {
            int hi = getFence(), lo = index, mid = (lo + hi) >>> 1;
            return (lo >= mid || current != null) ? null :
                    new KeySpliterator<>(map, lo, index = mid, est >>>= 1,
                            expectedModCount);
        }

        public void forEachRemaining(Consumer<? super K> action) {
            int i, hi, mc;
            if (action == null)
                throw new NullPointerException();
            HashMap<K, V> m = map;
            Node<K, V>[] tab = m.table;
            if ((hi = fence) < 0) {
                mc = expectedModCount = m.modCount;
                hi = fence = (tab == null) ? 0 : tab.length;
            } else
                mc = expectedModCount;
            if (tab != null && tab.length >= hi &&
                    (i = index) >= 0 && (i < (index = hi) || current != null)) {
                Node<K, V> p = current;
                current = null;
                do {
                    if (p == null)
                        p = tab[i++];
                    else {
                        action.accept(p.key);
                        p = p.next;
                    }
                } while (p != null || i < hi);
                if (m.modCount != mc)
                    throw new ConcurrentModificationException();
            }
        }

        public boolean tryAdvance(Consumer<? super K> action) {
            int hi;
            if (action == null)
                throw new NullPointerException();
            Node<K, V>[] tab = map.table;
            if (tab != null && tab.length >= (hi = getFence()) && index >= 0) {
                while (current != null || index < hi) {
                    if (current == null)
                        current = tab[index++];
                    else {
                        K k = current.key;
                        current = current.next;
                        action.accept(k);
                        if (map.modCount != expectedModCount)
                            throw new ConcurrentModificationException();
                        return true;
                    }
                }
            }
            return false;
        }

        public int characteristics() {
            return (fence < 0 || est == map.size ? Spliterator.SIZED : 0) |
                    Spliterator.DISTINCT;
        }
    }

    static final class ValueSpliterator<K, V>
            extends HashMapSpliterator<K, V>
            implements Spliterator<V> {
        ValueSpliterator(HashMap<K, V> m, int origin, int fence, int est,
                         int expectedModCount) {
            super(m, origin, fence, est, expectedModCount);
        }

        public ValueSpliterator<K, V> trySplit() {
            int hi = getFence(), lo = index, mid = (lo + hi) >>> 1;
            return (lo >= mid || current != null) ? null :
                    new ValueSpliterator<>(map, lo, index = mid, est >>>= 1,
                            expectedModCount);
        }

        public void forEachRemaining(Consumer<? super V> action) {
            int i, hi, mc;
            if (action == null)
                throw new NullPointerException();
            HashMap<K, V> m = map;
            Node<K, V>[] tab = m.table;
            if ((hi = fence) < 0) {
                mc = expectedModCount = m.modCount;
                hi = fence = (tab == null) ? 0 : tab.length;
            } else
                mc = expectedModCount;
            if (tab != null && tab.length >= hi &&
                    (i = index) >= 0 && (i < (index = hi) || current != null)) {
                Node<K, V> p = current;
                current = null;
                do {
                    if (p == null)
                        p = tab[i++];
                    else {
                        action.accept(p.value);
                        p = p.next;
                    }
                } while (p != null || i < hi);
                if (m.modCount != mc)
                    throw new ConcurrentModificationException();
            }
        }

        public boolean tryAdvance(Consumer<? super V> action) {
            int hi;
            if (action == null)
                throw new NullPointerException();
            Node<K, V>[] tab = map.table;
            if (tab != null && tab.length >= (hi = getFence()) && index >= 0) {
                while (current != null || index < hi) {
                    if (current == null)
                        current = tab[index++];
                    else {
                        V v = current.value;
                        current = current.next;
                        action.accept(v);
                        if (map.modCount != expectedModCount)
                            throw new ConcurrentModificationException();
                        return true;
                    }
                }
            }
            return false;
        }

        public int characteristics() {
            return (fence < 0 || est == map.size ? Spliterator.SIZED : 0);
        }
    }

    static final class EntrySpliterator<K, V>
            extends HashMapSpliterator<K, V>
            implements Spliterator<Map.Entry<K, V>> {
        EntrySpliterator(HashMap<K, V> m, int origin, int fence, int est,
                         int expectedModCount) {
            super(m, origin, fence, est, expectedModCount);
        }

        public EntrySpliterator<K, V> trySplit() {
            int hi = getFence(), lo = index, mid = (lo + hi) >>> 1;
            return (lo >= mid || current != null) ? null :
                    new EntrySpliterator<>(map, lo, index = mid, est >>>= 1,
                            expectedModCount);
        }

        public void forEachRemaining(Consumer<? super Map.Entry<K, V>> action) {
            int i, hi, mc;
            if (action == null)
                throw new NullPointerException();
            HashMap<K, V> m = map;
            Node<K, V>[] tab = m.table;
            if ((hi = fence) < 0) {
                mc = expectedModCount = m.modCount;
                hi = fence = (tab == null) ? 0 : tab.length;
            } else
                mc = expectedModCount;
            if (tab != null && tab.length >= hi &&
                    (i = index) >= 0 && (i < (index = hi) || current != null)) {
                Node<K, V> p = current;
                current = null;
                do {
                    if (p == null)
                        p = tab[i++];
                    else {
                        action.accept(p);
                        p = p.next;
                    }
                } while (p != null || i < hi);
                if (m.modCount != mc)
                    throw new ConcurrentModificationException();
            }
        }

        public boolean tryAdvance(Consumer<? super Map.Entry<K, V>> action) {
            int hi;
            if (action == null)
                throw new NullPointerException();
            Node<K, V>[] tab = map.table;
            if (tab != null && tab.length >= (hi = getFence()) && index >= 0) {
                while (current != null || index < hi) {
                    if (current == null)
                        current = tab[index++];
                    else {
                        Node<K, V> e = current;
                        current = current.next;
                        action.accept(e);
                        if (map.modCount != expectedModCount)
                            throw new ConcurrentModificationException();
                        return true;
                    }
                }
            }
            return false;
        }

        public int characteristics() {
            return (fence < 0 || est == map.size ? Spliterator.SIZED : 0) |
                    Spliterator.DISTINCT;
        }
    }



    /* ---------------- 内部类开始 -------------- */

    /**
     * 键集合类
     *
     * @see AbstractSet
     */
    final class KeySet extends AbstractSet<K> {
        /**
         * 返回集合大小, 集合的大小和哈希表的 size 是相同的
         *
         * @return
         */
        public final int size() {
            return size;
        }

        /**
         * 释放键, 调用 {@link HashMap#clear()}
         */
        public final void clear() {
            HashMap.this.clear();
        }

        /**
         * 返回迭代器
         *
         * @return 迭代器
         */
        public final Iterator<K> iterator() {
            return new KeyIterator();
        }

        /**
         * 判断集合中是否存在某个对象
         *
         * @param o 某个要判断的对象
         * @return 集合中是否存在这个对象
         */
        public final boolean contains(Object o) {
            return containsKey(o);
        }

        /**
         * 调用 {@link #removeNode(int, Object, Object, boolean, boolean)}
         * 方法删除节点
         *
         * @param key 键
         * @return 键所对应的旧值
         */
        public final boolean remove(Object key) {
            return removeNode(hash(key), key, null, false, true) != null;
        }

        /**
         * 可分割迭代器
         *
         * @return 一个可分割迭代器
         */
        public final Spliterator<K> spliterator() {
            return new KeySpliterator<>(HashMap.this, 0, -1, 0, 0);
        }

        /**
         * 针对每个元素所做的操作
         *
         * @param action 要执行的操作
         */
        public final void forEach(Consumer<? super K> action) {
            // 操作不能为空
            if (action == null) {
                throw new NullPointerException();
            }

            // 指向当前数组
            Node<K, V>[] tab = table;

            // 遍历数组
            if (size > 0 && tab != null) {
                // 保存 modCount 的值
                int mc = modCount;

                // 遍历链表
                for (int i = 0; i < tab.length; ++i) {
                    for (Node<K, V> e = tab[i]; e != null; e = e.next) {
                        // 传入键并执行相应操作
                        action.accept(e.key);
                    }
                }

                // 支持 fail-fast 机制
                if (modCount != mc) {
                    throw new ConcurrentModificationException();
                }
            }
        }
    }

    /**
     * 一个键组成的集合
     *
     * @see AbstractCollection
     */
    final class Values extends AbstractCollection<V> {
        /**
         * 这个集合的大小, 它的大小与当前哈希表的的 {@linkplain HashMap#size 键值对数量}
         * 是相同的
         *
         * @return 集合的大小
         */
        public final int size() {
            return size;
        }

        /**
         * 清空键值对
         */
        public final void clear() {
            HashMap.this.clear();
        }

        /**
         * 返回这个集合的迭代器
         *
         * @return 迭代器
         */
        public final Iterator<V> iterator() {
            return new ValueIterator();
        }

        /**
         * 判断指定的对象在集合中是否存在
         *
         * @param o 某个对象
         * @return 在集合中是否存在
         */
        public final boolean contains(Object o) {
            return containsValue(o);
        }

        /**
         * 可分割迭代器
         *
         * @return 一个可分割迭代器
         */
        public final Spliterator<V> spliterator() {
            return new ValueSpliterator<>(HashMap.this, 0, -1, 0, 0);
        }

        /**
         * 针对每个元素所做的操作
         *
         * @param action 要执行的操作
         */
        public final void forEach(Consumer<? super V> action) {
            // 要执行的动作不能为空
            if (action == null) {
                throw new NullPointerException();
            }

            // 指向当前数组
            Node<K, V>[] tab = table;
            // 如果数组不为空
            if (size > 0 && tab != null) {
                // 记录一下当前的 modCount
                int mc = modCount;

                // 遍历链表
                for (int i = 0; i < tab.length; ++i) {
                    for (Node<K, V> e = tab[i]; e != null; e = e.next)
                        action.accept(e.value);
                }

                // 支持 fail-fast 机制
                if (modCount != mc) {
                    throw new ConcurrentModificationException();
                }
            }
        }
    }

    /**
     * 一个由键值对组合组成的集合
     *
     * @see AbstractSet
     * @see Map.Entry
     */
    final class EntrySet extends AbstractSet<Map.Entry<K, V>> {
        /**
         * 当前集合的容量, 这个容量与 {@link HashMap#size} 应该是相等的
         *
         * @return
         */
        public final int size() {
            return size;
        }

        /**
         * 清空所有键值对
         */
        public final void clear() {
            HashMap.this.clear();
        }

        /**
         * 返回迭代器
         *
         * @return 一个迭代器
         */
        public final Iterator<Map.Entry<K, V>> iterator() {
            return new EntryIterator();
        }

        /**
         * 根据键判断某个键值对是否存在
         *
         * @param o 某个键值对
         * @return 这个键值对是否存在
         */
        public final boolean contains(Object o) {
            // 如果不是 Map.Entry 就直接返回 false
            if (!(o instanceof Map.Entry)) {
                return false;
            }
            // 强转类型
            Map.Entry<?, ?> e = (Map.Entry<?, ?>) o;
            // 通过键获取节点
            Object key = e.getKey();
            Node<K, V> candidate = getNode(hash(key), key);
            // 如果节点存在并且与参数传入的键值对相同, 则认为存在
            return candidate != null && candidate.equals(e);
        }

        /**
         * 移除某个键值对
         *
         * @param o 某个键值对
         * @return 是否移除了
         */
        public final boolean remove(Object o) {
            // 非 Map.Entry 对象不做处理
            if (o instanceof Map.Entry) {
                // 通过 HashMap.removeNode 方法移除节点
                Map.Entry<?, ?> e = (Map.Entry<?, ?>) o;
                Object key = e.getKey();
                Object value = e.getValue();
                return removeNode(hash(key), key, value, true, true) != null;
            }
            return false;
        }

        /**
         * 可分割迭代器
         *
         * @return 一个可分割迭代器
         */
        public final Spliterator<Map.Entry<K, V>> spliterator() {
            return new EntrySpliterator<>(HashMap.this, 0, -1, 0, 0);
        }

        /**
         * 对每个元素要执行的操作
         *
         * @param action 要执行的操作
         */
        public final void forEach(Consumer<? super Map.Entry<K, V>> action) {
            // 操作不能为空
            if (action == null) {
                throw new NullPointerException();
            }

            // 遍历数组
            Node<K, V>[] tab = table;
            if (size > 0 && tab != null) {
                // 保存当前的 modCount
                int mc = modCount;
                // 遍历链表
                for (int i = 0; i < tab.length; ++i) {
                    for (Node<K, V> e = tab[i]; e != null; e = e.next) {
                        action.accept(e);
                    }
                }
                // 支持 fail-fast 机制
                if (modCount != mc) {
                    throw new ConcurrentModificationException();
                }
            }
        }
    }

    /* ------------------------------------------------------------ */
    // Tree bins

    /**
     * Entry for Tree bins. Extends LinkedHashMap.Entry (which in turn
     * extends Node) so can be used as extension of either regular or
     * linked node.
     */
    static final class TreeNode<K, V> extends LinkedHashMap.Entry<K, V> {
        TreeNode<K, V> parent;  // red-black tree links
        TreeNode<K, V> left;
        TreeNode<K, V> right;
        TreeNode<K, V> prev;    // needed to unlink next upon deletion
        boolean red;

        TreeNode(int hash, K key, V val, Node<K, V> next) {
            super(hash, key, val, next);
        }

        /**
         * Returns root of tree containing this node.
         */
        final TreeNode<K, V> root() {
            for (TreeNode<K, V> r = this, p; ; ) {
                if ((p = r.parent) == null)
                    return r;
                r = p;
            }
        }

        /**
         * Ensures that the given root is the first node of its bin.
         */
        static <K, V> void moveRootToFront(Node<K, V>[] tab, TreeNode<K, V> root) {
            int n;
            if (root != null && tab != null && (n = tab.length) > 0) {
                int index = (n - 1) & root.hash;
                TreeNode<K, V> first = (TreeNode<K, V>) tab[index];
                if (root != first) {
                    Node<K, V> rn;
                    tab[index] = root;
                    TreeNode<K, V> rp = root.prev;
                    if ((rn = root.next) != null)
                        ((TreeNode<K, V>) rn).prev = rp;
                    if (rp != null)
                        rp.next = rn;
                    if (first != null)
                        first.prev = root;
                    root.next = first;
                    root.prev = null;
                }
                assert checkInvariants(root);
            }
        }

        /**
         * Finds the node starting at root p with the given hash and key.
         * The kc argument caches comparableClassFor(key) upon first use
         * comparing keys.
         */
        final TreeNode<K, V> find(int h, Object k, Class<?> kc) {
            TreeNode<K, V> p = this;
            do {
                int ph, dir;
                K pk;
                TreeNode<K, V> pl = p.left, pr = p.right, q;
                if ((ph = p.hash) > h)
                    p = pl;
                else if (ph < h)
                    p = pr;
                else if ((pk = p.key) == k || (k != null && k.equals(pk)))
                    return p;
                else if (pl == null)
                    p = pr;
                else if (pr == null)
                    p = pl;
                else if ((kc != null ||
                        (kc = comparableClassFor(k)) != null) &&
                        (dir = compareComparables(kc, k, pk)) != 0)
                    p = (dir < 0) ? pl : pr;
                else if ((q = pr.find(h, k, kc)) != null)
                    return q;
                else
                    p = pl;
            } while (p != null);
            return null;
        }

        /**
         * Calls find for root node.
         */
        final TreeNode<K, V> getTreeNode(int h, Object k) {
            return ((parent != null) ? root() : this).find(h, k, null);
        }

        /**
         * Tie-breaking utility for ordering insertions when equal
         * hashCodes and non-comparable. We don't require a total
         * order, just a consistent insertion rule to maintain
         * equivalence across rebalancings. Tie-breaking further than
         * necessary simplifies testing a bit.
         */
        static int tieBreakOrder(Object a, Object b) {
            int d;
            if (a == null || b == null ||
                    (d = a.getClass().getName().
                            compareTo(b.getClass().getName())) == 0)
                d = (System.identityHashCode(a) <= System.identityHashCode(b) ?
                        -1 : 1);
            return d;
        }

        /**
         * Forms tree of the nodes linked from this node.
         */
        final void treeify(Node<K, V>[] tab) {
            TreeNode<K, V> root = null;
            for (TreeNode<K, V> x = this, next; x != null; x = next) {
                next = (TreeNode<K, V>) x.next;
                x.left = x.right = null;
                if (root == null) {
                    x.parent = null;
                    x.red = false;
                    root = x;
                } else {
                    K k = x.key;
                    int h = x.hash;
                    Class<?> kc = null;
                    for (TreeNode<K, V> p = root; ; ) {
                        int dir, ph;
                        K pk = p.key;
                        if ((ph = p.hash) > h)
                            dir = -1;
                        else if (ph < h)
                            dir = 1;
                        else if ((kc == null &&
                                (kc = comparableClassFor(k)) == null) ||
                                (dir = compareComparables(kc, k, pk)) == 0)
                            dir = tieBreakOrder(k, pk);

                        TreeNode<K, V> xp = p;
                        if ((p = (dir <= 0) ? p.left : p.right) == null) {
                            x.parent = xp;
                            if (dir <= 0)
                                xp.left = x;
                            else
                                xp.right = x;
                            root = balanceInsertion(root, x);
                            break;
                        }
                    }
                }
            }
            moveRootToFront(tab, root);
        }

        /**
         * Returns a list of non-TreeNodes replacing those linked from
         * this node.
         */
        final Node<K, V> untreeify(HashMap<K, V> map) {
            Node<K, V> hd = null, tl = null;
            for (Node<K, V> q = this; q != null; q = q.next) {
                Node<K, V> p = map.replacementNode(q, null);
                if (tl == null)
                    hd = p;
                else
                    tl.next = p;
                tl = p;
            }
            return hd;
        }

        /**
         * Tree version of putVal.
         */
        final TreeNode<K, V> putTreeVal(HashMap<K, V> map, Node<K, V>[] tab,
                                        int h, K k, V v) {
            Class<?> kc = null;
            boolean searched = false;
            TreeNode<K, V> root = (parent != null) ? root() : this;
            for (TreeNode<K, V> p = root; ; ) {
                int dir, ph;
                K pk;
                if ((ph = p.hash) > h)
                    dir = -1;
                else if (ph < h)
                    dir = 1;
                else if ((pk = p.key) == k || (k != null && k.equals(pk)))
                    return p;
                else if ((kc == null &&
                        (kc = comparableClassFor(k)) == null) ||
                        (dir = compareComparables(kc, k, pk)) == 0) {
                    if (!searched) {
                        TreeNode<K, V> q, ch;
                        searched = true;
                        if (((ch = p.left) != null &&
                                (q = ch.find(h, k, kc)) != null) ||
                                ((ch = p.right) != null &&
                                        (q = ch.find(h, k, kc)) != null))
                            return q;
                    }
                    dir = tieBreakOrder(k, pk);
                }

                TreeNode<K, V> xp = p;
                if ((p = (dir <= 0) ? p.left : p.right) == null) {
                    Node<K, V> xpn = xp.next;
                    TreeNode<K, V> x = map.newTreeNode(h, k, v, xpn);
                    if (dir <= 0)
                        xp.left = x;
                    else
                        xp.right = x;
                    xp.next = x;
                    x.parent = x.prev = xp;
                    if (xpn != null)
                        ((TreeNode<K, V>) xpn).prev = x;
                    moveRootToFront(tab, balanceInsertion(root, x));
                    return null;
                }
            }
        }

        /**
         * Removes the given node, that must be present before this call.
         * This is messier than typical red-black deletion code because we
         * cannot swap the contents of an interior node with a leaf
         * successor that is pinned by "next" pointers that are accessible
         * independently during traversal. So instead we swap the tree
         * linkages. If the current tree appears to have too few nodes,
         * the bin is converted back to a plain bin. (The test triggers
         * somewhere between 2 and 6 nodes, depending on tree structure).
         */
        final void removeTreeNode(HashMap<K, V> map, Node<K, V>[] tab,
                                  boolean movable) {
            int n;
            if (tab == null || (n = tab.length) == 0)
                return;
            int index = (n - 1) & hash;
            TreeNode<K, V> first = (TreeNode<K, V>) tab[index], root = first, rl;
            TreeNode<K, V> succ = (TreeNode<K, V>) next, pred = prev;
            if (pred == null)
                tab[index] = first = succ;
            else
                pred.next = succ;
            if (succ != null)
                succ.prev = pred;
            if (first == null)
                return;
            if (root.parent != null)
                root = root.root();
            if (root == null
                    || (movable
                    && (root.right == null
                    || (rl = root.left) == null
                    || rl.left == null))) {
                tab[index] = first.untreeify(map);  // too small
                return;
            }
            TreeNode<K, V> p = this, pl = left, pr = right, replacement;
            if (pl != null && pr != null) {
                TreeNode<K, V> s = pr, sl;
                while ((sl = s.left) != null) // find successor
                    s = sl;
                boolean c = s.red;
                s.red = p.red;
                p.red = c; // swap colors
                TreeNode<K, V> sr = s.right;
                TreeNode<K, V> pp = p.parent;
                if (s == pr) { // p was s's direct parent
                    p.parent = s;
                    s.right = p;
                } else {
                    TreeNode<K, V> sp = s.parent;
                    if ((p.parent = sp) != null) {
                        if (s == sp.left)
                            sp.left = p;
                        else
                            sp.right = p;
                    }
                    if ((s.right = pr) != null)
                        pr.parent = s;
                }
                p.left = null;
                if ((p.right = sr) != null)
                    sr.parent = p;
                if ((s.left = pl) != null)
                    pl.parent = s;
                if ((s.parent = pp) == null)
                    root = s;
                else if (p == pp.left)
                    pp.left = s;
                else
                    pp.right = s;
                if (sr != null)
                    replacement = sr;
                else
                    replacement = p;
            } else if (pl != null)
                replacement = pl;
            else if (pr != null)
                replacement = pr;
            else
                replacement = p;
            if (replacement != p) {
                TreeNode<K, V> pp = replacement.parent = p.parent;
                if (pp == null)
                    root = replacement;
                else if (p == pp.left)
                    pp.left = replacement;
                else
                    pp.right = replacement;
                p.left = p.right = p.parent = null;
            }

            TreeNode<K, V> r = p.red ? root : balanceDeletion(root, replacement);

            if (replacement == p) {  // detach
                TreeNode<K, V> pp = p.parent;
                p.parent = null;
                if (pp != null) {
                    if (p == pp.left)
                        pp.left = null;
                    else if (p == pp.right)
                        pp.right = null;
                }
            }
            if (movable)
                moveRootToFront(tab, r);
        }

        /**
         * Splits nodes in a tree bin into lower and upper tree bins,
         * or untreeifies if now too small. Called only from resize;
         * see above discussion about split bits and indices.
         *
         * @param map   the map
         * @param tab   the table for recording bin heads
         * @param index the index of the table being split
         * @param bit   the bit of hash to split on
         */
        final void split(HashMap<K, V> map, Node<K, V>[] tab, int index, int bit) {
            TreeNode<K, V> b = this;
            // Relink into lo and hi lists, preserving order
            TreeNode<K, V> loHead = null, loTail = null;
            TreeNode<K, V> hiHead = null, hiTail = null;
            int lc = 0, hc = 0;
            for (TreeNode<K, V> e = b, next; e != null; e = next) {
                next = (TreeNode<K, V>) e.next;
                e.next = null;
                if ((e.hash & bit) == 0) {
                    if ((e.prev = loTail) == null)
                        loHead = e;
                    else
                        loTail.next = e;
                    loTail = e;
                    ++lc;
                } else {
                    if ((e.prev = hiTail) == null)
                        hiHead = e;
                    else
                        hiTail.next = e;
                    hiTail = e;
                    ++hc;
                }
            }

            if (loHead != null) {
                if (lc <= UNTREEIFY_THRESHOLD)
                    tab[index] = loHead.untreeify(map);
                else {
                    tab[index] = loHead;
                    if (hiHead != null) // (else is already treeified)
                        loHead.treeify(tab);
                }
            }
            if (hiHead != null) {
                if (hc <= UNTREEIFY_THRESHOLD)
                    tab[index + bit] = hiHead.untreeify(map);
                else {
                    tab[index + bit] = hiHead;
                    if (loHead != null)
                        hiHead.treeify(tab);
                }
            }
        }

        /* ------------------------------------------------------------ */
        // Red-black tree methods, all adapted from CLR

        static <K, V> TreeNode<K, V> rotateLeft(TreeNode<K, V> root,
                                                TreeNode<K, V> p) {
            TreeNode<K, V> r, pp, rl;
            if (p != null && (r = p.right) != null) {
                if ((rl = p.right = r.left) != null)
                    rl.parent = p;
                if ((pp = r.parent = p.parent) == null)
                    (root = r).red = false;
                else if (pp.left == p)
                    pp.left = r;
                else
                    pp.right = r;
                r.left = p;
                p.parent = r;
            }
            return root;
        }

        static <K, V> TreeNode<K, V> rotateRight(TreeNode<K, V> root,
                                                 TreeNode<K, V> p) {
            TreeNode<K, V> l, pp, lr;
            if (p != null && (l = p.left) != null) {
                if ((lr = p.left = l.right) != null)
                    lr.parent = p;
                if ((pp = l.parent = p.parent) == null)
                    (root = l).red = false;
                else if (pp.right == p)
                    pp.right = l;
                else
                    pp.left = l;
                l.right = p;
                p.parent = l;
            }
            return root;
        }

        static <K, V> TreeNode<K, V> balanceInsertion(TreeNode<K, V> root,
                                                      TreeNode<K, V> x) {
            x.red = true;
            for (TreeNode<K, V> xp, xpp, xppl, xppr; ; ) {
                if ((xp = x.parent) == null) {
                    x.red = false;
                    return x;
                } else if (!xp.red || (xpp = xp.parent) == null)
                    return root;
                if (xp == (xppl = xpp.left)) {
                    if ((xppr = xpp.right) != null && xppr.red) {
                        xppr.red = false;
                        xp.red = false;
                        xpp.red = true;
                        x = xpp;
                    } else {
                        if (x == xp.right) {
                            root = rotateLeft(root, x = xp);
                            xpp = (xp = x.parent) == null ? null : xp.parent;
                        }
                        if (xp != null) {
                            xp.red = false;
                            if (xpp != null) {
                                xpp.red = true;
                                root = rotateRight(root, xpp);
                            }
                        }
                    }
                } else {
                    if (xppl != null && xppl.red) {
                        xppl.red = false;
                        xp.red = false;
                        xpp.red = true;
                        x = xpp;
                    } else {
                        if (x == xp.left) {
                            root = rotateRight(root, x = xp);
                            xpp = (xp = x.parent) == null ? null : xp.parent;
                        }
                        if (xp != null) {
                            xp.red = false;
                            if (xpp != null) {
                                xpp.red = true;
                                root = rotateLeft(root, xpp);
                            }
                        }
                    }
                }
            }
        }

        static <K, V> TreeNode<K, V> balanceDeletion(TreeNode<K, V> root,
                                                     TreeNode<K, V> x) {
            for (TreeNode<K, V> xp, xpl, xpr; ; ) {
                if (x == null || x == root)
                    return root;
                else if ((xp = x.parent) == null) {
                    x.red = false;
                    return x;
                } else if (x.red) {
                    x.red = false;
                    return root;
                } else if ((xpl = xp.left) == x) {
                    if ((xpr = xp.right) != null && xpr.red) {
                        xpr.red = false;
                        xp.red = true;
                        root = rotateLeft(root, xp);
                        xpr = (xp = x.parent) == null ? null : xp.right;
                    }
                    if (xpr == null)
                        x = xp;
                    else {
                        TreeNode<K, V> sl = xpr.left, sr = xpr.right;
                        if ((sr == null || !sr.red) &&
                                (sl == null || !sl.red)) {
                            xpr.red = true;
                            x = xp;
                        } else {
                            if (sr == null || !sr.red) {
                                if (sl != null)
                                    sl.red = false;
                                xpr.red = true;
                                root = rotateRight(root, xpr);
                                xpr = (xp = x.parent) == null ?
                                        null : xp.right;
                            }
                            if (xpr != null) {
                                xpr.red = (xp == null) ? false : xp.red;
                                if ((sr = xpr.right) != null)
                                    sr.red = false;
                            }
                            if (xp != null) {
                                xp.red = false;
                                root = rotateLeft(root, xp);
                            }
                            x = root;
                        }
                    }
                } else { // symmetric
                    if (xpl != null && xpl.red) {
                        xpl.red = false;
                        xp.red = true;
                        root = rotateRight(root, xp);
                        xpl = (xp = x.parent) == null ? null : xp.left;
                    }
                    if (xpl == null)
                        x = xp;
                    else {
                        TreeNode<K, V> sl = xpl.left, sr = xpl.right;
                        if ((sl == null || !sl.red) &&
                                (sr == null || !sr.red)) {
                            xpl.red = true;
                            x = xp;
                        } else {
                            if (sl == null || !sl.red) {
                                if (sr != null)
                                    sr.red = false;
                                xpl.red = true;
                                root = rotateLeft(root, xpl);
                                xpl = (xp = x.parent) == null ?
                                        null : xp.left;
                            }
                            if (xpl != null) {
                                xpl.red = (xp == null) ? false : xp.red;
                                if ((sl = xpl.left) != null)
                                    sl.red = false;
                            }
                            if (xp != null) {
                                xp.red = false;
                                root = rotateRight(root, xp);
                            }
                            x = root;
                        }
                    }
                }
            }
        }

        /**
         * Recursive invariant check
         */
        static <K, V> boolean checkInvariants(TreeNode<K, V> t) {
            TreeNode<K, V> tp = t.parent, tl = t.left, tr = t.right,
                    tb = t.prev, tn = (TreeNode<K, V>) t.next;
            if (tb != null && tb.next != t)
                return false;
            if (tn != null && tn.prev != t)
                return false;
            if (tp != null && t != tp.left && t != tp.right)
                return false;
            if (tl != null && (tl.parent != t || tl.hash > t.hash))
                return false;
            if (tr != null && (tr.parent != t || tr.hash < t.hash))
                return false;
            if (t.red && tl != null && tl.red && tr != null && tr.red)
                return false;
            if (tl != null && !checkInvariants(tl))
                return false;
            if (tr != null && !checkInvariants(tr))
                return false;
            return true;
        }
    }

}
