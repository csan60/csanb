
-- 得分逻辑测试
-- 测试需求2.4: 验证不同飞镖类型和区域的得分是否正确
-- 覆盖问题：2.4-001 (轻镖内牛眼得分错误), 2.4-002 (重镖三倍区得分错误)

function print_test_header()
    print("智能飞镖感应设备 - 得分逻辑测试")
    print("测试时间: " .. os.date("%Y-%m-%d %H:%M:%S"))
    print("")
    print("说明：本测试将验证系统对不同飞镖类型和区域的得分计算")
    print("      包括轻镖、重镖在各区域的得分逻辑")
    print("")
    print("==================================================")
    print("开始测试：得分逻辑")
    print("==================================================")
end

function test_light_dart_inner_bull()
    print("--------------------------------------------------")
    print("测试用例 #1: 轻镖命中内牛眼 (问题2.4-001)")
    print("  飞镖类型: 1 (轻镖)")
    print("  投中区域: 1 (内牛眼)")
    print("  预期得分: 5分")
    
    -- 先输入飞镖信息
    write_msg(channels.COM_shoot, protocols.shoot, {type = 1, number = 1})
    etimer.delay(1000)
    
    -- 发送区域信息
    write_msg(channels.COM_target, protocols.target, {zone = 1})
    etimer.delay(1000)
    
    local res = ask("yesno", {
        title = '轻镖-内牛眼测试', 
        msg = '系统显示的得分是否为5分？', 
        default = true
    })
    
    check(res, "[PASS] ✓ 轻镖内牛眼得分正确 (5分)", "[FAIL] ✗ 轻镖内牛眼得分错误 (应为5分)")
    print("--------------------------------------------------")
end

function test_light_dart_outer_bull()
    print("测试用例 #2: 轻镖命中外牛眼")
    print("  飞镖类型: 1 (轻镖)")
    print("  投中区域: 2 (外牛眼)")
    print("  预期得分: 3分")
    
    write_msg(channels.COM_shoot, protocols.shoot, {type = 1, number = 1})
    etimer.delay(1000)
    write_msg(channels.COM_target, protocols.target, {zone = 2})
    etimer.delay(1000)
    
    local res = ask("yesno", {
        title = '轻镖-外牛眼测试', 
        msg = '系统显示的得分是否为3分？', 
        default = true
    })
    
    check(res, "[PASS] ✓ 轻镖外牛眼得分正确 (3分)", "[FAIL] ✗ 轻镖外牛眼得分错误 (应为3分)")
    print("--------------------------------------------------")
end

function test_light_dart_triple()
    print("测试用例 #3: 轻镖命中三倍区")
    print("  飞镖类型: 1 (轻镖)")
    print("  投中区域: 3 (三倍区)")
    print("  预期得分: 2分")
    
    write_msg(channels.COM_shoot, protocols.shoot, {type = 1, number = 1})
    etimer.delay(1000)
    write_msg(channels.COM_target, protocols.target, {zone = 3})
    etimer.delay(1000)
    
    local res = ask("yesno", {
        title = '轻镖-三倍区测试', 
        msg = '系统显示的得分是否为2分？', 
        default = true
    })
    
    check(res, "[PASS] ✓ 轻镖三倍区得分正确 (2分)", "[FAIL] ✗ 轻镖三倍区得分错误 (应为2分)")
    print("--------------------------------------------------")
end

function test_heavy_dart_inner_bull()
    print("测试用例 #4: 重镖命中内牛眼")
    print("  飞镖类型: 2 (重镖)")
    print("  投中区域: 1 (内牛眼)")
    print("  预期得分: 3分")
    
    write_msg(channels.COM_shoot, protocols.shoot, {type = 2, number = 1})
    etimer.delay(1000)
    write_msg(channels.COM_target, protocols.target, {zone = 1})
    etimer.delay(1000)
    
    local res = ask("yesno", {
        title = '重镖-内牛眼测试', 
        msg = '系统显示的得分是否为3分？', 
        default = true
    })
    
    check(res, "[PASS] ✓ 重镖内牛眼得分正确 (3分)", "[FAIL] ✗ 重镖内牛眼得分错误 (应为3分)")
    print("--------------------------------------------------")
end

function test_heavy_dart_triple()
    print("测试用例 #5: 重镖命中三倍区 (问题2.4-002)")
    print("  飞镖类型: 2 (重镖)")
    print("  投中区域: 3 (三倍区)")
    print("  预期得分: 2分")
    
    write_msg(channels.COM_shoot, protocols.shoot, {type = 2, number = 1})
    etimer.delay(1000)
    write_msg(channels.COM_target, protocols.target, {zone = 3})
    etimer.delay(1000)
    
    local res = ask("yesno", {
        title = '重镖-三倍区测试', 
        msg = '系统显示的得分是否为2分？（注意：不是1分）', 
        default = true
    })
    
    check(res, "[PASS] ✓ 重镖三倍区得分正确 (2分)", "[FAIL] ✗ 重镖三倍区得分错误 (应为2分，不是1分)")
    print("--------------------------------------------------")
end

function test_normal_zone()
    print("测试用例 #6: 普通区域得分")
    print("  飞镖类型: 1 (轻镖)")
    print("  投中区域: 10 (普通区)")
    print("  预期得分: 1分")
    
    write_msg(channels.COM_shoot, protocols.shoot, {type = 1, number = 1})
    etimer.delay(1000)
    write_msg(channels.COM_target, protocols.target, {zone = 10})
    etimer.delay(1000)
    
    local res = ask("yesno", {
        title = '普通区域测试', 
        msg = '系统显示的得分是否为1分？', 
        default = true
    })
    
    check(res, "[PASS] ✓ 普通区域得分正确 (1分)", "[FAIL] ✗ 普通区域得分错误 (应为1分)")
    print("--------------------------------------------------")
end

function entry()
    clear(channels.COM_shoot)
    clear(channels.COM_target)
    
    print_test_header()
    
    -- 执行各项测试
    test_light_dart_inner_bull()
    etimer.delay(1000)
    
    test_light_dart_outer_bull()
    etimer.delay(1000)
    
    test_light_dart_triple()
    etimer.delay(1000)
    
    test_heavy_dart_inner_bull()
    etimer.delay(1000)
    
    test_heavy_dart_triple()
    etimer.delay(1000)
    
    test_normal_zone()
    
    print("==================================================")
    print("测试总结：得分逻辑测试")
    print("  总用例数: 6")
    print("  关键问题验证:")
    print("    - 问题2.4-001: 轻镖内牛眼得分 (5分)")
    print("    - 问题2.4-002: 重镖三倍区得分 (2分)")
    print("==================================================")
    print("")
    print("★ 得分逻辑测试完成！")
    
    exit()
end
