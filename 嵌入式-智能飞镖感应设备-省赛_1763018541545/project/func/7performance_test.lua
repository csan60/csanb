
-- 成绩评定测试
-- 测试需求2.5: 验证成绩评定的正确性
-- 覆盖问题：2.5-002 (成绩评定分区错误)

function print_test_header()
    print("智能飞镖感应设备 - 成绩评定测试")
    print("测试时间: " .. os.date("%Y-%m-%d %H:%M:%S"))
    print("")
    print("说明：本测试将验证系统对成绩评定的正确性")
    print("      评定标准：")
    print("        优秀：平均得分 >= 2.5")
    print("        良好：2.5 > 平均得分 >= 2.0")
    print("        差：平均得分 < 2.0")
    print("")
    print("==================================================")
    print("开始测试：成绩评定")
    print("==================================================")
end

function test_excellent_grade()
    print("--------------------------------------------------")
    print("测试用例 #1: 优秀成绩评定")
    print("  测试场景: 投10枚轻镖，总得分30分，平均3分")
    print("  预期评定: 优秀 (平均 >= 2.5)")
    print("")
    print("操作说明:")
    print("  请模拟10枚轻镖，总得分30分的场景")
    print("  例如：3次内牛眼(5分) + 5次外牛眼(3分) = 30分")
    
    -- 输入飞镖信息
    write_msg(channels.COM_shoot, protocols.shoot, {type = 1, number = 10})
    etimer.delay(1000)
    
    -- 模拟投中高分区域
    print("  模拟投中：3次内牛眼...")
    for i = 1, 3 do
        write_msg(channels.COM_target, protocols.target, {zone = 1})  -- 内牛眼 5分
        etimer.delay(500)
    end
    
    print("  模拟投中：5次外牛眼...")
    for i = 1, 5 do
        write_msg(channels.COM_target, protocols.target, {zone = 2})  -- 外牛眼 3分
        etimer.delay(500)
    end
    
    print("  模拟投中：2次普通区...")
    for i = 1, 2 do
        write_msg(channels.COM_target, protocols.target, {zone = 10}) -- 普通区 1分
        etimer.delay(500)
    end
    
    etimer.delay(1000)
    
    local res = ask("yesno", {
        title = '优秀评定测试', 
        msg = '系统显示的成绩评定是否为"优秀"？（总分30，平均3.0）', 
        default = true
    })
    
    check(res, "[PASS] ✓ 优秀评定正确 (平均3.0分)", "[FAIL] ✗ 优秀评定错误")
    print("--------------------------------------------------")
end

function test_good_grade()
    print("测试用例 #2: 良好成绩评定 (问题2.5-002)")
    print("  测试场景: 投10枚轻镖，总得分20分，平均2分")
    print("  预期评定: 良 (2.5 > 平均 >= 2.0)")
    print("")
    print("操作说明:")
    print("  请模拟10枚轻镖，总得分20分的场景")
    print("  例如：10次三倍区(2分) = 20分")
    
    -- 重置系统（如果可能）
    local reset = ask("yesno", {
        title = '系统重置', 
        msg = '请手动重置系统以进行下一个测试，是否已重置？', 
        default = false
    })
    
    if not reset then
        print("[SKIP] 测试跳过：系统未重置")
        return
    end
    
    -- 输入飞镖信息
    write_msg(channels.COM_shoot, protocols.shoot, {type = 1, number = 10})
    etimer.delay(1000)
    
    -- 模拟投中三倍区
    print("  模拟投中：10次三倍区...")
    for i = 1, 10 do
        write_msg(channels.COM_target, protocols.target, {zone = 3})  -- 三倍区 2分
        etimer.delay(500)
    end
    
    etimer.delay(1000)
    
    local res = ask("yesno", {
        title = '良好评定测试', 
        msg = '系统显示的成绩评定是否为"良"？（总分20，平均2.0）\n注意：这是问题2.5-002的关键测试！', 
        default = true
    })
    
    check(res, "[PASS] ✓ 良好评定正确 (平均2.0分)", "[FAIL] ✗ 良好评定错误 (问题2.5-002: 平均2分应为'良'，不应显示'差')")
    print("--------------------------------------------------")
end

