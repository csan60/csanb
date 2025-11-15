-- ==============================================================================
-- 测试脚本：飞镖信息输入功能测试
-- 测试目标：验证数据传送机能够正确接收并处理飞镖信息输入
-- 需求编号：功能需求 - 飞镖信息输入
-- 测试类型：功能测试
-- ==============================================================================

-- 飞镖信息输入测试函数
function test_shoot_input()
    print("==================================================")
    print("开始测试：飞镖信息输入功能")
    print("==================================================")
    
    -- 清空通道缓冲区
    clear(channels.COM_shoot)
    etimer.delay(500)
    
    -- 测试用例计数
    local test_count = 0
    local pass_count = 0
    
    -- 遍历测试数据
    for index, value in ipairs(test_data.shoot) do
        test_count = test_count + 1
        
        local dart_type = value.type
        local dart_number = value.number
        
        print("--------------------------------------------------")
        print(string.format("测试用例 #%d:", index))
        print(string.format("  飞镖类型: %d", dart_type))
        print(string.format("  飞镖数量: %d", dart_number))
        
        -- 发送飞镖信息
        local result = write_msg(channels.COM_shoot, protocols.shoot, {
            type = dart_type,
            number = dart_number
        })
        
        if result then
            print("  ✓ 数据包发送成功")
        else
            print("  ✗ 数据包发送失败")
        end
        
        -- 等待系统处理
        etimer.delay(1000)
        
        -- 询问用户确认
        local user_confirm = ask("yesno", {
            title = "测试确认",
            msg = string.format("是否收到飞镖信息：类型=%d, 数量=%d?", dart_type, dart_number),
            default = true
        })
        
        -- 验证结果
        if check(user_confirm, 
                string.format("测试用例#%d 通过：系统正确接收飞镖信息", index),
                string.format("测试用例#%d 失败：系统未接收或处理错误", index)) then
            pass_count = pass_count + 1
        end
        
        print("--------------------------------------------------")
        etimer.delay(500)
    end
    
    -- 输出测试总结
    print("==================================================")
    print("测试总结：飞镖信息输入功能")
    print(string.format("  总用例数: %d", test_count))
    print(string.format("  通过用例: %d", pass_count))
    print(string.format("  失败用例: %d", test_count - pass_count))
    print(string.format("  通过率: %.1f%%", (pass_count / test_count) * 100))
    print("==================================================")
end

-- 主入口函数
function entry()
    print("智能飞镖感应设备 - 飞镖信息输入功能测试")
    print("测试时间: " .. os.date("%Y-%m-%d %H:%M:%S"))
    print("")
    
    -- 执行测试
    test_shoot_input()
    
    -- 退出测试
    exit()
end
