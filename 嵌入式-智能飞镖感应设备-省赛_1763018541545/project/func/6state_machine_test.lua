
-- 状态机测试
-- 测试需求2.2, 2.3, 2.5, 2.6: 验证系统状态机的正确性
-- 覆盖问题：2.2-002 (非准备状态仍能接收), 2.3-001 (无倒计时), 
--          2.5-001 (时间结束后仍接收), 2.6-001 (非结束状态点击复位)

function print_test_header()
    print("智能飞镖感应设备 - 状态机测试")
    print("测试时间: " .. os.date("%Y-%m-%d %H:%M:%S"))
    print("")
    print("说明：本测试将验证系统状态机的正确性")
    print("      包括状态转换、输入限制、倒计时等")
    print("")
    print("==================================================")
    print("开始测试：系统状态机")
    print("==================================================")
end

function test_non_ready_state_input()
    print("--------------------------------------------------")
    print("测试用例 #1: 非准备状态下输入飞镖信息 (问题2.2-002)")
    print("  测试场景: 在'投镖结束'状态下尝试输入飞镖信息")
    print("  预期行为: 系统应拒绝接收飞镖信息")
    print("")
    print("操作步骤:")
    print("  1. 请先将系统切换到'投镖结束'状态")
    print("  2. 然后观察系统是否接收以下飞镖信息")
    
    local res1 = ask("yesno", {
        title = '状态确认', 
        msg = '请确认系统当前是否处于"投镖结束"状态？', 
        default = false
    })
    
    if res1 then
        -- 在非准备状态下发送飞镖信息
        write_msg(channels.COM_shoot, protocols.shoot, {type = 1, number = 10})
        etimer.delay(1500)
        
        local res2 = ask("yesno", {
            title = '非准备状态输入测试', 
            msg = '系统是否接收了飞镖信息？（应该被拒绝）', 
            default = false
        })
        
        check(not res2, "[PASS] ✓ 正确：非准备状态拒绝飞镖输入", "[FAIL] ✗ 错误：非准备状态仍接收飞镖输入 (问题2.2-002)")
    else
        print("[SKIP] 测试跳过：无法切换到投镖结束状态")
    end
    
    print("--------------------------------------------------")
end

function test_countdown_start()
    print("测试用例 #2: 点击开始后倒计时启动 (问题2.3-001)")
    print("  测试场景: 在准备状态点击'开始'按钮")
    print("  预期行为: 系统应启动倒计时并进入'开始投镖'状态")
    print("")
    print("操作步骤:")
    print("  1. 请确保系统处于'准备投镖'状态")
    print("  2. 点击'开始'按钮")
    print("  3. 观察是否启动倒计时")
    
    local res1 = ask("yesno", {
        title = '状态确认', 
        msg = '请确认系统当前是否处于"准备投镖"状态？', 
        default = false
    })
    
    if res1 then
        local res2 = ask("yesno", {
            title = '倒计时测试', 
            msg = '点击"开始"按钮后，系统是否启动了倒计时？', 
            default = true
        })
        
        check(res2, "[PASS] ✓ 正确：点击开始后倒计时启动", "[FAIL] ✗ 错误：点击开始后无倒计时 (问题2.3-001)")
    else
        print("[SKIP] 测试跳过：系统未处于准备投镖状态")
    end
    
    print("--------------------------------------------------")
end

