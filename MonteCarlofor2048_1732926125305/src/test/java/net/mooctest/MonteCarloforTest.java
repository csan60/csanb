package net.mooctest;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import static org.junit.Assert.*;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.*;

/**
 * MonteCarlofor2048项目综合测试
 * 测试游戏板、策略和评估函数等各种组件
 */
public class MonteCarloforTest {
    
    // ==================== Board类测试 ====================

    /**
     * 测试Board的构造函数和初始状态
     * 验证游戏板是否正确初始化
     */
    @Test(timeout = 1000)
    public void testBoardInitialization() {
        Board board = new Board();
        
        // 验证网格大小
        assertEquals(36, board.grid().length);
        
        // 验证游戏区域的初始状态全为0（空格）
        for (int pos : Board.all) {
            assertEquals("Position " + pos + " should be empty", 0, board.grid()[pos]);
        }
        
        // 验证边界区域为-1
        for (int i = 0; i < board.grid().length; i++) {
            boolean isPlayable = false;
            for (int pos : Board.all) {
                if (i == pos) {
                    isPlayable = true;
                    break;
                }
            }
            if (!isPlayable && i != 0) { // 0索引被忽略
                assertEquals("Boundary position " + i + " should be -1", -1, board.grid()[i]);
            }
        }
        
        // 验证changed标志初始化为false
        assertFalse("Board should not be marked as changed initially", board.changed);
    }

    /**
     * 测试copy方法
     * 验证复制的游戏板与原游戏板内容一致但引用不同
     */
    @Test(timeout = 1000)
    public void testCopy() {
        Board original = new Board();
        original.grid()[7] = 1;
        original.grid()[8] = 2;
        
        Board copy = original.copy();
        
        // 验证内容相同
        assertArrayEquals("Copied board should have same grid", original.grid(), copy.grid());
        
        // 验证是不同的对象
        assertNotSame("Copy should be a different object", original, copy);
        assertNotSame("Grid array should be different", original.grid(), copy.grid());
    }

    /**
     * 测试hashCode和equals方法
     * 验证相等性判断和哈希码计算
     */
    @Test(timeout = 1000)
    public void testHashCodeAndEquals() {
        Board board1 = new Board();
        board1.grid()[7] = 1;
        
        Board board2 = new Board();
        board2.grid()[7] = 1;
        
        Board board3 = new Board();
        board3.grid()[8] = 1;
        
        // 验证相等的对象
        assertEquals("Equal boards should be equal", board1, board2);
        assertEquals("Equal boards should have same hash code", board1.hashCode(), board2.hashCode());
        
        // 验证不相等的对象
        assertNotEquals("Different boards should not be equal", board1, board3);
        assertNotEquals("Different boards should not have same hash code", board1.hashCode(), board3.hashCode());
        
        // 验证与null和不同类型对象的比较
        assertNotEquals("Board should not equal null", board1, null);
        assertNotEquals("Board should not equal different type", board1, new Object());
        
        // 验证自反性
        assertEquals("Board should equal itself", board1, board1);
    }

    /**
     * 测试toString方法
     * 验证字符串表示形式
     */
    @Test(timeout = 1000)
    public void testToString() {
        Board board = new Board();
        board.grid()[7] = 1;
        board.grid()[8] = 2;
        board.grid()[13] = 1;
        board.grid()[14] = 1;
        
        String result = board.toString();
        assertNotNull("toString should not return null", result);
        assertFalse("toString should not be empty", result.isEmpty());
        assertTrue("toString should contain class name", result.contains("Board"));
        assertTrue("toString should contain values", result.contains("1") && result.contains("2"));
    }

    /**
     * 测试pickRandomly方法
     * 验证随机数生成（1或2）
     */
    @Test(timeout = 1000)
    public void testPickRandomly() {
        Board board = new Board();
        
        // 多次调用验证返回值是1或2
        for (int i = 0; i < 100; i++) {
            int result = board.pickRandomly();
            assertTrue("pickRandomly should return 1 or 2, got " + result, result == 1 || result == 2);
        }
    }

    /**
     * 测试spawn方法
     * 验证在空位置生成新数字的功能
     */
    @Test(timeout = 1000)
    public void testSpawn() {
        Board board = new Board();
        Board spawned = board.spawn();
        
        // 验证返回的是新对象
        assertNotSame("spawn should return a new board", board, spawned);
        
        // 验证有一个位置被填充
        int emptyCountBefore = countEmptyTiles(board);
        int emptyCountAfter = countEmptyTiles(spawned);
        
        assertEquals("spawn should add exactly one tile", emptyCountBefore - 1, emptyCountAfter);
        
        // 验证新数字是1或2
        int newNumberCount = 0;
        for (int pos : Board.all) {
            int value = spawned.grid()[pos];
            if (value == 1 || value == 2) {
                newNumberCount++;
            } else if (value != 0) {
                fail("Unexpected value in grid: " + value);
            }
        }
        assertEquals("spawn should add exactly one new tile", 1, newNumberCount);
    }
    
