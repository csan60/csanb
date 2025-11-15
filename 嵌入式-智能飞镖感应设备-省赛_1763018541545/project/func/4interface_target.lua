-- ==============================================================================
-- 测试脚本：靶体接口校验测试
-- 测试目标：验证靶体接口对错误数据包的处理能力
-- 需求编号：接口需求 - 靶体接口校验
-- 测试类型：接口测试（错误处理）
-- ==============================================================================

require("e_checker")

-- 靶体接口校验测试函数
function test_target_interface_validation()
    print("==================================================")
    print("开始测试：靶体接口校验")
    print("==================================================")
    
    -- 清空通道缓冲区
    clear(channels.COM_target)
    etimer.delay(500)
    
    local test_count = 0
    local pass_count = 0
    
    -- 测试1：包头错误
    print("--------------------------------------------------")
    print("测试用例 #1: 包头错误测试")
    print("  预期行为: 系统应丢弃包头错误的数据包")
    print("  错误包头: 0x5511 (正确应为 0xAA55)")
    test_count = test_count + 1
    
    write_msg(channels.COM_target, protocols.target, {
        header = 0x5511,  -- 错误的包头
        zone = 1
    })
    etimer.delay(1000)
    
    local res1 = ask("yesno", {
        title = "包头错误测试",
        msg = "请确认是否收到了靶体信息？\n(应该不显示，选择\"否\")",
        default = false
    })
    
    if check(not res1,
            "✓ 测试通过：包头错误有丢包处理",
            "✗ 测试失败：包头错误没有丢包处理") then
        pass_count = pass_count + 1
    end
    
    etimer.delay(500)
    clear(channels.COM_target)
    
    -- 测试2：包尾错误
    print("--------------------------------------------------")
    print("测试用例 #2: 包尾错误测试")
    print("  预期行为: 系统应丢弃包尾错误的数据包")
    print("  错误包尾: 0xAA55 (正确应为 0x55AA)")
    test_count = test_count + 1
    
    write_msg(channels.COM_target, protocols.target, {
        tail = 0xAA55,  -- 错误的包尾（反了）
        zone = 2
    })
    etimer.delay(1000)
    
    local res2 = ask("yesno", {
        title = "包尾错误测试",
        msg = "请确认是否收到了靶体信息？\n(应该不显示，选择\"否\")",
        default = false
    })
    
    if check(not res2,
            "✓ 测试通过：包尾错误有丢包处理",
            "✗ 测试失败：包尾错误没有丢包处理") then
        pass_count = pass_count + 1
    end
    
    etimer.delay(500)
    clear(channels.COM_target)
    
    -- 测试3：数据标志1错误
    print("--------------------------------------------------")
    print("测试用例 #3: 数据标志1错误测试")
    print("  预期行为: 系统应丢弃数据标志1错误的数据包")
    print("  错误标志1: 0xBB (正确应为 0xCC)")
    test_count = test_count + 1
    
    write_msg(channels.COM_target, protocols.target, {
        data1 = 0xBB,  -- 错误的数据标志1
        zone = 3
    })
    etimer.delay(1000)
    
    local res3 = ask("yesno", {
        title = "数据标志1错误测试",
        msg = "请确认是否收到了靶体信息？\n(应该不显示，选择\"否\")",
        default = false
    })
    
    if check(not res3,
            "✓ 测试通过：数据标志1错误有丢包处理",
            "✗ 测试失败：数据标志1错误没有丢包处理") then
        pass_count = pass_count + 1
    end
    
    etimer.delay(500)
    clear(channels.COM_target)
    
    -- 测试4：数据标志2错误
    print("--------------------------------------------------")
    print("测试用例 #4: 数据标志2错误测试")
    print("  预期行为: 系统应丢弃数据标志2错误的数据包")
    print("  错误标志2: 0x22 (正确应为 0x11)")
    test_count = test_count + 1
    
    write_msg(channels.COM_target, protocols.target, {
        data2 = 0x22,  -- 错误的数据标志2
        zone = 4
    })
    etimer.delay(1000)
    
    local res4 = ask("yesno", {
        title = "数据标志2错误测试",
        msg = "请确认是否收到了靶体信息？\n(应该不显示，选择\"否\")",
        default = false
    })
    
    if check(not res4,
            "✓ 测试通过：数据标志2错误有丢包处理",
            "✗ 测试失败：数据标志2错误没有丢包处理") then
        pass_count = pass_count + 1
    end
    
    etimer.delay(500)
    clear(channels.COM_target)
    
    -- 测试5：区域超出范围（边界值测试）
    print("--------------------------------------------------")
    print("测试用例 #5: 区域超出范围测试")
    print("  预期行为: 系统应拒绝超出有效范围(1-20)的区域值")
    print("  测试值: 0 和 21")
    test_count = test_count + 1
    
    -- 测试区域0
    write_msg(channels.COM_target, protocols.target, {zone = 0})
    etimer.delay(800)
    
    -- 测试区域21
    write_msg(channels.COM_target, protocols.target, {zone = 21})
    etimer.delay(800)
    
    local res5 = ask("yesno", {
        title = "区域边界测试",
        msg = "请确认是否收到了区域0或区域21的信息？\n(应该不显示，选择\"否\")",
        default = false
    })
    
    if check(not res5,
            "✓ 测试通过：区域边界值正确处理",
            "✗ 测试失败：区域边界值处理错误") then
        pass_count = pass_count + 1
    end
    
    etimer.delay(500)
    clear(channels.COM_target)
    
    -- 测试6：正常数据包（对照测试）
    print("--------------------------------------------------")
    print("测试用例 #6: 正常数据包测试（对照组）")
    print("  预期行为: 系统应正确接收并处理正常数据包")
    test_count = test_count + 1
    
    write_msg(channels.COM_target, protocols.target, {zone = 10})
    etimer.delay(1000)
    
    local res6 = ask("yesno", {
        title = "正常数据包测试",
        msg = "请确认是否收到了靶体信息（区域10）？\n(应该显示，选择\"是\")",
        default = true
    })
    
    if check(res6,
            "✓ 测试通过：正常数据包正确处理",
            "✗ 测试失败：正常数据包未正确处理") then
        pass_count = pass_count + 1
    end
    
    -- 输出测试总结
    print("==================================================")
    print("测试总结：靶体接口校验")
    print(string.format("  总用例数: %d", test_count))
    print(string.format("  通过用例: %d", pass_count))
    print(string.format("  失败用例: %d", test_count - pass_count))
    print(string.format("  通过率: %.1f%%", (pass_count / test_count) * 100))
    print("==================================================")
    
    -- 返回测试结果
    return pass_count == test_count
end

-- 主入口函数
function entry()
    print("智能飞镖感应设备 - 靶体接口校验测试")
    print("测试时间: " .. os.date("%Y-%m-%d %H:%M:%S"))
    print("")
    print("说明：本测试将验证系统对错误数据包的处理能力")
    print("      包括包头错误、包尾错误、数据标志错误、边界值等")
    print("")
    
    -- 执行测试
    local result = test_target_interface_validation()
    
    -- 输出最终结果
    print("")
    if result then
        print("★ 接口校验测试全部通过！")
    else
        print("★ 接口校验测试存在失败用例，请检查系统实现！")
    end
    
    -- 退出测试
    exit()
end
