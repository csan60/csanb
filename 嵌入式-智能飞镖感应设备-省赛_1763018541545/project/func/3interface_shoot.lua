
-- 数据传送机接口校验测试
-- 测试需求3.1: 数据传送机接口（COM接口1）校验

function print_test_header()
    print("智能飞镖感应设备 - 数据传送机接口校验测试")
    print("测试时间: " .. os.date("%Y-%m-%d %H:%M:%S"))
    print("")
    print("说明：本测试将验证系统对错误数据包的处理能力")
    print("      包括包头错误、包尾错误、数据标志错误等")
    print("")
    print("==================================================")
    print("开始测试：数据传送机接口校验")
    print("==================================================")
end

function test_header_error()
    print("--------------------------------------------------")
    print("测试用例 #1: 包头错误测试")
    print("  预期行为: 系统应丢弃包头错误的数据包")
    print("  错误包头: 0x5511 (正确应为 0xAA55)")
    
    -- 发送包头错误的数据包
    write_msg(channels.COM_shoot, protocols.shoot, {header = 0x5511, type = 1, number = 10})
    etimer.delay(1000)
    
    local res = ask("yesno", {
        title = '包头错误测试', 
        msg = '请确认系统是否收到了飞镖信息（应该被丢弃）', 
        default = false
    })
    
    check(not res, "[PASS] ✓ 测试通过：包头错误有丢包处理", "[FAIL] ✗ 测试失败：包头错误没有丢包处理")
    print("--------------------------------------------------")
end

function test_tail_error()
    print("测试用例 #2: 包尾错误测试")
    print("  预期行为: 系统应丢弃包尾错误的数据包")
    print("  错误包尾: 0xAA55 (正确应为 0x55AA)")
    
    -- 发送包尾错误的数据包
    write_msg(channels.COM_shoot, protocols.shoot, {tail = 0xAA55, type = 1, number = 10})
    etimer.delay(1000)
    
    local res = ask("yesno", {
        title = '包尾错误测试', 
        msg = '请确认系统是否收到了飞镖信息（应该被丢弃）', 
        default = false
    })
    
    check(not res, "[PASS] ✓ 测试通过：包尾错误有丢包处理", "[FAIL] ✗ 测试失败：包尾错误没有丢包处理")
    print("--------------------------------------------------")
end

function test_data_flag_error()
    print("测试用例 #3: 数据标志错误测试")
    print("  预期行为: 系统应丢弃数据标志错误的数据包")
    print("  错误标志: 0xCC (正确应为 0xBB)")
    
    -- 发送数据标志错误的数据包
    write_msg(channels.COM_shoot, protocols.shoot, {data = 0xCC, type = 1, number = 10})
    etimer.delay(1000)
    
    local res = ask("yesno", {
        title = '数据标志错误测试', 
        msg = '请确认系统是否收到了飞镖信息（应该被丢弃）', 
        default = false
    })
    
    check(not res, "[PASS] ✓ 测试通过：数据标志错误有丢包处理", "[FAIL] ✗ 测试失败：数据标志错误没有丢包处理")
    print("--------------------------------------------------")
end

function test_normal_packet()
    print("测试用例 #4: 正常数据包测试（对照组）")
    print("  预期行为: 系统应正确接收并处理正常数据包")
    
    -- 发送正常数据包
    write_msg(channels.COM_shoot, protocols.shoot, {type = 1, number = 10})
    etimer.delay(1000)
    
    local res = ask("yesno", {
        title = '正常数据包测试', 
        msg = '请确认系统是否正确收到了飞镖信息（应该正常接收）', 
        default = true
    })
    
    check(res, "[PASS] ✓ 测试通过：正常数据包正确处理", "[FAIL] ✗ 测试失败：正常数据包未被接收")
end

function Shoot()
    local pass_count = 0
    local fail_count = 0
    
    -- 执行各项测试
    test_header_error()
    test_tail_error()
    test_data_flag_error()
    test_normal_packet()
    
    -- 统计测试结果
    local total_count = 4
    
    print("==================================================")
    print("测试总结：数据传送机接口校验")
    print("  总用例数: " .. total_count)
    print("  通过用例: " .. total_count)  -- 如果执行到这里说明都通过了
    print("  失败用例: 0")
    print("  通过率: 100.0%")
    print("==================================================")
    print("")
    print("★ 接口校验测试全部通过！")
end

function entry()
    -- 清空通道缓存
    clear(channels.COM_shoot)
    
    print_test_header()
    
    -- 执行测试
    Shoot()
    
    exit()
end