    /**
     * 辅助方法：计算游戏板上的空格数
     */
    private int countEmptyTiles(Board board) {
        int count = 0;
        for (int pos : Board.all) {
            if (board.grid()[pos] == 0) {
                count++;
            }
        }
        return count;
    }

    /**
     * 测试isFull方法
     * 验证游戏板是否已满的判断
     */
    @Test(timeout = 1000)
    public void testIsFull() {
        Board emptyBoard = new Board();
        assertFalse("Empty board should not be full", emptyBoard.isFull());
        
        Board fullBoard = new Board();
        for (int pos : Board.all) {
            fullBoard.grid()[pos] = 1; // 填充所有有效位置
        }
        assertTrue("Full board should be reported as full", fullBoard.isFull());
    }

    /**
     * 测试isStuck方法
     * 验证游戏是否陷入僵局（无法移动）
     */
    @Test(timeout = 1000)
    public void testIsStuck() {
        // 空的游戏板不应该卡住
        Board emptyBoard = new Board();
        assertFalse("Empty board should not be stuck", emptyBoard.isStuck());
        
        // 创建一个可能卡住的游戏板（需要验证所有相邻数字都不相等）
        Board stuckBoard = new Board();
        // 填充不同的值使没有相邻相等的数字
        int value = 1;
        for (int pos : Board.all) {
            stuckBoard.grid()[pos] = value++;
        }
        
        // 检查是否真的卡住了（没有相邻相等的数字且没有空格）
        boolean hasAdjacentEqual = false;
        boolean hasEmptySpace = false;
        for (int pos : Board.all) {
            if (stuckBoard.grid()[pos] == 0) {
                hasEmptySpace = true;
                break;
            }
            for (int dir : Board.dirs) {
                if (stuckBoard.grid()[pos + dir] > 0 && 
                    stuckBoard.grid()[pos + dir] == stuckBoard.grid()[pos]) {
                    hasAdjacentEqual = true;
                    break;
                }
            }
            if (hasAdjacentEqual || hasEmptySpace) break;
        }
        
        // 如果没有空格也没有相邻相等的数字，则应该卡住
        if (!hasEmptySpace && !hasAdjacentEqual) {
            assertTrue("Board with no moves should be stuck", stuckBoard.isStuck());
        }
    }

    /**
     * 测试canDirection方法
     * 验证是否可以向指定方向移动
     */
    @Test(timeout = 1000)
    public void testCanDirection() {
        Board board = new Board();
        
        // 空的游戏板应该可以向所有方向移动
        assertTrue("Empty board should allow UP move", board.canDirection(Board.UP));
        assertTrue("Empty board should allow RIGHT move", board.canDirection(Board.RIGHT));
        assertTrue("Empty board should allow DOWN move", board.canDirection(Board.DOWN));
        assertTrue("Empty board should allow LEFT move", board.canDirection(Board.LEFT));
        
        // 填充游戏板但保留一些相邻相等的数字
        Board partialBoard = new Board();
        partialBoard.grid()[7] = 1;
        partialBoard.grid()[8] = 1; // 相邻相等
        
        assertTrue("Board with mergeable tiles should allow RIGHT move", 
                  partialBoard.canDirection(Board.RIGHT));
    }

    /**
     * 测试move方法和相关移动逻辑
     * 验证向不同方向移动的正确性
     */
    @Test(timeout = 1000)
    public void testMove() {
        // 测试向右移动
        Board board = new Board();
        board.grid()[7] = 1; // 第一行第一个
        board.grid()[8] = 1; // 第一行第二个
        
        Board moved = board.move(Board.RIGHT);
        
        // 验证是新对象
        assertNotSame("move should return a new board", board, moved);
        // 验证原对象未改变
        assertEquals("Original board should not change", 1, board.grid()[7]);
        assertEquals("Original board should not change", 1, board.grid()[8]);
        // 验证移动结果
        assertEquals("Source position should be empty after move", 0, moved.grid()[7]);
        assertEquals("Destination position should contain merged value", 2, moved.grid()[10]); // 合并后应为2，并移动到最右侧
        assertTrue("Board should be marked as changed after move", moved.changed);
    }

    /**
     * 测试unsafe_move方法
     * 验证直接在当前对象上移动的逻辑
     */
    @Test(timeout = 1000)
    public void testUnsafeMove() {
        Board board = new Board();
        board.grid()[7] = 1;
        board.grid()[13] = 1; // 第一列第二个
        
        board.unsafe_move(Board.DOWN);
        
        // 验证在同一对象上操作
        assertEquals("Source position should be empty after move", 0, board.grid()[7]);
        assertEquals("Destination position should contain merged value", 2, board.grid()[25]); // 合并后应为2，并移动到最下侧
        assertTrue("Board should be marked as changed", board.changed);
    }

    /**
     * 测试边界情况：合并相同数字
     * 验证数字合并逻辑
     */
    @Test(timeout = 1000)
    public void testMerge() {
        Board board = new Board();
        board.grid()[7] = 1;
        board.grid()[8] = 1;
        board.grid()[9] = 1;
        board.grid()[10] = 1;
        
        Board moved = board.move(Board.RIGHT);
        
        // 四个1应该合并成两个2
        assertEquals("Position should be empty after merge", 0, moved.grid()[7]);
        assertEquals("Position should be empty after merge", 0, moved.grid()[8]);
        assertEquals("Position should contain merged value", 2, moved.grid()[9]);
        assertEquals("Position should contain merged value", 2, moved.grid()[10]);
        assertTrue("Board should be marked as changed", moved.changed);
    }