function test_timeout_reject_input()
    print("测试用例 #3: 时间结束后拒绝得分信息 (问题2.5-001)")
    print("  测试场景: 倒计时结束后发送靶区信息")
    print("  预期行为: 系统应拒绝接收靶区信息，得分不变")
    print("")
    print("操作步骤:")
    print("  1. 请等待倒计时结束")
    print("  2. 然后观察系统是否还接收靶区信息")
    
    local res1 = ask("yesno", {
        title = '倒计时状态确认', 
        msg = '倒计时是否已结束？', 
        default = false
    })
    
    if res1 then
        -- 记录当前得分
        local res2 = ask("input", {
            title = '当前得分', 
            msg = '请输入当前显示的得分（用于对比）：', 
            default = "0"
        })
        
        -- 发送靶区信息
        write_msg(channels.COM_target, protocols.target, {zone = 1})
        etimer.delay(1500)
        
        local res3 = ask("yesno", {
            title = '超时输入测试', 
            msg = '系统是否接收了靶区信息并增加了得分？（应该不增加）', 
            default = false
        })
        
        check(not res3, "[PASS] ✓ 正确：超时后拒绝靶区信息", "[FAIL] ✗ 错误：超时后仍接收得分 (问题2.5-001)")
    else
        print("[SKIP] 测试跳过：倒计时未结束")
    end
    
    print("--------------------------------------------------")
end

function test_reset_in_non_end_state()
    print("测试用例 #4: 非结束状态下点击复位 (问题2.6-001)")
    print("  测试场景: 在'准备投镖'状态下点击复位按钮")
    print("  预期行为: 系统应无反应，数据不应被清空")
    print("")
    print("操作步骤:")
    print("  1. 请确保系统处于'准备投镖'状态")
    print("  2. 先输入一些飞镖信息")
    print("  3. 然后点击'复位'按钮")
    print("  4. 观察数据是否被清空")
    
    -- 先输入飞镖信息
    write_msg(channels.COM_shoot, protocols.shoot, {type = 1, number = 10})
    etimer.delay(1000)
    
    local res1 = ask("yesno", {
        title = '数据输入确认', 
        msg = '系统是否显示了刚输入的飞镖信息？', 
        default = true
    })
    
    if res1 then
        local res2 = ask("yesno", {
            title = '非结束状态复位测试', 
            msg = '在当前状态（非投镖结束）下点击"复位"按钮，数据是否被清空？（应该不清空）', 
            default = false
        })
        
        check(not res2, "[PASS] ✓ 正确：非结束状态下复位无效", "[FAIL] ✗ 错误：非结束状态下复位清空数据 (问题2.6-001)")
    else
        print("[SKIP] 测试跳过：飞镖信息未成功输入")
    end
    
    print("--------------------------------------------------")
end

function test_normal_state_flow()
    print("测试用例 #5: 正常状态流转测试")
    print("  测试场景: 完整的正常使用流程")
    print("  预期行为: 状态按照 准备->投镖中->结束 正常流转")
    print("")
    print("操作步骤:")
    print("  1. 准备状态：输入飞镖信息")
    print("  2. 点击开始：进入投镖状态，启动倒计时")
    print("  3. 投镖中：发送靶区信息，记录得分")
    print("  4. 时间结束：显示成绩和评定")
    
    local res = ask("yesno", {
        title = '正常流程测试', 
        msg = '请执行完整的正常流程，流程是否都正常工作？', 
        default = true
    })
    
    check(res, "[PASS] ✓ 正常状态流转测试通过", "[FAIL] ✗ 正常状态流转存在问题")
    print("--------------------------------------------------")
end

function entry()
    clear(channels.COM_shoot)
    clear(channels.COM_target)
    
    print_test_header()
    
    -- 执行各项测试
    test_non_ready_state_input()
    etimer.delay(2000)
    
    test_countdown_start()
    etimer.delay(2000)
    
    test_timeout_reject_input()
    etimer.delay(2000)
    
    test_reset_in_non_end_state()
    etimer.delay(2000)
    
    test_normal_state_flow()
    
    print("==================================================")
    print("测试总结：状态机测试")
    print("  总用例数: 5")
    print("  关键问题验证:")
    print("    - 问题2.2-002: 非准备状态输入飞镖")
    print("    - 问题2.3-001: 倒计时启动")
    print("    - 问题2.5-001: 超时后拒绝输入")
    print("    - 问题2.6-001: 非结束状态复位")
    print("==================================================")
    print("")
    print("★ 状态机测试完成！")
    
    exit()
end
