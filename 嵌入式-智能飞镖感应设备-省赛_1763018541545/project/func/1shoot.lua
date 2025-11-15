
-- 飞镖信息输入功能测试
-- 测试需求2.2: 飞镖信息输入功能

function print_test_header()
    print("智能飞镖感应设备 - 飞镖信息输入功能测试")
    print("测试时间: " .. os.date("%Y-%m-%d %H:%M:%S"))
    print("")
    print("==================================================")
    print("开始测试：飞镖信息输入功能")
    print("==================================================")
end

function print_test_case(case_num, dart_type, dart_num)
    print("--------------------------------------------------")
    print("测试用例 #" .. case_num .. ":")
    print("  飞镖类型: " .. dart_type)
    print("  飞镖数量: " .. dart_num)
end

function Shoot()
    local pass_count = 0
    local fail_count = 0
    local total_count = #test_data.shoot
    
    for _index, value in ipairs(test_data.shoot) do
        local type = value.type
        local num = value.number
        
        print_test_case(_index, type, num)
        
        -- 发送飞镖信息
        local result = write_msg(channels.COM_shoot, protocols.shoot, {type = type, number = num})
        
        if result then
            print("  ✓ 数据包发送成功")
        else
            print("  ✗ 数据包发送失败")
        end
        
        etimer.delay(1000)
        
        -- 检查系统是否正确接收
        local res = ask("yesno", {
            title = '飞镖信息输入测试', 
            msg = '系统是否正确接收了飞镖类型' .. type .. '、数量' .. num .. '的信息？', 
            default = true
        })
        
        if res then
            print("[PASS] 测试用例#" .. _index .. " 通过：系统正确接收飞镖信息")
            pass_count = pass_count + 1
        else
            print("[FAIL] 测试用例#" .. _index .. " 失败：系统未正确接收飞镖信息")
            fail_count = fail_count + 1
        end
        
        print("--------------------------------------------------")
        etimer.delay(500)
    end
    
    -- 打印测试总结
    print("==================================================")
    print("测试总结：飞镖信息输入功能")
    print("  总用例数: " .. total_count)
    print("  通过用例: " .. pass_count)
    print("  失败用例: " .. fail_count)
    print("  通过率: " .. string.format("%.1f", (pass_count / total_count * 100)) .. "%")
    print("==================================================")
    
    -- 返回测试结果
    check(fail_count == 0, "所有飞镖信息输入测试通过", "存在失败的测试用例")
end

function entry()
    -- 清空通道缓存
    clear(channels.COM_shoot)
    
    print_test_header()
    
    -- 执行测试
    Shoot()
    
    exit()
end