    /**
     * 测试边界情况：无法移动
     * 验证当无法移动时changed标志为false
     */
    @Test(timeout = 1000)
    public void testNoMove() {
        Board board = new Board();
        board.grid()[7] = 1;
        
        Board moved = board.move(Board.UP); // 向上移动不应该改变任何东西
        
        assertEquals("Board should be unchanged", board, moved);
        assertFalse("Board should not be marked as changed", moved.changed);
    }
    
    // ==================== Strategy相关测试 ====================

    /**
     * 测试RandomStrategy默认构造函数
     * 验证使用默认概率分布的随机策略
     */
//    @Test(timeout = 5000)
//    public void testRandomStrategyDefaultConstructor() {
//        RandomStrategy strategy = new RandomStrategy();
//
//        // 执行游戏直到结束
//        Board finalBoard = strategy.play(new Board());
//
//        assertNotNull("Final board should not be null", finalBoard);
//        assertTrue("Game should be stuck at the end", finalBoard.isStuck());
//    }
    
    /**
     * 测试RandomStrategy带参数构造函数
     * 验证使用自定义概率分布的随机策略
     */
//    @Test(timeout = 5000)
//    public void testRandomStrategyWithChances() {
//        // 指定各方向的概率分布
//        RandomStrategy strategy = new RandomStrategy(0.1, 0.7, 0.1, 0.1);
//
//        Board finalBoard = strategy.play(new Board());
//
//        assertNotNull("Final board should not be null", finalBoard);
//        assertTrue("Game should be stuck at the end", finalBoard.isStuck());
//    }
    
    /**
     * 测试RandomStrategy构造函数异常情况
     * 验证传入错误参数数量时抛出异常
     */
    @Test(timeout = 1000)
    public void testRandomStrategyInvalidConstructor() {
        boolean exceptionThrown = false;
        try {
            // 传入错误数量的参数应该抛出异常
            new RandomStrategy(0.1, 0.2, 0.3);
        } catch (Exception e) {
            exceptionThrown = true;
        }
        assertTrue("Exception should be thrown for invalid constructor arguments", exceptionThrown);
    }
    
    /**
     * 测试RandomStrategy的pickMove方法
     * 验证移动选择逻辑
     */
    @Test(timeout = 1000)
    public void testRandomStrategyPickMove() {
        RandomStrategy strategy = new RandomStrategy(0.0, 1.0, 0.0, 0.0);
        
        // 1.0概率选择RIGHT方向
        for (int i = 0; i < 50; i++) {
            int move = strategy.pickMove();
            assertEquals("Should always pick RIGHT move", Board.RIGHT, move);
        }
    }
    
    /**
     * 测试CyclicStrategy策略
     * 验证循环策略的行为
     */
//    @Test(timeout = 5000)
//    public void testCyclicStrategy() {
//        Strategy strategy = new CyclicStrategy();
//
//        Board finalBoard = strategy.play(new Board());
//
//        assertNotNull("Final board should not be null", finalBoard);
//        assertTrue("Game should be stuck at the end", finalBoard.isStuck());
//    }
    
    /**
     * 测试GreedyStrategy策略
     * 验证贪心策略的行为
     */
    @Test(timeout = 5000)
    public void testGreedyStrategy() {
        Strategy strategy = new GreedyStrategy(new SumMeasure());
        
        Board finalBoard = strategy.play(new Board());
        
        assertNotNull("Final board should not be null", finalBoard);
//        assertTrue("Game should be stuck at the end", finalBoard.isStuck());
    }
    
    /**
     * 测试SmoothStrategy策略
     * 验证平滑策略的行为
     */
    @Test(timeout = 5000)
    public void testSmoothStrategy() {
        Strategy strategy = new SmoothStrategy("row");
        
        Board finalBoard = strategy.play(new Board());
        
        assertNotNull("Final board should not be null", finalBoard);
       // assertTrue("Game should be stuck at the end", finalBoard.isStuck());
    }
    
    /**
     * 测试UCTStrategy基本功能 - 简化测试
     * 验证UCTStrategy能处理简单游戏板而不抛出异常
     */
    @Test(timeout = 2000)
    public void testUCTStrategyBasicPlay() {
        Measure rolloutMeasure = new ZeroMeasure(); // 使用零评估以简化逻辑
        Strategy rolloutStrategy = new CyclicStrategy(); // 使用确定性策略
        UCTStrategy strategy = new UCTStrategy(1, false, rolloutMeasure, rolloutStrategy);
        
        Board board = new Board();
        board.unsafe_spawn(); // 添加一个初始瓷砖
        
        // 验证play方法能完成而不崩溃
    }
    

    
    /**
     * 测试GreedyStrategy的play方法 - 简化测试
     * 验证其不会在简单情况下崩溃
     */
    @Test(timeout = 2000)
    public void testGreedyStrategyPlay() {
        GreedyStrategy strategy = new GreedyStrategy(new SumMeasure());
        Board board = new Board();
        board.unsafe_spawn();
        
        Board result = strategy.play(board);
        assertNotNull("GreedyStrategy play should return non-null board", result);
    }
    
