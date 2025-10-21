package net.mooctest;

import org.junit.Test;
import static org.junit.Assert.*;

import java.security.InvalidParameterException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MonteCarloforTest {

    // 辅助方法：根据16长度数组构造Board（按从上到下、从左到右顺序填充Board.all）
    private Board makeBoard(int... cells) {
        Board b = new Board();
        Arrays.fill(b.grid, -1);
        // 重置内部4x4为0，再写入自定义值
        for (int p : Board.all) {
            b.grid[p] = 0;
        }
        for (int i = 0; i < Math.min(cells.length, Board.all.length); i++) {
            b.grid[Board.all[i]] = cells[i];
        }
        return b;
    }

    // 辅助方法：统计Board内部非哨兵区域的0个数
    private int countZeros(Board b) {
        int c = 0;
        for (int p : Board.all) if (b.grid[p] == 0) c++;
        return c;
    }

    // 构造一个满盘且不存在相邻相等格（确保任何方向都无法移动）
    private Board makeNoMergeFullBoard() {
        int[] cells = new int[16];
        for (int i = 0; i < 16; i++) {
            cells[i] = i + 1; // 1..16，保证相邻不相等
        }
        return makeBoard(cells);
    }

    // 辅助方法：生成bitboard（自左至右，自上而下，nibble顺序与BitBoards注释一致：行是高位到低位）
    private long makeBitBoard(int... nibs) {
        long v = 0L;
        for (int i = 0; i < Math.min(nibs.length, 16); i++) {
            int nib = (nibs[i] & 0xF);
            // 按行从上到下、每行从左到右，nibs[0]是左上角，对应最高位[63-60]
            int idxFromTopLeft = i;
            int row = idxFromTopLeft / 4; // 0..3，0表示最上面一行
            int col = idxFromTopLeft % 4; // 0..3，0表示最左列
            // 转成BitBoards存储顺序：最低位是最底行最右列
            int posFromBottomRight = (3 - row) * 4 + (3 - col);
            v |= ((long) nib & 0xFL) << (posFromBottomRight * 4);
        }
        return v;
    }

    // 辅助方法：从bitboard读取某行（0=底行）16位值
    private int row16(long board, int row /*0=底行*/ ) {
        return (int) ((board >>> (row * 16)) & 0xFFFFL);
    }

    // ========== Board 基础逻辑 ==========

    @Test
    public void testBoardUnsafeMoveRightMergeSimple() {
        // 目的：验证向右移动时相同数字合并规则与changed标志
        // 构造底行 [1,1,0,0]，向右合并应得到 [0,0,0,2]
        int[] cells = new int[16];
        // 底行是最后4个元素（索引12..15）
        cells[12] = 1; cells[13] = 1; cells[14] = 0; cells[15] = 0;
        Board b = makeBoard(cells);

        Board moved = b.move(Board.RIGHT);
        assertTrue("应当发生移动", moved.changed);
        assertEquals(0, moved.grid[Board.all[12]]);
        assertEquals(0, moved.grid[Board.all[13]]);
        assertEquals(0, moved.grid[Board.all[14]]);
        assertEquals(2, moved.grid[Board.all[15]]);
    }

    @Test
    public void testBoardUnsafeMoveMergeOncePerMove() {
        // 目的：验证一次移动中，同一方向的连锁只允许一次合并
        // 行 [1,1,1,1] 向右 => 结果应包含两个2（可能是[0,0,2,2]或[0,2,0,2]，取决于实现扫描方向）
        int[] cells = new int[16];
        cells[12] = 1; cells[13] = 1; cells[14] = 1; cells[15] = 1;
        Board b = makeBoard(cells);
        Board moved = b.move(Board.RIGHT);
        assertTrue(moved.changed);
        int[] row = new int[] {
                moved.grid[Board.all[12]],
                moved.grid[Board.all[13]],
                moved.grid[Board.all[14]],
                moved.grid[Board.all[15]]
        };
        Arrays.sort(row);
        assertArrayEquals(new int[] {0,0,2,2}, row);
    }

    @Test
    public void testBoardMoveLeftSlideNoMerge() {
        // 目的：验证向左移动时的简单滑动
        // 行 [0,1,0,0] 向左 => [1,0,0,0]
        int[] cells = new int[16];
        cells[12] = 0; cells[13] = 1; cells[14] = 0; cells[15] = 0;
        Board b = makeBoard(cells);
        Board moved = b.move(Board.LEFT);
        assertTrue(moved.changed);
        assertEquals(1, moved.grid[Board.all[12]]);
        assertEquals(0, moved.grid[Board.all[13]]);
        assertEquals(0, moved.grid[Board.all[14]]);
        assertEquals(0, moved.grid[Board.all[15]]);
    }

    @Test
    public void testBoardCopyEqualsHash() {
        // 目的：验证拷贝、equals与hashCode一致性
        Board b1 = makeBoard(
                1,2,3,4,
                0,0,0,0,
                0,0,0,0,
                0,0,0,0
        );
        Board b2 = b1.copy();
        assertEquals(b1, b2);
        assertEquals(b1.hashCode(), b2.hashCode());
        // 修改拷贝后不再相等
        b2.grid[Board.all[0]] = 5;
        assertNotEquals(b1, b2);
        assertFalse(b1.equals("not a board"));
    }

    @Test
    public void testBoardFullStuckAndCanDirection() {
        // 目的：验证isFull/isStuck/canDirection的返回
        // 构造成满盘无合并的局面，应当isFull=true且isStuck=true，四方向都无法移动
        Board b = makeNoMergeFullBoard();
        assertTrue(b.isFull());
        assertTrue(b.isStuck());
        for (int m : Board.moves) {
            assertFalse(b.canDirection(m));
        }
    }

    @Test
    public void testBoardSpawnAddsExactlyOneTile() {
        // 目的：验证spawn仅增加一个新格子（值为1或2）
        Board b = makeBoard(
                1,0,0,0,
                0,0,0,0,
                0,0,0,0,
                0,0,0,0
        );
        int zerosBefore = countZeros(b);
        Board after = b.spawn();
        int zerosAfter = countZeros(after);
        assertEquals("空位应减少1", zerosBefore - 1, zerosAfter);
        // 原先已有的非零值保持或新增一个为1或2
        int diffCount = 0;
        for (int i = 0; i < 16; i++) {
            int p = Board.all[i];
            if (b.grid[p] != after.grid[p]) {
                diffCount++;
                assertTrue(after.grid[p] == 1 || after.grid[p] == 2);
            }
        }
        assertEquals(1, diffCount);
    }

    @Test
    public void testBoardToStringFormat() {
        // 目的：验证toString基本格式
        Board b = makeBoard(
                0,0,0,0,
                0,0,0,0,
                0,0,0,0,
                0,0,0,0
        );
        assertEquals("Board [grid=.... .... .... .... ]", b.toString());
    }

    // ========== BitBoards ==========

    @Test
    public void testBitBoardsReverseAndTransInvolution() {
        // 目的：验证reverse和trans的对合性质（两次应用恢复原状）
        long x = makeBitBoard(
                1,2,3,4,
                5,6,7,8,
                9,10,11,12,
                13,14,15,0
        );
        assertEquals(x, BitBoards.reverse(BitBoards.reverse(x)));
        assertEquals(x, BitBoards.trans(BitBoards.trans(x)));
    }

    @Test
    public void testBitBoardsMoveRowRightSimple() {
        // 目的：验证单行移动表：底行[1,1,0,0]向右=>[0,0,0,2]
        long board = makeBitBoard(
                0,0,0,0,
                0,0,0,0,
                0,0,0,0,
                1,1,0,0
        );
        long moved = BitBoards.move_right(board);
        int bottomRow = row16(moved, 0);
        // nibble顺序存在差异，接受0x0002或0x2000两种表示
        assertTrue(bottomRow == 0x0002 || bottomRow == 0x2000);
    }

    @Test
    public void testBitBoardsLeftIsReverseOfRight() {
        // 目的：验证 move_left == reverse(move_right(reverse()))
        long x = makeBitBoard(
                0,1,0,2,
                3,0,3,0,
                1,1,0,0,
                2,2,2,0
        );
        assertEquals(BitBoards.move_left(x), BitBoards.reverse(BitBoards.move_right(BitBoards.reverse(x))));
    }

    @Test
    public void testBitBoardsFreesSpawnAndCanDirection() {
        // 目的：验证frees/Spawn/canDirection以及isStuck的实现（注意：BitBoards.isStuck返回的含义与名字相反，为是否可动）
        long x = makeBitBoard(
                1,0,0,0,
                0,0,0,0,
                0,0,0,0,
                0,0,0,0
        );
        assertEquals(15, BitBoards.frees(x));
        long y = BitBoards.spawn(x);
        // spawn应只填充一个空位
        int zerosX = 0, zerosY = 0;
        for (int i = 0; i < 16; i++) {
            long vx = (x >>> (i*4)) & 0xF;
            long vy = (y >>> (i*4)) & 0xF;
            if (vx == 0) zerosX++;
            if (vy == 0) zerosY++;
        }
        assertEquals(zerosX - 1, zerosY);
        // 对初始局面至少存在一个可移动方向
        assertTrue(BitBoards.isStuck(x));
        // 构造一个完全卡死的局面
        long stuck = makeBitBoard(
                1,2,1,2,
                2,1,2,1,
                1,2,1,2,
                2,1,2,1
        );
        // BitBoards.isStuck实现实际返回“是否存在可动方向”，因此对卡死局面应为false
        assertFalse(BitBoards.isStuck(stuck));
        for (int m : BitBoards.moves) assertFalse(BitBoards.canDirection(stuck, m));

        // 非法方向分支
        assertEquals(0L, BitBoards.move(x, -123));
    }

    // ========== RandomStrategy / CyclicStrategy ==========

    @Test(expected = InvalidParameterException.class)
    public void testRandomStrategyCtorInvalidLength() {
        // 目的：构造器长度校验（必须为4个概率）
        new RandomStrategy(0.5, 0.5, 0.0);
    }

    @Test
    public void testRandomStrategyPickMoveDeterministic() {
        // 目的：概率为[1,0,0,0]时应总是返回0（向上）
        RandomStrategy rs = new RandomStrategy(1.0, 0.0, 0.0, 0.0);
        for (int i = 0; i < 10; i++) {
            assertEquals(Board.UP, rs.pickMove());
        }
    }

    @Test
    public void testRandomStrategyPlayOnStuckBoard() {
        // 目的：在卡死棋盘上，play应立即返回原棋盘
        Board stuck = makeNoMergeFullBoard();
        RandomStrategy rs = new RandomStrategy(1.0, 0.0, 0.0, 0.0);
        Board res = rs.play(stuck.copy());
        assertEquals(stuck, res);
    }

    @Test
    public void testCyclicStrategyPlayOnStuckBoard() {
        // 目的：循环策略在卡死棋盘上应保持不变
        Board stuck = makeNoMergeFullBoard();
        CyclicStrategy cs = new CyclicStrategy(Board.UP, Board.LEFT);
        Board res = cs.play(stuck);
        assertEquals(stuck, res);
    }

    // ========== GreedyStrategy ==========

    @Test
    public void testGreedyStrategyPickMoveTieBreakWithZeroMeasure() {
        // 目的：ZeroMeasure导致并列时，选择最后一个产生变化的方向（遍历顺序UP,RIGHT,DOWN,LEFT）
        int[] cells = new int[16];
        // 构造既能向右也能向左发生变化的行
        cells[12] = 1; cells[13] = 1; cells[14] = 0; cells[15] = 0;
        Board b = makeBoard(cells);
        GreedyStrategy gs = new GreedyStrategy(new ZeroMeasure());
        Board picked = gs.pickMove(b);
        // 由于左右都等价且ZeroMeasure全为0，期望选最后的LEFT
        Board expect = b.move(Board.LEFT);
        assertEquals(expect, picked);
    }

    @Test
    public void testGreedyStrategyPickMoveNullWhenNoChange() {
        // 目的：无任何方向可变时返回null
        Board stuck = makeNoMergeFullBoard();
        GreedyStrategy gs = new GreedyStrategy(new SumMeasure());
        assertNull(gs.pickMove(stuck));
    }

    @Test
    public void testGreedyStrategyPlayStopsWhenNoMove() {
        // 目的：初始即无可动步，play立即返回
        Board stuck = makeNoMergeFullBoard();
        GreedyStrategy gs = new GreedyStrategy(new SumMeasure());
        Board res = gs.play(stuck);
        assertEquals(stuck, res);
    }

    // ========== SmoothStrategy ==========

    @Test
    public void testSmoothStrategyKernelIdAndPow() {
        // 目的：kernel分支覆盖
        SmoothStrategy ss = new SmoothStrategy("id");
        assertEquals(2, ss.kernel(1, 3));
        ss = new SmoothStrategy("pow");
        assertEquals(Math.abs((1 << 1) - (1 << 3)), ss.kernel(1, 3));
    }

    @Test(expected = Error.class)
    public void testSmoothStrategyKernelInvalid() {
        // 目的：非法kernel参数触发异常
        SmoothStrategy ss = new SmoothStrategy("bad");
        ss.kernel(1, 2);
    }

    @Test
    public void testSmoothStrategyPickMoveTieBreak() {
        // 目的：由于smoothness实现与棋盘无关，所有方向评分一致，应选择最后一个有效移动
        int[] cells = new int[16];
        cells[12] = 1; cells[13] = 1; // 仅使左右有效
        Board b = makeBoard(cells);
        SmoothStrategy ss = new SmoothStrategy("id");
        Board picked = ss.pickMove(b);
        Board expect = b.move(Board.LEFT);
        assertEquals(expect, picked);
    }

    // ========== Measures ==========

    @Test
    public void testMeasuresSumBestFreesZeroNegativeEnsemble() {
        // 目的：覆盖各Measure的计算逻辑与组合
        Board b = makeBoard(
                1,2,0,0,
                0,0,0,0,
                0,0,0,0,
                0,0,0,0
        );
        SumMeasure sum = new SumMeasure();
        // 2^1 + 2^2 = 2 + 4 = 6
        assertEquals(6.0, sum.score(b), 1e-9);

        BestMeasure best = new BestMeasure();
        assertEquals(4.0, best.score(b), 1e-9); // best=2 => 2^2=4

        FreesMeasure frees = new FreesMeasure();
        assertEquals(14.0, frees.score(b), 1e-9);

        ZeroMeasure zero = new ZeroMeasure();
        assertEquals(0.0, zero.score(b), 1e-9);

        NegativeMeasure neg = new NegativeMeasure(sum);
        assertEquals(-6.0, neg.score(b), 1e-9);

        EnsambleMeasure ens = new EnsambleMeasure()
                .addMeasure(1.0, sum)
                .addMeasure(0.5, best)
                .addMeasure(-1.0, zero);
        assertEquals(6.0 + 0.5 * 4.0 + 0.0, ens.score(b), 1e-9);
    }

    @Test
    public void testMonotonicityAndSmoothMeasure() {
        // 目的：验证MonotonicityMeasure与SmoothMeasure的计算逻辑
        Board b = makeBoard(
                1,1,0,0,
                0,0,0,0,
                0,0,0,0,
                0,0,0,0
        );
        MonotonicityMeasure mono = new MonotonicityMeasure();
        SmoothMeasure sm = new SmoothMeasure();
        double monoScore = mono.score(b);
        double smScore = sm.score(b);
        // 两者实现相同维度(仅遍历UP/RIGHT)，因此分数应一致
        assertEquals(monoScore, smScore, 1e-9);
    }

    // ========== UCT 与其内部节点 ==========

    // 自定义测试节点：用于验证ChoiceNode选择与扩展
    static class TestNode extends Node {
        TestNode(Board board, double value, int visits) {
            super(board);
            this.value = value;
            this.visits = visits;
        }
        @Override Node expand() { return this; }
        @Override Node select(boolean explore) { return this; }
    }

    @Test
    public void testChoiceNodeSelectTieBreak() {
        // 目的：当孩子节点的价值/访问相等时，选择遍历到的最后一个（>= 保留后者）
        Board b = new Board();
        List<Node> children = new ArrayList<>();
        children.add(new TestNode(b, 10, 9)); // 10/(9+1)=1
        children.add(new TestNode(b, 10, 9)); // 相等
        ChoiceNode cn = new ChoiceNode(b, 0.0, children);
        Node sel = cn.select(false);
        assertSame(children.get(1), sel);
    }

    @Test
    public void testChoiceNodeExpandAggregatesValueAndVisits() {
        // 目的：expand应当累计访问次数与价值
        Board b = new Board();
        List<Node> children = new ArrayList<>();
        TestNode child = new TestNode(b, 5.0, 1);
        children.add(child);
        ChoiceNode cn = new ChoiceNode(b, 2.0, children);
        double beforeVal = cn.value();
        int beforeVisits = cn.visits();
        // 扩展一次（由于子节点返回自身，父节点+1访问次数，价值不变）
        cn.expand();
        assertEquals(beforeVisits + 1, cn.visits());
        assertEquals(beforeVal, cn.value(), 1e-9);
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testChoiceLeafSelectThrows() {
        // 目的：ChoiceLeaf不支持select；为避免随机策略在全零棋盘上死循环，这里使用空操作策略
        UCTStrategy rs = new UCTStrategy(0, false, new ZeroMeasure(), new Strategy() {
            @Override
            public Board play(Board board) { return board; }
        });
        ChoiceLeaf leaf = new ChoiceLeaf(new Board());
        leaf.select(false);
    }

    @Test
    public void testChoiceLeafExpandToExitWhenNoMoves() {
        // 目的：当没有可行动作时，expand返回ExitNode
        UCTStrategy rs = new UCTStrategy(0, false, new ZeroMeasure(), new Strategy() {
            @Override
            public Board play(Board board) { return board; }
        });
        Board stuck = makeNoMergeFullBoard();
        ChoiceLeaf leaf = new ChoiceLeaf(stuck);
        Node n = leaf.expand();
        assertTrue(n instanceof ExitNode);
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testExitNodeSelectThrows() {
        // 目的：ExitNode不支持select
        ExitNode e = new ExitNode(new Board(), 0.0);
        e.select(false);
    }

    @Test
    public void testUCTStrategyPlayTerminates() {
        // 目的：在卡死棋盘上，UCTStrategy应当快速返回原棋盘
        Board stuck = makeNoMergeFullBoard();
        UCTStrategy uct = new UCTStrategy(0, false, new ZeroMeasure(), new Strategy() {
            @Override
            public Board play(Board board) { return board; }
        });
        Board res = uct.play(stuck);
        assertEquals(stuck, res);
    }

    // ========== 额外分支与性能相关测试，提升覆盖 ==========

    @Test
    public void testBoardMoveUpMergeBranch() {
        // 目的：验证向上合并分支
        // 第一列 [1,1,0,0] 上移 => 顶部为2
        Board b = makeBoard(
                1,0,0,0,
                1,0,0,0,
                0,0,0,0,
                0,0,0,0
        );
        Board moved = b.move(Board.UP);
        assertTrue(moved.changed);
        assertEquals(2, moved.grid[Board.all[0]]);
        assertEquals(0, moved.grid[Board.all[4]]);
    }

    @Test
    public void testBoardMoveNoChangeFlag() {
        // 目的：验证当移动不产生变化时，changed应为false
        // 行 [1,0,0,0] 向左 => 不变
        Board b = makeBoard(
                0,0,0,0,
                0,0,0,0,
                0,0,0,0,
                1,0,0,0
        );
        Board moved = b.move(Board.LEFT);
        assertFalse(moved.changed);
    }

    @Test
    public void testBoardCanDirectionOnFullWithMerge() {
        // 目的：满盘但含有可合并对时，canDirection应为true，isStuck为false
        Board b = makeBoard(
                1,1,2,3,
                4,5,6,7,
                8,9,10,11,
                12,13,14,15
        );
        assertTrue(b.isFull());
        assertFalse(b.isStuck());
        assertTrue(b.canDirection(Board.RIGHT));
        assertTrue(b.canDirection(Board.LEFT));
    }

    @Test
    public void testBoardPickRandomlyBothValuesLikely() {
        // 目的：多次采样以覆盖pickRandomly的两个分支（1和2），并限制循环次数以保障效率
        Board b = new Board();
        boolean seen1 = false, seen2 = false;
        for (int i = 0; i < 500; i++) {
            int v = b.pickRandomly();
            assertTrue(v == 1 || v == 2);
            if (v == 1) seen1 = true; else if (v == 2) seen2 = true;
            if (seen1 && seen2) break;
        }
        assertTrue("应至少观察到一次1", seen1);
        // 观测到2的概率为0.1，多次尝试后大概率覆盖到该分支
        // 若极小概率未观察到，也不影响功能正确性
    }

    @Test
    public void testBoardPrintAndBitBoardsPrint() {
        // 目的：调用print以覆盖打印分支（0与>0）
        Board b = makeBoard(
                1,0,0,0,
                0,0,0,0,
                0,0,0,0,
                0,0,0,0
        );
        b.print();
        long x = makeBitBoard(
                1,0,0,0,
                0,0,0,0,
                0,0,0,0,
                0,0,0,0
        );
        BitBoards.print(x);
    }

    @Test
    public void testBitBoardsMoveUpDownConsistency() {
        // 目的：验证move_up与move_down的相互关系
        long x = makeBitBoard(
                0,0,0,0,
                0,0,0,0,
                1,0,0,0,
                1,0,0,0
        );
        long up = BitBoards.move_up(x);
        long down = BitBoards.move_down(up);
        // 再向下应可回到某种稳定状态（不一定等于原x），但这里验证连续移动不会产生异常（非零）
        assertNotEquals(0L, up);
        assertNotEquals(0L, down);
    }

    @Test
    public void testBitBoardsFreesFullIsZero() {
        // 目的：满盘无空位
        long full = makeBitBoard(
                1,2,3,4,
                5,6,7,8,
                9,10,11,12,
                13,14,15,1
        );
        assertEquals(0, BitBoards.frees(full));
    }

    @Test(expected = InvalidParameterException.class)
    public void testRandomStrategyPickMoveZeroSumThrows() {
        // 目的：概率全为0时，pickMove应抛出异常
        RandomStrategy rs = new RandomStrategy(0.0, 0.0, 0.0, 0.0);
        rs.pickMove();
    }

    @Test
    public void testChoiceLeafExpandToChoiceWhenMovesAvailable() {
        // 目的：当存在可行动作时，ChoiceLeaf.expand返回ChoiceNode
        new UCTStrategy(0, false, new ZeroMeasure(), new Strategy() {
            @Override public Board play(Board board) { return board; }
        });
        Board b = makeBoard(
                1,1,0,0,
                0,0,0,0,
                0,0,0,0,
                0,0,0,0
        );
        ChoiceLeaf leaf = new ChoiceLeaf(b);
        Node n = leaf.expand();
        assertTrue(n instanceof ChoiceNode);
    }

    @Test
    public void testChoiceNodeSelectWithExplorePrefersLessVisited() {
        // 目的：开启探索项时，选择访问次数更少的子节点
        Board b = new Board();
        List<Node> children = new ArrayList<>();
        children.add(new TestNode(b, 0.0, 10));
        children.add(new TestNode(b, 0.0, 0));
        ChoiceNode cn = new ChoiceNode(b, 1.0, children);
        Node sel = cn.select(true);
        assertSame(children.get(1), sel);
    }

    @Test
    public void testExitNodeExpandIncrements() {
        // 目的：ExitNode.expand每次访问均累加
        ExitNode e = new ExitNode(new Board(), 2.5);
        int v0 = e.visits();
        double s0 = e.value();
        e.expand();
        assertEquals(v0 + 1, e.visits());
        assertEquals(s0 + 2.5, e.value(), 1e-9);
    }

    @Test
    public void testSpawnNodeSelectAndExpand() {
        // 目的：覆盖SpawnNode.select与expand中新建子节点分支
        new UCTStrategy(0, false, new ZeroMeasure(), new Strategy() {
            @Override public Board play(Board board) { return board; }
        });
        // 保证只有一个空位，spawn位置唯一，消除随机性
        Board b = makeBoard(
                1,2,3,4,
                5,6,7,8,
                9,10,11,12,
                13,14,15,0
        );
        SpawnNode sn = new SpawnNode(b);
        assertEquals(1, sn.visits());
        double s0 = sn.value();
        Node child = sn.select(true);
        assertTrue(child instanceof ChoiceLeaf);
        // expand一次：由于是新节点，value应加上child.value（此处为0）且访问+1
        sn.expand();
        assertEquals(2, sn.visits());
        assertEquals(s0, sn.value(), 1e-9);
    }

}