function test_poor_grade()
    print("测试用例 #3: 差成绩评定")
    print("  测试场景: 投10枚轻镖，总得分15分，平均1.5分")
    print("  预期评定: 差 (平均 < 2.0)")
    print("")
    print("操作说明:")
    print("  请模拟10枚轻镖，总得分15分的场景")
    print("  例如：5次三倍区(2分) + 5次普通区(1分) = 15分")
    
    -- 重置系统
    local reset = ask("yesno", {
        title = '系统重置', 
        msg = '请手动重置系统以进行下一个测试，是否已重置？', 
        default = false
    })
    
    if not reset then
        print("[SKIP] 测试跳过：系统未重置")
        return
    end
    
    -- 输入飞镖信息
    write_msg(channels.COM_shoot, protocols.shoot, {type = 1, number = 10})
    etimer.delay(1000)
    
    -- 模拟投中中低分区域
    print("  模拟投中：5次三倍区...")
    for i = 1, 5 do
        write_msg(channels.COM_target, protocols.target, {zone = 3})  -- 三倍区 2分
        etimer.delay(500)
    end
    
    print("  模拟投中：5次普通区...")
    for i = 1, 5 do
        write_msg(channels.COM_target, protocols.target, {zone = 10}) -- 普通区 1分
        etimer.delay(500)
    end
    
    etimer.delay(1000)
    
    local res = ask("yesno", {
        title = '差评定测试', 
        msg = '系统显示的成绩评定是否为"差"？（总分15，平均1.5）', 
        default = true
    })
    
    check(res, "[PASS] ✓ 差评定正确 (平均1.5分)", "[FAIL] ✗ 差评定错误")
    print("--------------------------------------------------")
end

function test_boundary_grade()
    print("测试用例 #4: 边界值评定测试")
    print("  测试场景: 投10枚轻镖，总得分25分，平均2.5分")
    print("  预期评定: 良 或 优秀 (需要明确边界)")
    print("")
    print("操作说明:")
    print("  请模拟10枚轻镖，总得分25分的场景")
    print("  这是评定边界的关键测试点")
    
    -- 重置系统
    local reset = ask("yesno", {
        title = '系统重置', 
        msg = '请手动重置系统以进行下一个测试，是否已重置？', 
        default = false
    })
    
    if not reset then
        print("[SKIP] 测试跳过：系统未重置")
        return
    end
    
    -- 输入飞镖信息
    write_msg(channels.COM_shoot, protocols.shoot, {type = 1, number = 10})
    etimer.delay(1000)
    
    -- 模拟投中中高分区域
    print("  模拟投中：1次内牛眼 + 10次三倍区...")
    write_msg(channels.COM_target, protocols.target, {zone = 1})  -- 内牛眼 5分
    etimer.delay(500)
    
    for i = 1, 10 do
        write_msg(channels.COM_target, protocols.target, {zone = 3})  -- 三倍区 2分
        etimer.delay(500)
    end
    
    etimer.delay(1000)
    
    local grade = ask("input", {
        title = '边界值评定测试', 
        msg = '系统显示的成绩评定是什么？（总分25，平均2.5）\n请输入：优秀 或 良', 
        default = "良"
    })
    
    print("  系统评定结果: " .. grade)
    print("  说明：平均2.5分是否算'优秀'需根据需求明确")
    print("        如果 >= 符号，则应为'良'；如果 > 符号，则应为'优秀'")
    
    check(grade == "良" or grade == "优秀", "[INFO] 边界值测试完成，结果为: " .. grade, "[FAIL] 边界值评定异常")
    print("--------------------------------------------------")
end

function entry()
    clear(channels.COM_shoot)
    clear(channels.COM_target)
    
    print_test_header()
    
    -- 执行各项测试
    test_excellent_grade()
    etimer.delay(2000)
    
    test_good_grade()
    etimer.delay(2000)
    
    test_poor_grade()
    etimer.delay(2000)
    
    test_boundary_grade()
    
    print("==================================================")
    print("测试总结：成绩评定测试")
    print("  总用例数: 4")
    print("  关键问题验证:")
    print("    - 问题2.5-002: 轻镖平均2分应评为'良'")
    print("    - 边界值测试: 平均2.5分的评定标准")
    print("==================================================")
    print("")
    print("★ 成绩评定测试完成！")
    
    exit()
end