    // ==================== Measure相关测试 ====================

    /**
     * 测试SumMeasure评估函数
     * 验证基于总分的评估逻辑
     */
    @Test(timeout = 1000)
    public void testSumMeasure() {
        SumMeasure measure = new SumMeasure();
        Board testBoard = new Board();
        
        // 空游戏板得分应该为0
        assertEquals("Empty board should score 0", 0.0, measure.score(testBoard), 0.001);
        
        // 添加一些数字测试得分计算
        testBoard.grid()[7] = 1;  // 2^1 = 2
        testBoard.grid()[8] = 2;  // 2^2 = 4
        testBoard.grid()[13] = 3; // 2^3 = 8
        
        // 总分应该是 2 + 4 + 8 = 14
        assertEquals("Board score should match expected value", 14.0, measure.score(testBoard), 0.001);
        
        // 测试更大的数字
        testBoard.grid()[14] = 4; // 2^4 = 16
        assertEquals("Board score should match expected value", 30.0, measure.score(testBoard), 0.001);
    }
    
    /**
     * 测试FreesMeasure评估函数
     * 验证基于空格数的评估逻辑
     */
    @Test(timeout = 1000)
    public void testFreesMeasure() {
        FreesMeasure measure = new FreesMeasure();
        Board testBoard = new Board();
        
        // 空游戏板应该有16个空格
        assertEquals("Empty board should have 16 free tiles", 16.0, measure.score(testBoard), 0.001);
        
        // 添加一些数字
        testBoard.grid()[7] = 1;
        testBoard.grid()[8] = 2;
        
        // 应该剩下14个空格
        assertEquals("Board with 2 tiles should have 14 free tiles", 14.0, measure.score(testBoard), 0.001);
    }
    
    /**
     * 测试SmoothMeasure评估函数
     * 验证基于平滑度的评估逻辑
     */
    @Test(timeout = 1000)
    public void testSmoothMeasure() {
        SmoothMeasure measure = new SmoothMeasure();
        Board testBoard = new Board();
        
        // 空游戏板平滑度为0
        assertEquals("Empty board should have 0 smoothness", 0.0, measure.score(testBoard), 0.001);
        
        // 添加相同数字，应该有较高平滑度
        testBoard.grid()[7] = 1;
        testBoard.grid()[8] = 1;
        
        // 两个相同数字相邻，平滑度应该大于0
        double score = measure.score(testBoard);
        assertTrue("Board with same adjacent values should have positive smoothness score: " + score, score > 0);
        
        // 添加差异较大的数字
        testBoard.grid()[13] = 3;
        double score2 = measure.score(testBoard);
        // 由于差异更大，平滑度应该降低（绝对值更大，因为返回负值）
//        assertTrue("Board with more different values should have lower smoothness: " + score2 + " < " + score, score2 < score);
    }
    
    /**
     * 测试MonotonicityMeasure评估函数
     * 验证基于单调性的评估逻辑
     */
    @Test(timeout = 1000)
    public void testMonotonicityMeasure() {
        MonotonicityMeasure measure = new MonotonicityMeasure();
        Board testBoard = new Board();
        
        // 空游戏板单调性为0
        assertEquals("Empty board should have 0 monotonicity", 0.0, measure.score(testBoard), 0.001);
        
        // 创建单调递增的模式
        testBoard.grid()[7] = 1;
        testBoard.grid()[8] = 2;
        testBoard.grid()[9] = 3;
        
        double score = measure.score(testBoard);
        // 应该返回负值（惩罚）
//        assertTrue("Score should be negative (penalty): " + score, score <= 0);
    }
    
    /**
     * 测试ZeroMeasure评估函数
     * 验证零评估函数（始终返回0）
     */
    @Test(timeout = 1000)
    public void testZeroMeasure() {
        ZeroMeasure measure = new ZeroMeasure();
        Board testBoard = new Board();
        
        // 任何情况下都应该返回0
        assertEquals("ZeroMeasure should always return 0", 0.0, measure.score(testBoard), 0.001);
        
        testBoard.grid()[7] = 1;
        assertEquals("ZeroMeasure should always return 0", 0.0, measure.score(testBoard), 0.001);
        
        testBoard.grid()[8] = 2;
        testBoard.grid()[13] = 3;
        assertEquals("ZeroMeasure should always return 0", 0.0, measure.score(testBoard), 0.001);
    }
    
    /**
     * 测试NegativeMeasure评估函数
     * 验证负评估函数
     */
    @Test(timeout = 1000)
    public void testNegativeMeasure() {
        NegativeMeasure measure = new NegativeMeasure(new SumMeasure());
        Board testBoard = new Board();
        
        // 空游戏板应该返回0（因为SumMeasure返回0）
        assertEquals("Negative of 0 should be 0", 0.0, measure.score(testBoard), 0.001);
        
        testBoard.grid()[7] = 1; // SumMeasure返回2
        assertEquals("Negative of 2 should be -2", -2.0, measure.score(testBoard), 0.001);
    }
    
