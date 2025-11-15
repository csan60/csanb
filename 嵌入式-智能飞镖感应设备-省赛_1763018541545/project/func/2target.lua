-- ==============================================================================
-- 测试脚本：靶区信息接收功能测试
-- 测试目标：验证靶体能够正确发送投中区域信息
-- 需求编号：功能需求 - 靶区信息接收
-- 测试类型：功能测试
-- ==============================================================================

-- 靶区信息接收测试函数
function test_target_reception()
    print("==================================================")
    print("开始测试：靶区信息接收功能")
    print("==================================================")
    
    -- 清空通道缓冲区
    clear(channels.COM_target)
    etimer.delay(500)
    
    -- 测试用例计数
    local test_count = 0
    local pass_count = 0
    
    -- 遍历测试数据
    for index, value in ipairs(test_data.target) do
        test_count = test_count + 1
        
        local target_zone = value.zone
        
        print("--------------------------------------------------")
        print(string.format("测试用例 #%d:", index))
        print(string.format("  投中区域: %d", target_zone))
        
        -- 模拟靶体发送投中区域信息
        local result = write_msg(channels.COM_target, protocols.target, {
            zone = target_zone
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
            msg = string.format("是否收到靶区信息：区域=%d?", target_zone),
            default = true
        })
        
        -- 验证结果
        if check(user_confirm,
                string.format("测试用例#%d 通过：系统正确接收靶区信息", index),
                string.format("测试用例#%d 失败：系统未接收或处理错误", index)) then
            pass_count = pass_count + 1
        end
        
        print("--------------------------------------------------")
        etimer.delay(500)
    end
    
    -- 测试不同区域
    print("")
    print("扩展测试：测试多个不同区域")
    print("--------------------------------------------------")
    
    local test_zones = {1, 5, 10, 15, 20}
    for _, zone in ipairs(test_zones) do
        test_count = test_count + 1
        
        print(string.format("测试区域: %d", zone))
        
        write_msg(channels.COM_target, protocols.target, {zone = zone})
        etimer.delay(1000)
        
        local confirm = ask("yesno", {
            title = "测试确认",
            msg = string.format("是否收到区域 %d 的信息?", zone),
            default = true
        })
        
        if check(confirm,
                string.format("区域%d 测试通过", zone),
                string.format("区域%d 测试失败", zone)) then
            pass_count = pass_count + 1
        end
        
        etimer.delay(500)
    end
    
    -- 输出测试总结
    print("==================================================")
    print("测试总结：靶区信息接收功能")
    print(string.format("  总用例数: %d", test_count))
    print(string.format("  通过用例: %d", pass_count))
    print(string.format("  失败用例: %d", test_count - pass_count))
    print(string.format("  通过率: %.1f%%", (pass_count / test_count) * 100))
    print("==================================================")
end

-- 主入口函数
function entry()
    print("智能飞镖感应设备 - 靶区信息接收功能测试")
    print("测试时间: " .. os.date("%Y-%m-%d %H:%M:%S"))
    print("")
    
    -- 执行测试
    test_target_reception()
    
    -- 退出测试
    exit()
end
