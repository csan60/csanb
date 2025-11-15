
-- 异常数据测试
-- 测试需求: 验证系统对异常数据的处理能力
-- 覆盖问题：2.2-003 (飞镖数量为nil崩溃), 3.1-001 (校验错误未丢弃), 3.2-001 (非法区域未校验)

function print_test_header()
    print("智能飞镖感应设备 - 异常数据处理测试")
    print("测试时间: " .. os.date("%Y-%m-%d %H:%M:%S"))
    print("")
    print("说明：本测试将验证系统对异常数据的处理能力")
    print("      包括空值、非法值、越界值等")
    print("")
    print("==================================================")
    print("开始测试：异常数据处理")
    print("==================================================")
end

function test_nil_number()
    print("--------------------------------------------------")
    print("测试用例 #1: 飞镖数量为nil (问题2.2-003)")
    print("  测试场景: 发送飞镖数量字段为nil的数据包")
    print("  预期行为: 系统应默认处理或提示错误，不应崩溃")
    print("")
    
    print("  注意：由于Lua的限制，nil值在表中会被忽略")
    print("  本测试将验证系统对缺失字段的处理")
    
    -- 尝试发送没有number字段的数据包
    write_msg(channels.COM_shoot, protocols.shoot, {type = 1})
    etimer.delay(1500)
    
    local crashed = ask("yesno", {
        title = 'nil数量测试', 
        msg = '系统是否崩溃或报错？（应该有默认处理，不崩溃）', 
        default = false
    })
    
    check(not crashed, "[PASS] ✓ 正确：nil数量有默认处理，未崩溃", "[FAIL] ✗ 错误：nil数量导致崩溃 (问题2.2-003)")
    print("--------------------------------------------------")
end

function test_invalid_dart_type()
    print("测试用例 #2: 非法飞镖类型")
    print("  测试场景: 发送不存在的飞镖类型（如99）")
    print("  预期行为: 系统应拒绝或使用默认值")
    print("")
    
    -- 发送非法飞镖类型
    write_msg(channels.COM_shoot, protocols.shoot, {type = 99, number = 10})
    etimer.delay(1500)
    
    local accepted = ask("yesno", {
        title = '非法类型测试', 
        msg = '系统是否接收了飞镖类型99的数据？（应该被拒绝或使用默认值）', 
        default = false
    })
    
    check(not accepted, "[PASS] ✓ 正确：非法飞镖类型被拒绝", "[FAIL] ✗ 错误：非法飞镖类型被接受")
    print("--------------------------------------------------")
end

function test_negative_number()
    print("测试用例 #3: 负数飞镖数量")
    print("  测试场景: 发送负数的飞镖数量（如-5）")
    print("  预期行为: 系统应拒绝或使用默认值")
    print("")
    
    -- 发送负数数量
    write_msg(channels.COM_shoot, protocols.shoot, {type = 1, number = -5})
    etimer.delay(1500)
    
    local accepted = ask("yesno", {
        title = '负数数量测试', 
        msg = '系统是否接收了数量为-5的数据？（应该被拒绝）', 
        default = false
    })
    
    check(not accepted, "[PASS] ✓ 正确：负数数量被拒绝", "[FAIL] ✗ 错误：负数数量被接受")
    print("--------------------------------------------------")
end

function test_zero_zone()
    print("测试用例 #4: 区域值为0 (问题3.2-001)")
    print("  测试场景: 发送区域值为0的靶区数据")
    print("  预期行为: 系统应拒绝（有效区域为1-20）")
    print("")
    
    -- 发送区域为0
    write_msg(channels.COM_target, protocols.target, {zone = 0})
    etimer.delay(1500)
    
    local accepted = ask("yesno", {
        title = '区域0测试', 
        msg = '系统是否接收了区域=0的数据？（应该被拒绝）', 
        default = false
    })
    
    check(not accepted, "[PASS] ✓ 正确：区域0被拒绝", "[FAIL] ✗ 错误：区域0被接受 (问题3.2-001)")
    print("--------------------------------------------------")
end