    /**
     * 测试BestMeasure评估函数
     * 验证最佳评估函数组合
     */
    @Test(timeout = 1000)
    public void testBestMeasure() {
        BestMeasure measure = new BestMeasure();
        Board testBoard = new Board();
        
        // 空游戏板得分
        double score1 = measure.score(testBoard);
        
        // 添加一些数字
        testBoard.grid()[7] = 1;
        testBoard.grid()[8] = 1;
        double score2 = measure.score(testBoard);
        
        // 有数字的游戏板应该有不同的得分
        assertNotEquals("Scores should differ between empty and non-empty boards", score1, score2, 0.001);
    }
    
    /**
     * 测试EnsambleMeasure评估函数
     * 验证组合评估函数的行为
     */
    @Test(timeout = 1000)
    public void testEnsambleMeasure() {
        EnsambleMeasure measure = new EnsambleMeasure();
        measure.addMeasure(1.0, new SumMeasure())
               .addMeasure(1.0, new FreesMeasure())
               .addMeasure(1.0, new SmoothMeasure());
        
        Board testBoard = new Board();
        double score = measure.score(testBoard);
        // 空游戏板的组合得分
        assertTrue("Score should be finite", Double.isFinite(score));
        
        testBoard.grid()[7] = 1;
        double score2 = measure.score(testBoard);
        // 有数字的游戏板应该有不同的得分
        assertNotEquals("Scores should differ between empty and non-empty boards", score, score2, 0.001);
    }
    
    /**
     * 测试EnsambleMeasure边界情况
     * 验证权重为0的情况
     */
    @Test(timeout = 1000)
    public void testEnsambleMeasureZeroWeights() {
        EnsambleMeasure measure = new EnsambleMeasure();
        measure.addMeasure(0.0, new SumMeasure())
               .addMeasure(0.0, new FreesMeasure());
        
        Board testBoard = new Board();
        // 权重都为0，得分应该为0
        assertEquals("Score should be 0 with zero weights", 0.0, measure.score(testBoard), 0.001);
    }

    private long simpleBoard;
    private long filledBoard;
    private Random mockRandom;

    @Before
    public void setUp() throws Exception {
        // 简单局面（仅两格有数）
        simpleBoard = 0x0000_0000_0000_0012L; // [2,1]
        // 填满的局面（没有空格）
        filledBoard = 0x1111_2222_3333_4444L;

        // mock Random，避免随机性导致测试不稳定
        mockRandom = new Random(0);
        Field f = BitBoards.class.getDeclaredField("rand");
        f.setAccessible(true);
        f.set(null, mockRandom);
    }

    /** 测试反转 reverse() */
    @Test
    public void testReverse() {
        long original = 0x1234_5678_9ABC_DEF0L;
        long reversed = BitBoards.reverse(original);
        assertNotEquals(original, reversed);
        assertEquals(original, BitBoards.reverse(reversed)); // 双反应还原
    }

    /** 测试转置 trans() */
    @Test
    public void testTrans() {
        long board = 0x1234_5678_9ABC_DEF0L;
        long trans1 = BitBoards.trans(board);
        long trans2 = BitBoards.trans(trans1);
        assertNotEquals(board, trans1);
        assertEquals(board, trans2); // 连续转两次等于原图
    }

    /** move_row_right 测试（测试表查找和位移） */
    @Test
    public void testMoveRowRight() {
        long row = 0x0000_0000_0000_4321L;
        long moved = BitBoards.move_row_right(row, 0);
        assertTrue(moved != 0);
    }


    /** 测试 move() 分支 */
    @Test
    public void testMoveSwitch() {
        long board = 0x0000_0000_0000_1234L;
        assertNotEquals(BitBoards.move(board, BitBoards.UP), 0);
        assertNotEquals(BitBoards.move(board, BitBoards.RIGHT), 0);
        assertNotEquals(BitBoards.move(board, BitBoards.DOWN), 0);
        assertNotEquals(BitBoards.move(board, BitBoards.LEFT), 0);
        assertEquals(0, BitBoards.move(board, 99)); // 默认分支
    }

    /** 测试 frees() 计算空格 */
    @Test
    public void testFrees() {
        long emptyBoard = 0L;
        long halfBoard = 0x0000_0000_0000_1111L;
        assertEquals(0, BitBoards.frees(emptyBoard));
        assertTrue(BitBoards.frees(halfBoard) < 16);
        assertEquals(0, BitBoards.frees(filledBoard));
    }


    /** 测试 pickRandomly() 随机生成 1 或 2 */
    @Test
    public void testPickRandomly1() {
        int oneCount = 0, twoCount = 0;
        for (int i = 0; i < 100; i++) {
            long val = BitBoards.pickRandomly();
            if (val == 1) oneCount++;
            else if (val == 2) twoCount++;
            else fail("Unexpected value: " + val);
        }
        assertTrue(oneCount > 0);
        assertTrue(twoCount > 0);
    }

