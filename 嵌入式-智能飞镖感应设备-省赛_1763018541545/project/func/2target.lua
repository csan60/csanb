
-- 靶区信息接收功能测试
-- 测试需求2.3: 靶区信息接收功能

function print_test_header()
    print("智能飞镖感应设备 - 靶区信息接收功能测试")
    print("测试时间: " .. os.date("%Y-%m-%d %H:%M:%S"))
    print("")
    print("==================================================")
    print("开始测试：靶区信息接收功能")
    print("==================================================")
end

function print_test_case(case_num, zone_num)
    print("--------------------------------------------------")
    print("测试用例 #" .. case_num .. ":")
    print("  投中区域: " .. zone_num)
end

function Target()
    local pass_count = 0
    local fail_count = 0
    local total_count = #test_data.target
    
    -- 测试基本靶区信息接收
    for _index, value in ipairs(test_data.target) do
        local zone = value.zone
        
        print_test_case(_index, zone)
        
        -- 发送靶区信息
        local result = write_msg(channels.COM_target, protocols.target, {zone = zone})
        
        if result then
            print("  ✓ 数据包发送成功")
        else
            print("  ✗ 数据包发送失败")
        end
        
        etimer.delay(1000)
        
        -- 检查系统是否正确接收
        local res = ask("yesno", {
            title = '靶区信息接收测试', 
            msg = '系统是否正确接收了区域' .. zone .. '的靶区信息？', 
            default = true
        })
        
        if res then
            print("[PASS] 测试用例#" .. _index .. " 通过：系统正确接收靶区信息")
            pass_count = pass_count + 1
        else
            print("[FAIL] 测试用例#" .. _index .. " 失败：系统未正确接收靶区信息")
            fail_count = fail_count + 1
        end
        
        print("--------------------------------------------------")
        etimer.delay(500)
    end
    
    -- 扩展测试：测试多个不同区域
    print("")
    print("扩展测试：测试多个不同区域")
    print("--------------------------------------------------")
    
    local extended_zones = {1, 5, 10, 15, 20}
    for _, zone in ipairs(extended_zones) do
        write_msg(channels.COM_target, protocols.target, {zone = zone})
        etimer.delay(500)
        print("测试区域: " .. zone)
        
        local res = ask("yesno", {
            title = '扩展测试', 
            msg = '区域' .. zone .. '的数据是否正确处理？', 
            default = true
        })
        
        if res then
            print("[PASS] 区域" .. zone .. " 测试通过")
            pass_count = pass_count + 1
            total_count = total_count + 1
        else
            print("[FAIL] 区域" .. zone .. " 测试失败")
            fail_count = fail_count + 1
            total_count = total_count + 1
        end
    end
    
    -- 打印测试总结
    print("==================================================")
    print("测试总结：靶区信息接收功能")
    print("  总用例数: " .. total_count)
    print("  通过用例: " .. pass_count)
    print("  失败用例: " .. fail_count)
    print("  通过率: " .. string.format("%.1f", (pass_count / total_count * 100)) .. "%")
    print("==================================================")
    
    -- 返回测试结果
    check(fail_count == 0, "所有靶区信息接收测试通过", "存在失败的测试用例")
end

function entry()
    -- 清空通道缓存
    clear(channels.COM_target)
    
    print_test_header()
    
    -- 执行测试
    Target()
    
    exit()
end