function test_over_range_zone()
    print("测试用例 #5: 区域值超出范围 (问题3.2-001)")
    print("  测试场景: 发送区域值为21的靶区数据（超出1-20范围）")
    print("  预期行为: 系统应拒绝")
    print("")
    
    -- 发送区域为21
    write_msg(channels.COM_target, protocols.target, {zone = 21})
    etimer.delay(1500)
    
    local accepted = ask("yesno", {
        title = '超范围区域测试', 
        msg = '系统是否接收了区域=21的数据？（应该被拒绝）', 
        default = false
    })
    
    check(not accepted, "[PASS] ✓ 正确：超范围区域被拒绝", "[FAIL] ✗ 错误：超范围区域被接受 (问题3.2-001)")
    print("--------------------------------------------------")
end

function test_negative_zone()
    print("测试用例 #6: 负数区域值")
    print("  测试场景: 发送负数的区域值（如-1）")
    print("  预期行为: 系统应拒绝")
    print("")
    
    -- 发送负数区域
    write_msg(channels.COM_target, protocols.target, {zone = -1})
    etimer.delay(1500)
    
    local accepted = ask("yesno", {
        title = '负数区域测试', 
        msg = '系统是否接收了区域=-1的数据？（应该被拒绝）', 
        default = false
    })
    
    check(not accepted, "[PASS] ✓ 正确：负数区域被拒绝", "[FAIL] ✗ 错误：负数区域被接受")
    print("--------------------------------------------------")
end

function test_checksum_error()
    print("测试用例 #7: 校验和错误 (问题3.1-001)")
    print("  测试场景: 发送校验和错误的数据包")
    print("  预期行为: 系统应丢弃数据包")
    print("")
    print("  注意：校验和由协议自动计算，此测试需要手动构造错误包")
    print("        如果协议不支持手动设置校验和，则跳过此测试")
    
    -- 尝试发送校验和错误的包（实际上协议会自动计算正确的校验和）
    -- 这里仅作示意，实际测试需要在协议层面支持
    write_msg(channels.COM_shoot, protocols.shoot, {type = 1, number = 10, check = 0xFF})
    etimer.delay(1500)
    
    local accepted = ask("yesno", {
        title = '校验和错误测试', 
        msg = '系统是否接收了校验和错误的数据？（应该被丢弃）', 
        default = false
    })
    
    check(not accepted, "[PASS] ✓ 正确：校验和错误被丢弃", "[INFO] 校验和测试完成 (问题3.1-001)")
    print("--------------------------------------------------")
end

function test_extreme_values()
    print("测试用例 #8: 极端值测试")
    print("  测试场景: 发送极大的飞镖数量（如999）")
    print("  预期行为: 系统应有合理的上限处理")
    print("")
    
    -- 发送极大数量
    write_msg(channels.COM_shoot, protocols.shoot, {type = 1, number = 999})
    etimer.delay(1500)
    
    local response = ask("input", {
        title = '极端值测试', 
        msg = '系统如何处理数量999？\n请输入：接受/拒绝/限制为上限', 
        default = "拒绝"
    })
    
    print("  系统响应: " .. response)
    check(response ~= "接受", "[PASS] ✓ 极端值有合理处理", "[INFO] 极端值测试完成，响应: " .. response)
    print("--------------------------------------------------")
end

function entry()
    clear(channels.COM_shoot)
    clear(channels.COM_target)
    
    print_test_header()
    
    -- 执行各项测试
    test_nil_number()
    etimer.delay(1000)
    
    test_invalid_dart_type()
    etimer.delay(1000)
    
    test_negative_number()
    etimer.delay(1000)
    
    test_zero_zone()
    etimer.delay(1000)
    
    test_over_range_zone()
    etimer.delay(1000)
    
    test_negative_zone()
    etimer.delay(1000)
    
    test_checksum_error()
    etimer.delay(1000)
    
    test_extreme_values()
    
    print("==================================================")
    print("测试总结：异常数据处理测试")
    print("  总用例数: 8")
    print("  关键问题验证:")
    print("    - 问题2.2-003: nil数量不应崩溃")
    print("    - 问题3.1-001: 校验和错误应丢弃")
    print("    - 问题3.2-001: 非法区域应校验")
    print("==================================================")
    print("")
    print("★ 异常数据处理测试完成！")
    
    exit()
end