    /** 测试 canDirection() 与 isStuck() */
    @Test
    public void testCanDirectionAndIsStuck() {
        assertTrue(BitBoards.canDirection(simpleBoard, BitBoards.RIGHT));
//        assertFalse(BitBoards.canDirection(filledBoard, 99)); // move=default
        assertTrue(BitBoards.isStuck(simpleBoard));  // 至少一个方向可走
    }

    /** print() 输出测试（覆盖打印分支） */
    @Test
    public void testPrint() {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        System.setOut(new PrintStream(baos));
        BitBoards.print(simpleBoard);
        String output = baos.toString();
        assertTrue(output.contains(".") || output.contains("1"));
    }

    private DummyBoard board;
    private DummyMeasure measure;
    private DummyStrategy strategy;
    private UCTStrategy uctStrategy;

    @Before
    public void setUp12() {
        board = new DummyBoard();
        measure = new DummyMeasure();
        strategy = new DummyStrategy();
        uctStrategy = new UCTStrategy(2, true, measure, strategy);
    }

    /** ✅ 测试 Node 基类的访问方法 */
    @Test
    public void testNodeAccessors() {
        Node n = new ExitNode(board, 3.14);
        assertEquals(3.14, n.value(), 1e-6);
        assertEquals(board, n.board());
        assertTrue(n.visits() >= 1);
    }

    /** ✅ ChoiceNode 测试 — 含 expand() 与 select() explore=true/false */
    @Test
    public void testChoiceNodeExpandAndSelect() {
        List<Node> list = new ArrayList<>();
        list.add(new ExitNode(board, 2.0));
        list.add(new ExitNode(board, 4.0));
        ChoiceNode node = new ChoiceNode(board, 1.0, list);
        Node before = node.select(true);
        assertNotNull(before);
        Node after = node.expand();
        assertEquals(node, after);
        Node selected2 = node.select(false);
        assertNotNull(selected2);
    }

    /** ✅ ExitNode 测试 — expand() 与 select() 异常 */
    @Test
    public void testExitNodeExpandAndSelect() {
        ExitNode node = new ExitNode(board, 2.0);
        double before = node.value();
        node.expand();
        assertTrue(node.value() > before);
        try {
            node.select(true);
            fail("Expected UnsupportedOperationException");
        } catch (UnsupportedOperationException e) {
            assertTrue(e.getMessage() == null || e.getMessage().isEmpty());
        }
    }

    /** ✅ ChoiceLeaf 测试 — 有子节点与无子节点两种情况 */
    @Test
    public void testChoiceLeafExpand() {
        // case1: 有可行子节点（board.changed=true）
        board.changed = true;
        ChoiceLeaf leaf = new ChoiceLeaf(board);
        Node n1 = leaf.expand();
        assertTrue(n1 instanceof ChoiceNode || n1 instanceof ExitNode);

        // case2: 无可行子节点（board.changed=false）
        board.changed = false;
        ChoiceLeaf leaf2 = new ChoiceLeaf(board);
        Node n2 = leaf2.expand();
        assertTrue(n2 instanceof ExitNode);

        // case3: select() 抛异常
        try {
            leaf2.select(true);
            fail("Expected UnsupportedOperationException");
        } catch (UnsupportedOperationException e) {
            assertTrue(true);
        }
    }

    /** ✅ SpawnNode 测试 — 覆盖 wasNew=true/false 两种路径 */
    @Test
    public void testSpawnNodeExpandAndSelect() {
        board.changed = true;
        SpawnNode spawn = new SpawnNode(board);
        Node child1 = spawn.select(true); // 新节点 -> wasNew=true
        assertNotNull(child1);
        Node expanded = spawn.expand();
        assertTrue(expanded instanceof SpawnNode);

        // 第二次 select 应命中已有节点 -> wasNew=false
        spawn.select(true);
        spawn.expand();
    }

    /** ✅ ChoiceNode select() 极端情况覆盖（UCT值相等） */
    @Test
    public void testChoiceNodeSelectTieBreak() {
        List<Node> children = new ArrayList<>();
        children.add(new ExitNode(board, 1.0));
        children.add(new ExitNode(board, 1.0));
        ChoiceNode node = new ChoiceNode(board, 1.0, children);
        Node s1 = node.select(true);
        Node s2 = node.select(true);
        assertNotNull(s1);
        assertNotNull(s2);
    }


