package cn.enaium.ml4g.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Enaium
 */
class GameUtilTest {
    @Test
    public void getLibraries() {
        GameUtil.getLibraries("1.16.5").forEach(System.out::println);
    }

    @Test
    public void getNativesTest() {
        GameUtil.getNatives("1.16.5").forEach(System.out::println);
    }
}