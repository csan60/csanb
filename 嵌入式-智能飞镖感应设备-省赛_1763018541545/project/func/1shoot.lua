
-- 飞镖信息输入
function Shoot()

  for _index, value in ipairs(test_data.shoot) do
    local type = value.type
    local num = value.number
    write_msg(channels.COM_shoot,protocols.shoot,{type = type,number = num})
    etimer.delay(1000)

  end

end


function entry()
    -- 此处输入程序代码
    Shoot()
    exit()
end