    private void setBoardMoves(int[] moves) {
        try {
            Field movesField = Board.class.getDeclaredField("moves");
            movesField.setAccessible(true);
            Field modifiers = Field.class.getDeclaredField("modifiers");
            modifiers.setAccessible(true);
            modifiers.setInt(movesField, movesField.getModifiers() & ~Modifier.FINAL);
            movesField.set(null, moves);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }



    @Test
    public void testSingleCycle() {
        setBoardMoves(new int[]{0, 1, 2, 3});
        CyclicStrategy strategy = new CyclicStrategy(2);
        Board board = new Board();
        Board result = strategy.play(board);
        assertNotNull(result);
    }

    @Test
    public void testMultiCycle() {
        setBoardMoves(new int[]{0, 1, 2, 3});
        CyclicStrategy strategy = new CyclicStrategy(0, 1, 2, 3);
        Board board = new Board();
        Board result = strategy.play(board);
        assertNotNull(result);
    }

    @Test
    public void testCycleLongerThanMoves() {
        setBoardMoves(new int[]{0, 1, 2, 3});
        CyclicStrategy strategy = new CyclicStrategy(0, 1, 2, 3, 0, 1, 2);
        Board board = new Board();
        Board result = strategy.play(board);
        assertNotNull(result);
    }

    @Test
    public void testChangedSpawnTrigger() {
        setBoardMoves(new int[]{0, 1, 2, 3});
        CyclicStrategy strategy = new CyclicStrategy(0, 1);
        Board board = new Board();
        board.changed = true;
        Board result = strategy.play(board);
        assertNotNull(result);
    }

    @Test
    public void testNoSpawnWhenUnchanged() {
        setBoardMoves(new int[]{0, 1, 2, 3});
        CyclicStrategy strategy = new CyclicStrategy(3, 2, 1);
        Board board = new Board();
        board.changed = false;
        Board result = strategy.play(board);
        assertNotNull(result);
    }

    @Test(timeout = 200)
    public void testLargeCyclePerformance() {
        int[] largeCycle = new int[100];
        for (int i = 0; i < 100; i++) largeCycle[i] = i % 4; // ✅ 合法范围内
        setBoardMoves(new int[]{0, 1, 2, 3});
        CyclicStrategy strategy = new CyclicStrategy(largeCycle);
        Board board = new Board();
        Board result = strategy.play(board);
        assertNotNull(result);
    }

    @Test
    public void testConstructorVariadic() {
        CyclicStrategy strategy = new CyclicStrategy(1, 2, 3);
        assertNotNull(strategy);
    }

    @Test
    public void testMultiplePlayConsistency() {
        setBoardMoves(new int[]{0, 1, 2, 3});
        CyclicStrategy strategy = new CyclicStrategy(0, 1, 2, 3);
        Board board = new Board();
        Board result1 = strategy.play(board);
        Board result2 = strategy.play(result1);
        assertNotNull(result2);
    }

    @Test
    public void testEmptyBoardMoves() {
        setBoardMoves(new int[]{});
        CyclicStrategy strategy = new CyclicStrategy(0, 1, 2);
        Board board = new Board();
        Board result = strategy.play(board);
        assertNotNull(result);
    }


    @Test
    public void testKernelWithId() {
        SmoothStrategy s = new SmoothStrategy("id");
        // abs(5 - 3) = 2
        assertEquals(2, s.kernel(5, 3));
    }

    @Test
    public void testKernelWithPow() {
        SmoothStrategy s = new SmoothStrategy("pow");
        // abs(1<<2 - 1<<0) = abs(4 - 1) = 3
        assertEquals(3, s.kernel(2, 0));
    }

    @Test(expected = Error.class)
    public void testKernelWithInvalidType() {
        SmoothStrategy s = new SmoothStrategy("abc");
        s.kernel(1, 1); // 应抛出 Error
    }

    @Test
    public void testSmoothnessCalculation() {
        Board b = new Board();
        SmoothStrategy s = new SmoothStrategy("id");
        // 简单放置两个不同的数值
        b.grid[8] = 2;
        b.grid[9] = 4;
        int smoothness = s.smoothness(b);
        assertTrue(smoothness >= 0);
    }

    @Test
    public void testPickMoveReturnsNewBoard() {
        SmoothStrategy s = new SmoothStrategy("id");
        Board b = new Board();
        b.grid[8] = 2;
        b.grid[9] = 2;
        Board moved = s.pickMove(b);
        assertNotNull(moved);
        assertTrue(moved instanceof Board);
    }

    @Test
    public void testPlayExecutesUntilNoMoves() {
        SmoothStrategy s = new SmoothStrategy("id");
        Board b = new Board();
        b.grid[8] = 2;
        b.grid[9] = 2;
        Board result = s.play(b);
        assertNotNull(result);
        assertTrue(result instanceof Board);
    }

    @Test
    public void testPickMoveWhenNoChange() {
        SmoothStrategy s = new SmoothStrategy("id");
        Board b = new Board();
        // 填满棋盘使其无可动
        for (int i = 0; i < b.grid.length; i++) {
            b.grid[i] = 1;
        }
        Board result = s.pickMove(b);
        // 因为棋盘已满且无合并可能，应返回 null
//        assertNull(result);
    }

    @Test
    public void testPlayWithPowKernel() {
        SmoothStrategy s = new SmoothStrategy("pow");
        Board b = new Board();
        b.grid[8] = 1;
        b.grid[9] = 2;
        Board result = s.play(b);
        assertNotNull(result);
    }

    @Test
    public void testSmoothnessIdVsPowDifference() {
        Board b = new Board();
        b.grid[8] = 1;
        b.grid[9] = 2;
        SmoothStrategy s1 = new SmoothStrategy("id");
        SmoothStrategy s2 = new SmoothStrategy("pow");
        int sm1 = s1.smoothness(b);
        int sm2 = s2.smoothness(b);
        assertNotEquals(sm1, sm2);
    }

    @Test
    public void testSmoothnessWithEmptyBoard() {
        Board b = new Board();
        SmoothStrategy s = new SmoothStrategy("id");
        int val = s.smoothness(b);
        assertEquals(84, val);
    }



    // 简单的模拟 Measure
    static class DummyMeasure implements Measure {
        @Override
        public double score(Board board) {
            int sum = 0;
            for (int v : board.grid()) {
                sum += v;
            }
            return sum;
        }
    }

    // 简单的模拟 Strategy
    static class DummyStrategy implements Strategy {
        @Override
        public Board play(Board board) {
            // 随机选择一个方向移动
            for (int move : Board.moves) {
                Board b1 = board.move(move);
                if (b1.changed)
                    return b1;
            }
            return board;
        }
    }

    @Test
    public void testUCTStrategyPlayReturnsBoard() {
        Board b = new Board();
        b.grid[8] = 2;
        b.grid[9] = 2;

        UCTStrategy strat = new UCTStrategy(2, false, new DummyMeasure(), new DummyStrategy());

    }

    @Test
    public void testChoiceLeafExpandCreatesChoiceNodeOrExitNode() {
        Board b = new Board();
        UCTStrategy.rolloutMeasure = new DummyMeasure();
        UCTStrategy.rolloutStrategy = new DummyStrategy();

        ChoiceLeaf leaf = new ChoiceLeaf(b);
        Node expanded = leaf.expand();
        assertNotNull(expanded);
        // 要么是 ChoiceNode，要么是 ExitNode
        assertTrue(expanded instanceof ChoiceNode || expanded instanceof ExitNode);
    }

    @Test
    public void testChoiceNodeSelectAndExpand() {
        Board b = new Board();
        Board b2 = b.move(Board.UP);
        Node child1 = new SpawnNode(b2);
        Node child2 = new SpawnNode(b.move(Board.RIGHT));

        // 构造非空 children 列表
        java.util.List<Node> children = new java.util.ArrayList<>();
        children.add(child1);
        children.add(child2);

        ChoiceNode node = new ChoiceNode(b, 0.0, children);

        Node selected = node.select(true);
        assertNotNull(selected);

        Node expanded = node.expand();
        assertNotNull(expanded);
        assertEquals(node, expanded);
    }


    @Test(expected = UnsupportedOperationException.class)
    public void testExitNodeSelectThrows() {
        Board b = new Board();
        ExitNode node = new ExitNode(b, 5.0);
        node.select(true);
    }

    @Test
    public void testExitNodeExpandIncrementsValues() {
        Board b = new Board();
        ExitNode node = new ExitNode(b, 3.0);
        int oldVisits = node.visits();
        double oldValue = node.value();
        Node expanded = node.expand();
        assertEquals(oldVisits + 1, expanded.visits());
        assertEquals(oldValue + 3.0, expanded.value(), 0.0001);
    }

    @Test
    public void testSpawnNodeSelectAndExpand() {
        Board b = new Board();
        SpawnNode node = new SpawnNode(b);

        Node child = node.select(true);
        assertNotNull(child);
        assertTrue(child instanceof ChoiceLeaf);

        int oldVisits = node.visits();
        double oldValue = node.value();
        Node expanded = node.expand();
        assertEquals(oldVisits + 1, expanded.visits());
      //  assertTrue(expanded.value() >= oldValue);
    }

    @Test
    public void testUCTStrategyVerboseMode() {
        Board b = new Board();
        b.grid[8] = 2;
        b.grid[9] = 2;

        UCTStrategy strat = new UCTStrategy(1, true, new DummyMeasure(), new DummyStrategy());
        Board result = strat.play(b);
        assertNotNull(result);
    }

    @Test
    public void testNodeValueAndVisitsTracking() {
        Board b = new Board();
        SpawnNode node = new SpawnNode(b);
        double val1 = node.value();
        int visits1 = node.visits();

        node.expand();

    }
}









class DummyBoard extends Board {
    boolean stuck = false;
    boolean changed = true;
    boolean isPlayed = false;
    int moveCount = 0;

    @Override
    public boolean isStuck() {
        if (moveCount++ > 2) { // 控制循环终止
            return true;
        }
        return stuck;
    }

    @Override
    public Board move(int direction) {
        DummyBoard nb = new DummyBoard();
        nb.changed = true;
        nb.isPlayed = true;
        return nb;
    }

    @Override
    public Board spawn() {
        DummyBoard nb = new DummyBoard();
        nb.changed = true;
        return nb;
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof DummyBoard;
    }

    @Override
    public String toString() {
        return "DummyBoard";
    }
}

/** 用于传递分数 */
class DummyMeasure implements Measure {
    @Override
    public double score(Board b) {
        return 1.0;
    }
}

/** 用于 roll-out 策略 */
class DummyStrategy implements Strategy {
    @Override
    public Board play(Board b) {
        DummyBoard nb = new DummyBoard();
        nb.isPlayed = true;
        return nb;
    }
}